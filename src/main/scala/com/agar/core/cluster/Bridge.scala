package com.agar.core.cluster

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._
import com.agar.core.cluster.Bridge.FromBridge
import com.agar.core.region.Protocol.Virtual

object Bridge {

  def props(port: Int): Props = Props(new Bridge(port))

  case class FromBridge(event: Any)

}

class Bridge(port: Int) extends Actor with ActorLogging {

  val regionAddress = s"akka.tcp://agar@127.0.0.1:$port/user/bridge"

  def receive: Receive = {

    case event@Virtual(_) =>
      context.actorSelection(regionAddress) ! event

  }

}
