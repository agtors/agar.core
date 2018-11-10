package com.agar.core.arbritrator

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, PoisonPill, Props}
import com.agar.core.arbritrator.Player._
import com.agar.core.arbritrator.Region.{AreaOfInterest, Players}
import com.agar.core.context.AgarSystem
import com.agar.core.gameplay.player.AOI
import com.agar.core.region.Region.GetEntitiesAOISet

import scala.language.postfixOps

// TEMPORARY DEFINITIONS -----------------------------------------------------------------------------------------------
object Region {
  type AreaOfInterest = Any
  type Players = Map[ActorRef, AreaOfInterest]
}

object Player {
  type Position = Any

  case object StartGameTurn

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

}

object ArbitratorProtocol {

  case class AOISet(players: Players)

  case object TimeOutTurn

}

class Arbitrator(region: ActorRef)(implicit agarSystem: AgarSystem) extends Actor with ActorLogging {

  import com.agar.core.arbritrator.ArbitratorProtocol._
  import context.dispatcher

  type PlayersAOI = Map[ActorRef, AOI]
  type PlayersStatus = Map[ActorRef, Status]

  override def receive: Receive = waitingForNewGameTurn

  //
  // Waiting for start turn behavior
  //

  def waitingForNewGameTurn: Receive = {
    case StartGameTurn =>

      region ! GetEntitiesAOISet

      context become waitingForAOISet
  }

  //
  // Waiting for new turn behavior
  //

  def waitingForAOISet: Receive = {
    case AOISet(players) =>
      val waitingPlayers = players.map { case (player, area) =>
        player ! Tick(area)
        player -> Running
      }

      context become inProgressGameTurn(waitingPlayers, scheduleTurnTimeOut)
  }

  //
  // Ongoing turn behavior
  //

  def inProgressGameTurn(players: PlayersStatus, cancellable: Cancellable): Receive = {
    case event@MovePlayer(player, _) =>
      val newPlayers = players.get(player).fold {
        players
      } { _ =>
        region ! event
        players + (player -> Running)
      }

      context become inProgressGameTurn(newPlayers, cancellable)

    case TimeOutTurn =>
      runningPlayers(players).foreach { case (player, _) =>
        player ! PoisonPill
        region ! DestroyPlayer(player)
      }

      self ! StartGameTurn

      context become waitingForNewGameTurn
  }

  //
  // Private behaviors
  //

  private def scheduleTurnTimeOut: Cancellable = {
    context.system.scheduler.scheduleOnce(agarSystem.timeout(), self, TimeOutTurn)
  }

  private def runningPlayers(players: PlayersStatus): PlayersStatus = players.filter {
    case (_, status) => status.equals(Running)
  }

}

//#arbitrator-actor
