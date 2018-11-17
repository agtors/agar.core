package com.agar.core.gameplay.energy

import akka.actor.{Actor, ActorRef, Props}
import com.agar.core.gameplay.energy.Energy.TryConsume
import com.agar.core.gameplay.player.Player.Consumed
import com.agar.core.region.Protocol.Destroy

object Energy {
  def props(value: Int)(region: ActorRef): Props = Props(new Energy(value)(region))

  case object TryConsume

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
