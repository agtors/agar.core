package com.agar.core.cluster

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

object AgarCluster {

  def props(port: Int, starter: () => Unit): Props = Props(new AgarCluster(port, starter))

}

class AgarCluster(port: Int, starter: () => Unit) extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive: Receive = {

    case MemberUp(member) ⇒
      if (member.address.port == Option(port)) {
        starter()
      }
    case UnreachableMember(member) ⇒
      println("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) ⇒
      println("Member is Removed: {} after {}", member.address, previousStatus)
    case _: MemberEvent ⇒ // ignore

  }

}
