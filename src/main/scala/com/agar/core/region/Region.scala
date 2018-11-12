package com.agar.core.region


import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import com.agar.core.arbritrator.ArbitratorProtocol.AOISet
import com.agar.core.bridge.Bridge.FromBridge
import com.agar.core.context.AgarSystem
import com.agar.core.gameplay.energy.Energy
import com.agar.core.gameplay.player.{AreaOfInterest, Player}
import com.agar.core.utils.Vector2d


// TODO -- move these definitions

case class PlayerState(position: Vector2d, weight: Int, velocity: Vector2d, virtual: Option[ActorRef] = Option.empty)

case class EnergyState(position: Vector2d, value: Int, virtual: Option[ActorRef] = Option.empty)

// ------------------------------

object Region {

  case object GetEntitiesAOISet

  final case class InitRegion(nbOfPlayer: Int, nbOfStartingEnergy: Int)

  final case class Initialized(players: Map[ActorRef, PlayerState], energies: Map[ActorRef, EnergyState])

  case class Move(player: ActorRef, position: Vector2d)

  case class Destroy(player: ActorRef)

  def props(bridge: ActorRef, logger: ActorRef, width: Int, height: Int)(implicit agarSystem: AgarSystem): Props = Props(new Region(bridge, logger)(width, height)(agarSystem))
}

class Region(bridge: ActorRef, logger: ActorRef)(width: Int, height: Int)(implicit agarSystem: AgarSystem) extends Actor with Stash with ActorLogging {

  import Region._

  def MAX_ENERGY_VALUE = 10

  def DEFAULT_VELOCITY = Vector2d(2, 2)

  def WEIGHT_AT_START = 1


  def id: UUID = UUID.randomUUID()

  var players: Map[ActorRef, PlayerState] = Map()
  var energies: Map[ActorRef, EnergyState] = Map()

  def initialized: Receive = {
    case GetEntitiesAOISet =>
      sender ! AOISet(AreaOfInterest.getPlayersAOISet(this.players, this.energies))

    case e: FromBridge =>
      log.info(s"RECV $e")

  }

  def receive: Receive = {
    case InitRegion(nbOfPlayer, nbOfStartingEnergy) =>
      initializeEntities(nbOfPlayer, nbOfStartingEnergy)
      context become initialized
      logger ! Initialized(this.players, this.energies)

    case _ => stash()
  }

  def initializeEntities(nbOfPlayer: Int, nbOfStartingEnergy: Int): Unit = {
    val r = new scala.util.Random

    this.players = (0 until nbOfPlayer)
      .map { n =>
        val position = Vector2d(r.nextInt(width), r.nextInt(height))
        val player = createNewPlayer(position)
        player -> PlayerState(position, WEIGHT_AT_START, DEFAULT_VELOCITY)
      }.toMap

    this.energies = (0 until nbOfStartingEnergy)
      .map { _ =>
        val position = Vector2d(r.nextInt(width), r.nextInt(height))
        val powerOfTheEnergy = 1 + r.nextInt(MAX_ENERGY_VALUE)
        val energy = createNewEnergy(powerOfTheEnergy)
        energy -> EnergyState(position, powerOfTheEnergy)
      }.toMap
  }

  private def createNewPlayer(position: Vector2d): ActorRef = {
    context.actorOf(Player.props(position, WEIGHT_AT_START)(self))
  }

  private def createNewEnergy(valueOfEnergy: Int): ActorRef = {
    context.actorOf(Energy.props(valueOfEnergy))
  }
}
