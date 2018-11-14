package com.agar.core.gameplay.energy

import akka.actor.{Actor, ActorRef, Props}
import com.agar.core.gameplay.energy.Energy.{Consumed, TryConsume}
import com.agar.core.region.Protocol.Destroy
import com.agar.core.utils.Vector2d

object Energy {
  def props(value: Int)(region: ActorRef): Props = Props(new Energy(value)(region))

  case object TryConsume

  case class Consumed(value: Int)

}

class Energy(value: Int)(region: ActorRef) extends Actor {

  def receive: Receive = {
    case TryConsume =>
      sender ! Consumed(value)
      region ! Destroy(self)
      context become consumed
  }

  def consumed: Receive = {
    case _ =>
      ()
  }

}
