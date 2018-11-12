package com.agar.core.arbritrator

<<<<<<< HEAD
<<<<<<< HEAD
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
=======
import java.util.logging

=======
>>>>>>> Review test and change arguments according to the new protocol
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, PoisonPill, Props}
<<<<<<< HEAD
>>>>>>> Add first definitions and bridge actor for clustering
import com.agar.core.arbritrator.Player._
import com.agar.core.context.AgarSystem
import com.agar.core.gameplay.player.AOI
import com.agar.core.gameplay.player.Player.{KilledPlayer, Tick}
import com.agar.core.region.Protocol.{GetEntitiesAOISet, Killed, Move}
=======
import com.agar.core.arbritrator.Arbitrator.{MovePlayer, StartGameTurn}
import com.agar.core.arbritrator.Player._
import com.agar.core.context.AgarSystem
import com.agar.core.gameplay.player.AOI
import com.agar.core.gameplay.player.Player.Tick
<<<<<<< HEAD
import com.agar.core.region.Region.{Destroy, GetEntitiesAOISet}
>>>>>>> Review and simplify protocol Region <-> Arbitrator <-> Player
=======
import com.agar.core.region.Region.{Destroy, GetEntitiesAOISet, Move}
>>>>>>> Fix message sent to the region when a plauyer has moved + Type in the playerspec
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

<<<<<<< HEAD
<<<<<<< HEAD
  def props(region: ActorRef)(implicit agarContext: AgarSystem): Props =
    Props(new Arbitrator(region)(agarContext))
=======
  def props(bridge: ActorRef, region: ActorRef)(implicit agarContext: AgarSystem): Props = Props(new Arbitrator(bridge, region)(agarContext))
>>>>>>> Add first definitions and bridge actor for clustering
=======
  def props(bridge: ActorRef, region: ActorRef)(implicit agarContext: AgarSystem): Props =
    Props(new Arbitrator(bridge, region)(agarContext))
>>>>>>> Review test and change arguments according to the new protocol

  case object StartGameTurn

  case class MovePlayer(position: Vector2d)

}

object Protocol {

  case class StartNewGame(players: Int, energies: Int)

  case object StartGameTurn

  case class MovePlayer(position: Vector2d, weight: Int)

  case class AOISet(players: Map[ActorRef, AOI])

  case object TimeOutTurn

}

class Arbitrator(bridge: ActorRef, region: ActorRef)(implicit agarSystem: AgarSystem)
  extends Actor with ActorLogging {

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
<<<<<<< HEAD
<<<<<<< HEAD
    case MovePlayer(position, weight) =>
=======
    case event@MovePlayer(_) =>
>>>>>>> Review and simplify protocol Region <-> Arbitrator <-> Player
=======
    case MovePlayer(position) =>
>>>>>>> Fix message sent to the region when a plauyer has moved + Type in the playerspec

      val newPlayers = players.get(sender).fold {
        players
      } { _ =>
<<<<<<< HEAD
<<<<<<< HEAD
        region ! Move(sender, position, weight)
        players + (sender -> Ended)
=======
        region ! event
<<<<<<< HEAD
        bridge ! event
        players + (player -> Ended)
>>>>>>> Add first definitions and bridge actor for clustering
=======
=======
        region ! Move(sender, position)
>>>>>>> Fix message sent to the region when a plauyer has moved + Type in the playerspec
        players + (sender -> Ended)
>>>>>>> Review and simplify protocol Region <-> Arbitrator <-> Player
      }

      context become inProgressGameTurn(newPlayers)

    case KilledPlayer =>
      region ! Killed(sender)

    case TimeOutTurn =>
      runningPlayers(players).foreach { case (player, _) =>
<<<<<<< HEAD
        region ! Killed(player)
=======
        player ! PoisonPill
        region ! Destroy(player)
>>>>>>> Review and simplify protocol Region <-> Arbitrator <-> Player
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
