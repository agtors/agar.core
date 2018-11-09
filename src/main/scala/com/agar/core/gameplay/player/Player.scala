package com.agar.core.gameplay.player

import akka.actor.{Actor, ActorRef, Props}
import com.agar.core.arbritrator.Arbitrator
import com.agar.core.utils.Vector2d

object Player {
  def props(number: Int): Props = Props(new Player(number))

object Player {

  def props(position: Vector2d, weight: Int)(regionActor: ActorRef): Props = Props(new Player(position, weight)(regionActor))

  case class Tick(aoi: AOI)
}

// TODO - Weight to be added
class Player (var position: Vector2d, var weight: Int, var activeState: List[State] = List(CollectEnergy))(regionActor: ActorRef) extends Actor {

  import com.agar.core.gameplay.player.Player._

  def MAX_VELOCITY: Double = 3
  var velocity: Vector2d = Vector2d(2, 2)

  override def receive(): Receive = {
    case Tick(areaOfInterest) =>
      update(areaOfInterest)
      sender ! Arbitrator.Played
  }

  def update(areaOfInterest: AOI) = {
    updateState(areaOfInterest)
    val steering = getSteeringBasedOnState
    moveBasedOnVelocity(steering)
  }

  def updateState(aoi: AOI) =  {
    this.activeState.head match {
      case LetsHuntThem => chaseThem(aoi)
      case RunAway => runForYourLife(aoi)
      case CollectEnergy => collectEnergy(aoi)
      case Wander => tryToFindAGoalInHisLife(aoi)
    };
  }

  def chaseThem(aoi: AOI): Unit = {

  }

  def runForYourLife(aoi: AOI) = {

  }

  def collectEnergy(aoi: AOI) = {

  }

  def tryToFindAGoalInHisLife(aoi: AOI): Unit = {

  }

  def getSteeringBasedOnState(): Vector2d = {
    Vector2d(0, 0)
  }

  def moveBasedOnVelocity(steering: Vector2d) = {
      this.velocity = truncateAt(velocity + steering, MAX_VELOCITY)
      this.position = position + velocity
  }

  private def truncateAt(v: Vector2d, n: Double): Vector2d = {
    if (v.x > MAX_VELOCITY) v.x = n
    if (v.y > MAX_VELOCITY) v.y = n
    v
  }
}