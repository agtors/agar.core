package com.agar.core.region

import akka.actor.ActorRef
import com.agar.core.region.State.{EnergyState, PlayerState}

object Conflicts {

  type Players = Map[ActorRef, PlayerState]
  type Eaten = List[ActorRef]
  type Energies = Map[ActorRef, EnergyState]

  def solve(players: Players, energies: Energies): (Players, Energies, Eaten) = {
    val (players1, eaten1) = solvePlayers(players)
    val (players2, energies2, eaten2) = solveEnergies(players1, energies)

    (players2, energies2, eaten1 ::: eaten2)
  }

  def solvePlayers(players: Players): (Players, Eaten) = {
    players.foldRight[(Players, Eaten)]((Map(), List())) { (player, result) => solvePlayers(player, result) }
  }

  def solveEnergies(players: Players, energies: Energies): (Players, Energies, Eaten) = {
    players.foldRight[(Players, Energies, Eaten)]((Map(), energies, List())) { (player, result) => solveEnergies(player, result) }
  }

  private def solvePlayers(player: (ActorRef, PlayerState), result: (Players, Eaten)): (Players, Eaten) = {
    val active = result._1
    val eaten = result._2
    val (player1, state1) = player

    val eaters = active.filter { case (_, state2) => canEatThePlayer(state2, state1) }
    if (eaters.nonEmpty) {
      val (player2, state2) = eaters.head // Pick up the first one (?)
      val newActive = active + (player2 -> (state2 increaseWeight state1.weight))
      val newEaten = eaten :+ player1
      return (newActive, newEaten)
    }

    val canEat = active.filter { case (_, state2) => canEatThePlayer(state1, state2) }
    if (canEat.nonEmpty) {
      val newActive = removeEaten(active, canEat) + (player1 -> eatAllPlayers(state1, canEat))
      val newEaten = eaten ::: canEat.toList.map {
        _._1
      }
      return (newActive, newEaten)
    }

    (active + (player1 -> state1), eaten)
  }

  private def solveEnergies(player: (ActorRef, PlayerState), result: (Players, Energies, Eaten)): (Players, Energies, Eaten) = {
    val active = result._1
    val energies = result._2
    val eaten = result._3
    val (player1, state1) = player

    val canEat = energies.filter { case (_, state2) => canConsumeTheEnergie(state1, state2) }
    if (canEat.nonEmpty) {
      val newActive = active + (player1 -> eatAllEnergies(state1, canEat))
      val newEnergies = removeEaten(energies, canEat)
      val newEaten = eaten ::: canEat.toList.map {
        _._1
      }

      return (newActive, newEnergies, newEaten)
    }

    (active + (player1 -> state1), energies, eaten)
  }

  private def removeEaten[A](active: Map[ActorRef, A], canEat: Map[ActorRef, A]): Map[ActorRef, A] = {
    active.filter { case (player2, _) => canEat.get(player2).isEmpty }
  }

  private def eatAllPlayers(state1: PlayerState, canEat: Players): PlayerState = {
    canEat.foldRight(state1) { (player2, state1) => state1 increaseWeight player2._2.weight }
  }

  private def eatAllEnergies(state1: PlayerState, canEat: Energies): PlayerState = {
    canEat.foldRight(state1) { (energy, state1) => state1 increaseWeight energy._2.value }
  }

  private def canEatThePlayer(player1: PlayerState, player2: PlayerState): Boolean =
    player1.position.euclideanDistance(player2.position) <= this.radiusPlayer(player1) && player1.weight > player2.weight

  private def canConsumeTheEnergie(player: PlayerState, energy: EnergyState): Boolean =
    player.position.euclideanDistance(energy.position) <= this.radiusPlayer(player)

  private def radiusPlayer(player: PlayerState): Int =
    player.weight / 2

}
