package com.agar.core.cluster

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.Cluster
import akka.cluster.ClusterEvent._

object AgarCluster {

  def props(starter: () => Unit): Props = Props(new AgarCluster(starter))

  case class FromBridge(event: Any)

}

class AgarCluster(starter: () => Unit) extends Actor with ActorLogging {

  val cluster = Cluster(context.system)

  override def preStart(): Unit = {
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[MemberEvent], classOf[UnreachableMember])
  }

  override def postStop(): Unit = cluster.unsubscribe(self)

  def receive: Receive = {

    case MemberUp(member) ⇒
      if (member.address.port != self.path.address.port) {
        log.info("Member is Up: {}", member.address)
        starter()
      }
    case UnreachableMember(member) ⇒
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) ⇒
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
    case _: MemberEvent ⇒ // ignore

  }

}
