package com.agar.core.region


import java.util.UUID

import akka.actor.{Actor, ActorRef, Props, Stash}
import com.agar.core.arbritrator.{PlayerStatus, WaitingPlayer}
import com.agar.core.context.AgarContext
import com.agar.core.gameplay.energy.{Energy, EnergyStatus}
import com.agar.core.gameplay.player.Player
import com.agar.core.gameplay.player.Player.Init
import com.agar.core.utils.Point2d

object Region {

  case object GetEntitiesAOISet

  final case class InitRegion(nbOfPlayer: Int, nbOfStartingEnergy: Int)
  final case class Initialized(players: Map[Int, (ActorRef, PlayerStatus)], energies: Map[UUID, (ActorRef, EnergyStatus)])

  def props(arbitrator: ActorRef, logger: ActorRef, width: Int, height: Int)(implicit agarContext: AgarContext): Props = Props(new Region(arbitrator, logger)(width, height)(agarContext))
}

class Region(arbitrator: ActorRef, logger: ActorRef)(width: Int, height: Int)(implicit agarContext: AgarContext)  extends Actor with Stash {

  import Region._

  def uuid = UUID.randomUUID()

  var players: Map[Int, (ActorRef, PlayerStatus)] = Map.empty[Int, (ActorRef, PlayerStatus)]
  var energies: Map[UUID, (ActorRef, EnergyStatus)] = Map.empty[UUID, (ActorRef, EnergyStatus)]

  def initialized: Receive = {
    case GetEntitiesAOISet => sender ! ()
  }

  def receive = {
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
        val position = agarContext.position.fresh()

        player ! Init(new Point2d(r.nextInt(width), r.nextInt(height)))
        n -> (player, WaitingPlayer(position))
      }.toMap

    this.energies = (0 until nbOfStartingEnergy)
      .map { _ =>
        val id = UUID.randomUUID()
        val energy = freshEnergy(id, 1 + r.nextInt(10))
        val position = agarContext.position.fresh()
        id -> (energy, EnergyStatus(position))
      }.toMap
  }

  private def freshPlayer(n: Int): ActorRef = {
    context.actorOf(Player.props(n)(agarContext.algorithm), name = s"player-$n")
  }

  private def freshEnergy(id: UUID, valueOfEnergy : Int): ActorRef = {
    context.actorOf(Energy.props(valueOfEnergy), name = s"energy-$id")
  }
}
