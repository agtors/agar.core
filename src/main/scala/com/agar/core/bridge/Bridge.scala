package com.agar.core.bridge

import akka.actor.{Actor, ActorLogging, Props}
import com.agar.core.bridge.Bridge.FromBridge

object Bridge {

  def props(port: Int): Props = Props(new Bridge(port))

  case class FromBridge(event: Any)

}

class Bridge(port: Int) extends Actor with ActorLogging {

  val regionAddress = s"akka.tcp://agar@127.0.0.1:$port/user/region"

  override def receive: Receive = {

    case event =>
      log.info(event.toString)
      context.actorSelection(regionAddress) ! FromBridge(event)

  }

}