package com.agar.core.region

import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import com.agar.core.arbritrator.Protocol.AOISet
import com.agar.core.gameplay.energy.Energy
import com.agar.core.gameplay.player.{AreaOfInterest, Player}
import com.agar.core.logger.Journal.WorldState
import com.agar.core.region.Protocol._
import com.agar.core.region.State.{EnergyState, PlayerState, VirtualPlayerState}
import com.agar.core.utils.Vector2d

object State {

  case class VirtualPlayerState(position: Vector2d, weight: Int, velocity: Vector2d)

  case class PlayerState(position: Vector2d, weight: Int, velocity: Vector2d, virtual: Boolean = false) {
    def increaseWeight(value: Int): PlayerState = PlayerState(position, weight + value, velocity, virtual)
  }

  case class EnergyState(position: Vector2d, value: Int, virtual: Boolean = false)

}

object Region {

  def props(journal: ActorRef, width: Int, height: Int): Props = Props(new Region(journal)(width, height))

}

object Protocol {

  case object GetEntitiesAOISet

  final case class InitRegion(nbOfPlayer: Int, nbOfStartingEnergy: Int)

  final case class Initialized(players: Map[ActorRef, PlayerState], energies: Map[ActorRef, EnergyState])

  case class Move(player: ActorRef, position: Vector2d)

  case class Destroy(player: ActorRef)

}

trait Constants {
  def MAX_ENERGY_VALUE = 10

  def DEFAULT_VELOCITY = Vector2d(2, 2)

  def WEIGHT_AT_START = 1
}

class Region(journal: ActorRef)(width: Int, height: Int) extends Actor with Stash with ActorLogging with Constants {

  var virtualPlayers: Map[ActorRef, VirtualPlayerState] = Map()
  var players: Map[ActorRef, PlayerState] = Map()
  var energies: Map[ActorRef, EnergyState] = Map()

  def initialized: Receive = {
    case GetEntitiesAOISet =>
      this.solveConflicts()

      journal ! WorldState(players, energies)
      sender ! AOISet(AreaOfInterest.getPlayersAOISet(this.players, this.energies))

    case Move(player, position) =>
      players = players.get(player).fold {
        players
      } { state =>
        players + (player -> PlayerState(position, state.weight, state.velocity, state.virtual))
      }

      virtualPlayers = virtualPlayers.get(player).fold {
        virtualPlayers
      } { state =>
        virtualPlayers + (player -> VirtualPlayerState(position, state.weight, state.velocity))
      }

    case Destroy(player) =>
      destroyPlayer(player)
  }

  def receive: Receive = {
    case InitRegion(nbOfPlayer, nbOfStartingEnergy) =>
      initializeEntities(nbOfPlayer, nbOfStartingEnergy)
      journal ! Initialized(this.players, this.energies)
      context become initialized

    case _ => stash()
  }

  def initializeEntities(nbOfPlayer: Int, nbOfStartingEnergy: Int): Unit = {
    val r = new scala.util.Random

    this.players = (0 until nbOfPlayer)
      .map { _ =>
        val position = Vector2d(r.nextInt(width), r.nextInt(height))
        val player = createNewPlayer(position, WEIGHT_AT_START)
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

  private def solveConflicts(): Unit = {
    val (newPlayers, newEnergies, eaten) = Conflicts.solve(players, energies)

    this.players = newPlayers
    this.energies = newEnergies

    eaten.foreach {
      context.stop
    }
  }

  private def destroyPlayer(player: ActorRef): Unit = {
    players.get(player).foreach { state =>
      context.stop(player)
    }

    // Perform destruction
    players = players.filterKeys {
      player != _
    }

  }

  private def createNewPlayer(position: Vector2d, weight: Int): ActorRef = {
    context.actorOf(Player.props(position, weight), s"player-${UUID.randomUUID().toString}")
  }

  private def createNewEnergy(valueOfEnergy: Int): ActorRef = {
    context.actorOf(Energy.props(valueOfEnergy))
  }

}
