package com.agar.core.region


import java.util.UUID

import akka.actor.{Actor, ActorRef, Props, Stash}
import com.agar.core.context.AgarContext
import com.agar.core.gameplay.energy.Energy
import com.agar.core.gameplay.player.{AreaOfInterest, Player}
import com.agar.core.gameplay.player.Player.Init
import com.agar.core.utils.{Point2d, Vector2d}


// TODO -- move these definitions

case class PlayerState(position: Vector2d, weight: Int, velocity: Vector2d, virtual: Option[ActorRef] = Option.empty)

case class EnergyState(position: Vector2d, value: Int, virtual: Option[ActorRef] = Option.empty)

// ------------------------------

object Region {

  case object GetEntitiesAOISet

  final case class InitRegion(nbOfPlayer: Int, nbOfStartingEnergy: Int)

  final case class Initialized(players: Map[ActorRef, PlayerState], energies: Map[ActorRef, EnergyState])

  def props(arbitrator: ActorRef, logger: ActorRef, width: Int, height: Int)(implicit agarContext: AgarContext): Props = Props(new Region(arbitrator, logger)(width, height)(agarContext))
}

class Region(arbitrator: ActorRef, logger: ActorRef)(width: Int, height: Int)(implicit agarContext: AgarContext) extends Actor with Stash {

  import Region._

  def MAX_ENERGY_VALUE = 10
  def DEFAULT_VELOCITY = Vector2d(2, 2)
  def WEIGHT_AT_START = 1


  def id = UUID.randomUUID()

  var players: Map[ActorRef, PlayerState] = Map()
  var energies: Map[ActorRef, EnergyState] = Map()

  def initialized: Receive = {
    case GetEntitiesAOISet => sender ! ()
  }

  def receive: Receive = {
    case InitRegion(nbOfPlayer, nbOfStartingEnergy) =>
      initializeEntities(nbOfPlayer, nbOfStartingEnergy)
      context become initialized
      logger ! Initialized(this.players, this.energies)

    case _ => stash()
  }

  def initializeEntities(nbOfPlayer: Int, nbOfStartingEnergy: Int) = {
    val r = new scala.util.Random

    this.players = (0 until nbOfPlayer)
      .map { n =>
        val player = freshPlayer(n)
        val (xStart, yStart) = (r.nextInt(width), r.nextInt(height))
        player ! Init(Point2d(xStart, yStart))
        player -> PlayerState(Vector2d(xStart, yStart), WEIGHT_AT_START, DEFAULT_VELOCITY)
      }.toMap

    this.energies = (0 until nbOfStartingEnergy)
      .map { _ =>
        val id = UUID.randomUUID()
        val position = Vector2d(r.nextInt(width), r.nextInt(height))
        val powerOfTheEnergy = 1 + r.nextInt(MAX_ENERGY_VALUE)
        val energy = freshEnergy(id, powerOfTheEnergy)
        energy -> EnergyState(position, powerOfTheEnergy)
      }.toMap
  }

  private def freshPlayer(n: Int): ActorRef = {
    context.actorOf(Player.props(n)(agarContext.algorithm), name = s"player-$n")
  }

  private def freshEnergy(id: UUID, valueOfEnergy: Int): ActorRef = {
    context.actorOf(Energy.props(valueOfEnergy), name = s"energy-$id")
  }
}
