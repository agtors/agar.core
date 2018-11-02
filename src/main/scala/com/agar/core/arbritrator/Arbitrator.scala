package com.agar.core.arbritrator

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import com.agar.core.context.AgarContext
import com.agar.core.logger.Logger
import com.agar.core.player.Player
import com.agar.core.player.Player.{Init, Move}

import scala.language.postfixOps


//#logger-companion

object Arbitrator {
  //#printer-messages
  def props(loggerActor: ActorRef)(implicit agarContext: AgarContext): Props = Props(new Arbitrator(loggerActor)(agarContext))

  final case class Start(numbers: Int)

  final case class Played(number: Int, position: (Int, Int))

  case object NewTurn

  case object SolveTurn

  case object TimeoutTurn

}

//#logger-companion

//#arbitrator-actor

class Arbitrator(logger: ActorRef)(implicit agarContext: AgarContext) extends Actor with ActorLogging {

  import Arbitrator._
  import context.dispatcher

  type Universe = Map[Int, (ActorRef, PlayerStatus)]

  // Initial behavior

  def receive: PartialFunction[Any, Unit] = {
    case Start(numbers) =>
      val players = (0 until numbers)
        .map { n =>
          val player = freshPlayer(n)
          val position = agarContext.position.fresh()
          player ! Init(position)
          logger ! Logger.PlayerCreated(n, position)
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
      log.info(s"Start new turn with ${players.size} players")

      val newPlayers = players.map {
        case (n, (p, _)) =>
          p ! Move
          n -> (p, RunningPlayer)
      }

      val cancellable = context.system.scheduler.scheduleOnce(agarContext.system.timeout(), self, TimeoutTurn)

      context.become(runGameTurn(newPlayers, cancellable))

    case SolveTurn =>
      // Should detect collision and kill weakest actor
      self ! NewTurn

    case e =>
      println(s"Receive unexpected event $e")
  }

  //
  // Ongoing turn behavior
  //

  def runGameTurn(players: Universe, cancellable: Cancellable): PartialFunction[Any, Unit] = {
    case Played(number, position) =>
      val newPlayers = players.get(number).map {
        case (p, _) =>
          logger ! Logger.PlayerMoved(number, position)
          number -> (p, WaitingPlayer(position))
      }.fold {
        players
      } {
        players + _
      }

      context.become(runGameTurn(newPlayers, cancellable))

    case TimeoutTurn =>
      runningPlayers(players).foreach { case (n, (p, _)) =>
        // kill this actor
        logger ! Logger.PlayerDestroyed(n)
        context.stop(p)
      }

      context.become(startGameTurn(waitingPlayers(players)))
      self ! SolveTurn

    case e =>
      println(s"Receive unexpected event $e")
  }

  //
  // Private behaviors
  //

  private def runningPlayers(players: Universe): Map[Int, (ActorRef, PlayerStatus)] = players.filter {
    case (_, (_, s)) => s.equals(RunningPlayer)
  }

  private def waitingPlayers(players: Universe): Map[Int, (ActorRef, PlayerStatus)] = players.filter {
    case (_, (_, s)) => !s.equals(RunningPlayer)
  }

  private def freshPlayer(n: Int): ActorRef = {
    context.actorOf(Player.props(n)(agarContext.algorithm), name = s"player-$n")
  }
}

//#arbitrator-actor
