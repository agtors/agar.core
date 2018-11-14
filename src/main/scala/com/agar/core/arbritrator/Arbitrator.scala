package com.agar.core.arbritrator

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, PoisonPill, Props}
import com.agar.core.arbritrator.Player._
import com.agar.core.context.AgarSystem
import com.agar.core.gameplay.player.AOI
import com.agar.core.gameplay.player.Player.{KilledPlayer, Tick}
import com.agar.core.region.Protocol.{Destroy, GetEntitiesAOISet, Kill, Move}
import com.agar.core.utils.Vector2d

import scala.language.postfixOps

// TEMPORARY DEFINITIONS -----------------------------------------------------------------------------------------------

object Player {

  sealed trait Status

  case object Ended extends Status

  case object Running extends Status

}

// ---------------------------------------------------------------------------------------------------------------------

object Arbitrator {

  def props(region: ActorRef)(implicit agarContext: AgarSystem): Props =
    Props(new Arbitrator(region)(agarContext))

}

object Protocol {

  case class StartNewGame(players: Int, energies: Int)

  case object StartGameTurn

  case class MovePlayer(position: Vector2d, weight: Int)

  case class AOISet(players: Map[ActorRef, AOI])

  case object TimeOutTurn

}

class Arbitrator(region: ActorRef)(implicit agarSystem: AgarSystem) extends Actor with ActorLogging {

  import com.agar.core.arbritrator.Protocol._
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

      // logging.Logger.getAnonymousLogger.info(s"Starting a new game turn with ${players.size} players")

      val waitingPlayers = players.map { case (player, area) =>
        player ! Tick(area)
        player -> Running
      }

      scheduleTurnTimeOut()

      context become inProgressGameTurn(waitingPlayers)
  }

  //
  // Ongoing turn behavior
  //

  def inProgressGameTurn(players: PlayersStatus): Receive = {
    case MovePlayer(position, weight) =>

      val newPlayers = players.get(sender).fold {
        players
      } { _ =>
        region ! Move(sender, position, weight)
        players + (sender -> Ended)
      }

      context become inProgressGameTurn(newPlayers)

    case KilledPlayer =>
      region ! Kill(sender)

    case TimeOutTurn =>
      runningPlayers(players).foreach { case (player, _) =>
        player ! PoisonPill
        region ! Destroy(player)
      }

      context become waitingForNewGameTurn

      self ! StartGameTurn
  }

  //
  // Private behaviors
  //

  private def scheduleTurnTimeOut(): Cancellable = {
    context.system.scheduler.scheduleOnce(agarSystem.timeout(), self, TimeOutTurn)
  }

  private def runningPlayers(players: PlayersStatus): PlayersStatus = players.filter {
    case (_, status) => status.equals(Running)
  }

}

//#arbitrator-actor
