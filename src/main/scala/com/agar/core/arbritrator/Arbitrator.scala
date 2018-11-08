package com.agar.core.arbritrator

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, PoisonPill, Props}
import com.agar.core.context.AgarSystem
import com.agar.core.arbritrator.Player._
import com.agar.core.arbritrator.Region.{AreaOfInterest, Players}

import scala.language.postfixOps

// TEMPORARY DEFINITIONS -----------------------------------------------------------------------------------------------
object Region {
  type AreaOfInterest = Any
  type Players = Map[ActorRef, AreaOfInterest]
}

object Player {
  type Position = Any

  case class Tick(area: AreaOfInterest)

  case class MovePlayer(player: ActorRef, position: Position)

  case class DestroyPlayer(player: ActorRef)

  // -------------------------------------------------------------------------------------------------------------------

  sealed trait Status

  case object Ended extends Status

  case object Running extends Status


}

// ---------------------------------------------------------------------------------------------------------------------

object Arbitrator {

  def props(region: ActorRef)(implicit agarContext: AgarSystem): Props = Props(new Arbitrator(region)(agarContext))

  // Messages
  case class NewGameTurn(players: Players)

  case object TimeOutTurn

}

class Arbitrator(region: ActorRef)(implicit agarSystem: AgarSystem) extends Actor with ActorLogging {


  import Arbitrator._
  import context.dispatcher

  type Universe = Map[ActorRef, Status]

  def receive: Receive = {
    case NewGameTurn(players) =>
      val waitingPlayers = players.map { case (player, area) =>
        player ! Tick(area)
        player -> Running
      }

      val cancellable = context.system.scheduler.scheduleOnce(agarSystem.timeout(), self, TimeOutTurn)

      context.become(inProgressGameTurn(waitingPlayers, cancellable))
  }

  //
  // Ongoing turn behavior
  //

  def inProgressGameTurn(players: Universe, cancellable: Cancellable): Receive = {
    case event@MovePlayer(actorReference, _) =>
      val newPlayers = players.get(actorReference).map { _ =>
        region ! event
        actorReference -> Ended
      }.fold {
        players
      } {
        players + _
      }

      context.become(inProgressGameTurn(newPlayers, cancellable))

    case TimeOutTurn =>
      runningPlayers(players).foreach { case (p, _) =>
        region ! DestroyPlayer(p)
        p ! PoisonPill
      }

      context.become(receive)
  }

  //
  // Private behaviors
  //

  private def runningPlayers(players: Universe): Universe = players.filter {
    case (_, s) => s.equals(Running)
  }

}

//#arbitrator-actor
