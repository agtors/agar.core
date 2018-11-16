package com.agar.core.region


import akka.actor.{Actor, ActorLogging, ActorRef, Props, Stash}
import com.agar.core.arbritrator.Protocol.AOISet
import com.agar.core.gameplay.energy.Energy
import com.agar.core.gameplay.player.{AreaOfInterest, Player, VirtualPlayer}
import com.agar.core.logger.Journal.WorldState
import com.agar.core.region.Protocol._
import com.agar.core.region.State.{EnergyState, PlayerState}
import com.agar.core.utils.{RegionBoundaries, Vector2d}

import scala.util.Random

object State {

  case class PlayerState(position: Vector2d, weight: Int, velocity: Vector2d, virtual: Boolean = false) {
    def virtual(b: Boolean): PlayerState = PlayerState(position, weight, velocity, b)
  }

  case class EnergyState(position: Vector2d, value: Int, virtual: Boolean = false) {
    def virtual(b: Boolean): EnergyState = EnergyState(position, value, b)
  }

}

object Region {
  def props(worldSquare: RegionBoundaries, regionSquare: RegionBoundaries, frontierSquare: RegionBoundaries)
           (journal: ActorRef, bridge: ActorRef): Props =
    Props(new Region(worldSquare, regionSquare, frontierSquare)(journal, bridge))
}

object Protocol {

  case object GetEntitiesAOISet

  case class InitRegion(nbOfPlayer: Int, nbOfStartingEnergy: Int)

  case class Initialized(players: Map[ActorRef, PlayerState], energies: Map[ActorRef, EnergyState])

  case class CreatePlayer(state: PlayerState)

  case class CreateEnergy(state: EnergyState)

  case class RegisterPlayer(player: ActorRef, state: PlayerState)

  case class RegisterEnergy(player: ActorRef, state: EnergyState)

  case class Move(player: ActorRef, position: Vector2d, weight: Int)

  case class Killed(player: ActorRef)

  case class Destroy(player: ActorRef)

  case class Virtual(event: Any) // ^^ TODO(didier) Define a upper bound type

}

trait Constants {
  def MAX_ENERGY_VALUE = 50

  def DEFAULT_VELOCITY = Vector2d(2, 2)

  def WEIGHT_AT_START = 10
}

class Region(worldSquare: RegionBoundaries, regionSquare: RegionBoundaries, frontierSquare: RegionBoundaries)(journal: ActorRef, bridge: ActorRef) extends Actor with Stash with ActorLogging with Constants {

  var players: Map[ActorRef, PlayerState] = Map()
  var energies: Map[ActorRef, EnergyState] = Map()

  var virtualPlayers: Map[ActorRef, PlayerState] = Map()
  var virtualEnergies: Map[ActorRef, EnergyState] = Map()

  def receive: Receive = {
    case InitRegion(nbOfPlayer, nbOfStartingEnergy) =>
      initializeEntities(nbOfPlayer, nbOfStartingEnergy)
      journal ! Initialized(players, energies)
      context become initialized

    case _ => stash()
  }

  def initialized: Receive = {
    case GetEntitiesAOISet =>
      journal ! WorldState(players, energies)
      sender ! AOISet(AreaOfInterest.getPlayersAOISet(players, virtualPlayers, energies, virtualEnergies))

    case CreatePlayer(state) =>
      val player = createNewPlayer(state.position, state.weight)
      val virtual = manageVirtualPlayer(player, state)

      players = players + (player -> state.virtual(virtual))

    case CreateEnergy(state) =>
      val energy = createNewEnergy(state.value)
      val virtual = manageVirtualEnergy(energy, state)

      energies = energies + (energy -> state.virtual(virtual))

    case Move(player, position, weight) =>
      players = players.get(player).fold {
        players
      } { s =>
        val state = PlayerState(position, weight, s.velocity, s.virtual)
        val virtual = manageVirtualPlayer(player, state)

        players + (player -> state.virtual(virtual))
      }

    case e@Killed(player) =>
      players.get(player).foreach { s =>
        if (s.virtual) bridge ! Virtual(e)

        val energy = createNewEnergy(s.weight)
        val state = EnergyState(s.position, s.weight)

        manageVirtualEnergy(energy, state)

        energies = energies + (energy -> state)
        context.stop(player)
      }

      players = players.filterKeys {
        player != _
      }

    case e@Destroy(energy) =>
      println(e)
      energies.get(energy).foreach { s =>
        if (s.virtual) bridge ! Virtual(e)

        context.stop(energy)
      }

      energies = energies.filterKeys {
        energy != _
      }

    // Virtual messages reification

    case e@Virtual(CreatePlayer(state)) =>
      println(e)
      val player = createNewPlayer(state.position, state.weight)
      players = players + (player -> state.virtual(false))

    case e@Virtual(RegisterPlayer(p, s)) =>
      println(e)
      virtualPlayers = virtualPlayers + (p -> s)

    case e@Virtual(RegisterEnergy(p, s)) =>
      println(e)
      virtualEnergies = virtualEnergies + (p -> s)

    case e@Virtual(Move(player, position, weight)) =>
      println(e)
      virtualPlayers = virtualPlayers.get(player).fold {
        virtualPlayers
      } { s =>
        virtualPlayers + (player -> PlayerState(position, weight, s.velocity))
      }

    case e@Virtual(Killed(player)) =>
      println(e)
      virtualPlayers = virtualPlayers.filterKeys {
        player != _
      }

    case e@Virtual(Destroy(entity)) =>
      println(e)
      virtualPlayers = virtualPlayers.filterKeys {
        entity != _
      }
      virtualEnergies = virtualEnergies.filterKeys {
        entity != _
      }
  }

  private def manageVirtualPlayer(player: ActorRef, state: PlayerState): Boolean = {
    val inFrontier = isInFrontier(state.position, state.weight)

    (state.virtual, inFrontier) match {
      case (true, true) =>
        bridge ! Virtual(Move(player, state.position, state.weight))
      case (false, true) =>
        bridge ! Virtual(RegisterPlayer(player, state))
      case (true, false) =>
        if (isInRegion(state.position, state.weight)) {
          bridge ! Virtual(Destroy(player))
        } else {
          context.stop(player)
          bridge ! Virtual(CreatePlayer(state))
        }
      case (false, false) =>
        // Small frontier isn't it?
        if (!isInRegion(state.position, state.weight)) {
          context.stop(player)
          bridge ! Virtual(CreatePlayer(state))
        }
    }

    inFrontier
  }

  private def manageVirtualEnergy(energy: ActorRef, state: EnergyState): Boolean = {
    val inFrontier = isInFrontier(state.position, state.value)

    if (inFrontier) {
      bridge ! Virtual(RegisterEnergy(energy, state))
    }

    inFrontier
  }

  def initializeEntities(nbOfPlayer: Int, nbOfStartingEnergy: Int): Unit = {
    val r = new scala.util.Random

    this.players = players ++ (0 until nbOfPlayer)
      .map { _ =>
        val position: Vector2d = generatePosition(r)
        val player = createNewPlayer(position, WEIGHT_AT_START)
        val state = PlayerState(position, WEIGHT_AT_START, DEFAULT_VELOCITY)
        val virtual = manageVirtualPlayer(player, state)

        player -> state.virtual(virtual)
      }.toMap

    this.energies = energies ++ (0 until nbOfStartingEnergy)
      .map { _ =>
        val position: Vector2d = generatePosition(r)
        val powerOfTheEnergy = 1 + r.nextInt(MAX_ENERGY_VALUE)
        val energy = createNewEnergy(powerOfTheEnergy)
        val state = EnergyState(position, powerOfTheEnergy)
        val virtual = manageVirtualEnergy(energy, state)

        energy -> state.virtual(virtual)
      }.toMap
  }

  private def createNewPlayer(position: Vector2d, weight: Int): ActorRef = {
    context.actorOf(Player.props(worldSquare, position, weight)(self))
  }

  private def createNewVirtualPlayer(position: Vector2d, weight: Int): ActorRef = {
    context.actorOf(VirtualPlayer.props(bridge))
  }

  private def createNewEnergy(valueOfEnergy: Int): ActorRef = {
    context.actorOf(Energy.props(valueOfEnergy)(self))
  }

  //
  // Position management
  //

  private def generatePosition(r: Random) = {
    Vector2d(
      r.nextDouble() * regionSquare.height,
      r.nextDouble() * regionSquare.width
    )
  }

  private def isInRegion(position: Vector2d, weight: Int): Boolean = {
    regionSquare.intersect(position, weight)
  }

  private def isInFrontier(position: Vector2d, weight: Int): Boolean = {
    frontierSquare.intersect(position, weight)
  }

}
