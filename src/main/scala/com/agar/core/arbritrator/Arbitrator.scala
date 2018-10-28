package com.agar.core.arbritrator

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, PoisonPill, Props}
import com.agar.core.player.Player
import com.agar.core.player.Player.{Init, Move}

import scala.concurrent.duration
import scala.language.postfixOps


//#logger-companion
object Arbitrator {
  //#printer-messages
  def props(loggerActor: ActorRef): Props = Props(new Arbitrator(loggerActor))

  final case class Start(numbers: Int)

  final case class Played(number: Int, position: (Int, Int))

  case object NewTurn

  case object SolveTurn

  case object TimeoutTurn

}

//#logger-companion

//#arbitrator-actor

class Arbitrator(loggerActor: ActorRef) extends Actor with ActorLogging {

  import Arbitrator._
  import context.dispatcher

  type Universe = Map[Int, (ActorRef, PlayerStatus)]

  // Initial behavior

  def receive: PartialFunction[Any, Unit] = {
    case Start(numbers) =>
      val players = (0 to numbers)
        .map { n =>
          val player = freshPlayer(n)
          val position = (0, 0)
          player ! Init(position)
          n -> (player, WaitingPlayer(position))
        }.toMap

      context.become(startGameTurn(players))
      self ! NewTurn
  }

  //
  // Start turn behavior
  //

  def startGameTurn(players: Universe): PartialFunction[Any, Unit] = {
    case NewTurn =>
      val newPlayers = players.map {
        case (n, (p, _)) =>
          p ! Move
          n -> (p, RunningPlayer)
      }

      val d = duration.FiniteDuration(5, TimeUnit.SECONDS)
      val cancellable = context.system.scheduler.scheduleOnce(d, self, TimeoutTurn)

      context.become(runGameTurn(newPlayers, cancellable))

    case SolveTurn =>
      // Should detect collision and kill weakest actor
      self ! NewTurn
  }

  //
  // Ongoing turn behavior
  //

  def runGameTurn(players: Universe, cancellable: Cancellable): PartialFunction[Any, Unit] = {
    case Played(number, position) =>
      val newPlayers = players.get(number).map {
        case (p, _) =>
          number -> (p, WaitingPlayer(position))
      }.fold {
        players
      } {
        players + _
      }

      if (runningPlayers(newPlayers).isEmpty) {
        cancellable.cancel()
        context.become(startGameTurn(newPlayers))
        self ! SolveTurn
      }

    case TimeoutTurn =>
      runningPlayers(players).foreach { case (n, (p, _)) =>
        // kill this actor
        p ! PoisonPill
      }

      context.become(startGameTurn(waitingPlayers(players)))
      self ! SolveTurn
  }

  //
  // Private behaviors
  //

  private def runningPlayers(players: Universe): Map[Int, (ActorRef, PlayerStatus)] = players.filter {
    case (_, (_,s)) => s == RunningPlayer
  }

  private def waitingPlayers(players: Universe): Map[Int, (ActorRef, PlayerStatus)] = players.filter {
    case (_, (_,s)) => s != RunningPlayer
  }

  private def freshPlayer(n: Int): ActorRef = {
    context.actorOf(Player.props(n, loggerActor))
  }
}

//#arbitrator-actor
