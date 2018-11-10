package com.agar.core.gameplay.player

import akka.actor.ActorRef
import com.agar.core.region.{EnergyState, PlayerState}
import com.agar.core.utils.Vector2d

// ref can be the ActorRef of the virtual or the real actor
case class PlayerInfos(position: Vector2d, velocity: Vector2d, weight: Int, ref: ActorRef)

// ref can be the ActorRef of the virtual or the real actor
case class EnergyInfos(position: Vector2d, value: Int, ref: ActorRef)

case class AOI(players: List[PlayerInfos], energies: List[EnergyInfos])


object AreaOfInterest {

  // There is overwhelming scientific evidence that the correct number is this one
  val RADIUS_AREA_OF_INTEREST = 400

  def getPlayersAOISet(players: Map[ActorRef, PlayerState], energies: Map[ActorRef, EnergyState]):  Map[ActorRef, AOI] = {
    players.map{ case (ref, playerState) =>
      val otherPlayers = players.filter({case (actorRef, _) => isNotTheSamePlayer(actorRef, ref) })
      ref -> getPlayerAOI(playerState, otherPlayers, energies)
    }
  }

  private def getPlayerAOI(playerState: PlayerState, players: Map[ActorRef, PlayerState], energies: Map[ActorRef, EnergyState]):  AOI = {
    AOI(
      getPlayersInAOIOfPlayer(playerState.position, players),
      getEnergiesInAOIOfPlayer(playerState.position, energies)
    )
  }

  private def getPlayersInAOIOfPlayer(playerPosition: Vector2d, players: Map[ActorRef, PlayerState]): List[PlayerInfos] =
    players
      .filter{case (_, p) => areClose(p.position, playerPosition)}
      .map{case (r, p) => PlayerInfos(p.position, p.velocity, p.weight, r) }
      .toList

  private def getEnergiesInAOIOfPlayer(playerPosition: Vector2d, energies: Map[ActorRef, EnergyState]): List[EnergyInfos] =
    energies
      .filter{case (_, e) => areClose(e.position, playerPosition)}
      .map{case (r, e) => EnergyInfos(e.position, e.value, r) }
      .toList

  private def isNotTheSamePlayer(a1: ActorRef, a2: ActorRef) = a1 != a2

  private def areClose(p1: Vector2d, p2: Vector2d)  = p1.euclideanDistance(p2) <= RADIUS_AREA_OF_INTEREST
}