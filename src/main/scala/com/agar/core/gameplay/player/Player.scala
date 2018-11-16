package com.agar.core.gameplay.player

import akka.actor.{Actor, ActorRef, Props}
import com.agar.core.arbritrator.Protocol.MovePlayer
import com.agar.core.gameplay.Behavior
import com.agar.core.gameplay.Behavior.TargetEntity
import com.agar.core.gameplay.energy.Energy.TryConsume
import com.agar.core.gameplay.player.Player.{CollectEnergy, State}
import com.agar.core.region.Protocol.Killed
import com.agar.core.utils.{Algorithm, Vector2d}

object Player {

  // The "brain" of an PlayerActor is implemented using a stack-based Finite "State" Machine: every "State" represents an action, such as "Pursuit" or "Evade".
  // The top of the stack contains the active state; transitions are handled by pushing or popping states from the stack.
  // NOTE: The FSM is describe in /doc/FSM_player.png
  sealed trait State

  case object LetsHuntThem extends State

  case object CollectEnergy extends State

  case object RunAway extends State

  case object Wander extends State

  def props(worldSquare: List[Double], position: Vector2d, weight: Int)(region: ActorRef): Props = Props(new Player(worldSquare, position, weight)(region))

  case class Tick(aoi: AOI)

  case object TryKill

  case object KilledPlayer

}

class Player(worldSquare: List[Double], var position: Vector2d, var weight: Int, var activeState: List[State] = List(CollectEnergy))(region: ActorRef) extends Actor {

  import com.agar.core.gameplay.player.Player._

  def MAX_VELOCITY: Short = 3

  var velocity: Vector2d = Vector2d(2, 2)

  override def receive(): Receive = {
    case Tick(areaOfInterest) =>
      val energies = energiesToConsume(areaOfInterest.energies)
      if (energies.nonEmpty) {
        energies.head.ref ! TryConsume
        sender ! MovePlayer(position, weight)
      } else {
        val players = playersToKill(areaOfInterest.players)
        if (players.nonEmpty) {
          players.head.ref ! TryKill
          sender ! MovePlayer(position, weight)
        } else {
          update(areaOfInterest)
          sender ! MovePlayer(Vector2d(position.x, position.y), weight)
        }
      }

    case TryKill =>
      region ! Killed(self)
      context become killed
  }

  def killed: Receive = {
    case Tick(_) =>
      ()
  }

  def update(areaOfInterest: AOI): Unit = {
    //The player pop from the stack, which means the current state is complete and the next state in the stack should become active.
    // He can just push a new state, which means the currently active state will change for a while,
    // but when it pops itself from the stack, the previously active state will take over again.
    val target = updateStateAndGetTarget(areaOfInterest)

    val steering = target match {
      case Some(Left(player)) => moveTowardTheTargetBasedOnState(player.intoTargetEntity())
      case Some(Right(energy)) => moveTowardTheTargetBasedOnState(energy.intoTargetEntity())
      case None => Behavior.wander(velocity, 10, 10) // TODO: find the correct scalar values for wanderDistance and wanderRadiu
    }

    moveBasedOnVelocity(steering)

  }

  def updateStateAndGetTarget(aoi: AOI): Option[Either[PlayerInfos, EnergyInfos]] = {
    this.activeState.head match {
      case LetsHuntThem => chaseThem(aoi)
      case RunAway => runForYourLife(aoi)
      case CollectEnergy => collectEnergy(aoi)
      case Wander => tryToFindAGoalInHisLife(aoi)
    }
  }

  private def chaseThem(aoi: AOI): Option[Either[PlayerInfos, EnergyInfos]] = {
    getNearestWeakPlayerAround(aoi.players) match {
      case None =>
        this.activeState.drop(1) // the weak player escaped
        this.activeState = Wander :: this.activeState
        None
      case Some(weakPlayer) => Some(Left(weakPlayer)) // continue the hunt
    }
  }

  private def runForYourLife(aoi: AOI): Option[Either[PlayerInfos, EnergyInfos]] = {
    getPositionOfTheNearestDangerousPlayerInThreatRadius(aoi.players) match {
      case None =>
        this.activeState.drop(1) // the dangerous player is distant
        this.activeState = Wander :: this.activeState
        None
      case Some(dangerousPlayer) => Some(Left(dangerousPlayer)) // continue to run away
    }
  }

  private def collectEnergy(aoi: AOI): Option[Either[PlayerInfos, EnergyInfos]] = {
    getPositionOfTheNearestDangerousPlayerInThreatRadius(aoi.players) match {
      case Some(dangerousPlayer) =>
        this.activeState = RunAway :: this.activeState
        return Some(Left(dangerousPlayer)) // start to run away
      case _ =>
    }

    getNearestWeakPlayerAround(aoi.players) match {
      case Some(weakPlayer) =>
        this.activeState = LetsHuntThem :: this.activeState
        return Some(Left(weakPlayer)) // start the hunt
      case _ =>
    }

    getPositionOfTheNearestEnergy(aoi.energies) match {
      case None =>
        this.activeState.drop(1)
        this.activeState = Wander :: this.activeState
        None
      case Some(energy) => Some(Right(energy)) // move toward this energy
    }
  }

  def tryToFindAGoalInHisLife(aoi: AOI): Option[Either[PlayerInfos, EnergyInfos]] = {
    getPositionOfTheNearestDangerousPlayerInThreatRadius(aoi.players) match {
      case Some(dangerousPlayer) =>
        this.activeState = RunAway :: this.activeState
        return Some(Left(dangerousPlayer)) // start to run away
      case _ =>
    }

    getNearestWeakPlayerAround(aoi.players) match {
      case Some(weakPlayer) =>
        this.activeState = LetsHuntThem :: this.activeState
        return Some(Left(weakPlayer)) // start the hunt
      case _ =>
    }

    getPositionOfTheNearestEnergy(aoi.energies) match {
      case None =>
        this.activeState.drop(1)
        this.activeState = CollectEnergy :: this.activeState
        None
      case Some(energy) => Some(Right(energy)) // move torward this energy
    }
  }

  private def moveTowardTheTargetBasedOnState(target: TargetEntity): Vector2d = {
    this.activeState.head match {
      case LetsHuntThem => Behavior.pursuit(target, position, velocity, MAX_VELOCITY)
      case RunAway => Behavior.evade(target, position, velocity, MAX_VELOCITY)
      case CollectEnergy => Behavior.seek(target.position, position, velocity, MAX_VELOCITY)
      case Wander => Vector2d(0, 0) // case catched above. We put Wander only to remove a warning
    }
  }

  private def moveBasedOnVelocity(steering: Vector2d): Unit = {
    this.velocity = truncateAt(velocity + steering, MAX_VELOCITY)
    this.position = Algorithm.clamp(position + velocity, worldSquare)
  }

  private def truncateAt(v: Vector2d, n: Double): Vector2d = {
    // TODO -- Remove these side effects
    if (v.x > MAX_VELOCITY) v.x = n
    if (v.y > MAX_VELOCITY) v.y = n
    v
  }

  //TODO: We could provide an AOI already ordered ?
  private def getNearestWeakPlayerAround(players: List[PlayerInfos]): Option[PlayerInfos] =
    players
      .filter(p => canEat(this.weight, p.weight))
      .sortBy(p => p.position.euclideanDistance(position))
      .headOption

  //TODO: We could provide an AOI already ordered ?
  private def getPositionOfTheNearestDangerousPlayerInThreatRadius(players: List[PlayerInfos]): Option[PlayerInfos] =
    players
      .filter(p => canEat(p.weight, this.weight))
      .sortBy(p => p.position.euclideanDistance(position))
      .headOption

  // The cell need to be 1/3 bigger than the other
  private def canEat(weight1: Int, weight2: Int): Boolean = weight1 * 0.75 >= weight2

  private def getPositionOfTheNearestEnergy(energies: List[EnergyInfos]): Option[EnergyInfos] =
    energies
      .sortBy(e => e.position.euclideanDistance(position))
      .headOption

  def playersToKill(active: List[PlayerInfos]): List[PlayerInfos] =
    active.filter { player2 => canKillThePlayer(player2) }

  def energiesToConsume(energies: List[EnergyInfos]): List[EnergyInfos] =
    energies.filter { energy => canConsumeTheEnergy(energy) }

  private def canKillThePlayer(player2: PlayerInfos): Boolean =
    this.position.euclideanDistance(player2.position) <= this.radiusPlayer() && this.weight > player2.weight

  private def canConsumeTheEnergy(energy: EnergyInfos): Boolean =
    this.position.euclideanDistance(energy.position) <= this.radiusPlayer()

  private def radiusPlayer(): Int =
    this.weight / 2

}
