package com.agar.core.cluster

import akka.actor.{Actor, ActorLogging, Props}
import com.agar.core.region.Protocol.Virtual

object Bridge {

  def props(port: Int): Props = Props(new Bridge(port))

}

class Bridge(port: Int) extends Actor with ActorLogging {

  val regionAddress = s"akka.tcp://agar@127.0.0.1:$port/user/region"

  def receive: Receive = {

    case v@Virtual(e) =>
      context.actorSelection(regionAddress) ! v
  }

}
