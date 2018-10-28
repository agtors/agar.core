package com.agar.core

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, PoisonPill, Props}
import com.agar.core.Player.Move

import scala.concurrent.duration
import scala.language.postfixOps


//#logger-companion
object Arbitrator {
  //#printer-messages
  def props(loggerActor: ActorRef): Props = Props(new Arbitrator(loggerActor))


  //#printer-messages
  final case class Start(numbers: Int)

  final case class Played(number: Int, position: (Int, Int))

  case object NewTurn

  case object SolveTurn

  case object TimeoutTurn

}

//#logger-companion

sealed trait PlayerStatus

case class WaitingPlayer(position: (Int, Int)) extends PlayerStatus

case object RunningPlayer extends PlayerStatus


//#logger-actor
class Arbitrator(loggerActor: ActorRef) extends Actor with ActorLogging {

  import Arbitrator._
  import context.dispatcher

  var players: Map[Int, (ActorRef, PlayerStatus)] = Map()

  def receive: PartialFunction[Any, Unit] = {
    case Start(numbers) =>
      players = (0 to numbers).map { n => (n, freshPlayer(n)) }.toMap
      context.become(startGameTurn)
      self ! NewTurn
  }

  def startGameTurn: PartialFunction[Any, Unit] = {

    case NewTurn => {
      players = players.map { case (n, (p, _)) =>
        p ! Move
        n -> (p, RunningPlayer)
      }

      val d = duration.FiniteDuration(1000, TimeUnit.SECONDS)
      val cancellable = context.system.scheduler.scheduleOnce(d, self, TimeoutTurn)

      context.become(runGameTurn(cancellable))
    }

    case SolveTurn => {
      // Should detect collision and kill weakest actor
      self ! NewTurn
    }
  }

  def runGameTurn(cancellable: Cancellable): PartialFunction[Any, Unit] = {
    case Played(number, position) => {
      players.get(number).foreach { case (p, _) =>
        players = players + (number -> (p, WaitingPlayer(position)))
      }

      if (turnIfFinished()) {
        cancellable.cancel()
        context.become(startGameTurn)
        self ! SolveTurn
      }
    }

    case TimeoutTurn => {
      players = players.map {
        case (n, (p, RunningPlayer)) =>
          // kill this actor
          p ! PoisonPill
          n -> (p, RunningPlayer)
        case (n, (p, s)) =>
          n -> (p, s)
      }.filter(_._2._2 != RunningPlayer)
    }
  }

  private def turnIfFinished(): Boolean = !players.exists {
    _._2._2 == RunningPlayer
  }

  private def freshPlayer(n: Int): (ActorRef, WaitingPlayer) = {
    Pair(context.actorOf(Player.props(n, (0, 0), loggerActor)), WaitingPlayer(0, 0))
  }
}

//#logger-actor
