package com.agar.core.gameplay.player

import akka.actor.{Actor, ActorRef, Props}

object VirtualPlayer {

  def props(bridge: ActorRef): Props = Props(new VirtualPlayer(bridge))

}

class VirtualPlayer(bridge: ActorRef) extends Actor {
  override def receive: Receive = {
    case e => bridge ! e
  }
}
