package com.agar.core.tracer

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

object Tracer {

  def trace(actor: ActorRef)(implicit system: ActorSystem): ActorRef =
    system.actorOf(Props(new Tracer(actor)))

}

class Tracer(a: ActorRef) extends Actor with ActorLogging {
  override def receive: Receive = {
    case e =>
      //log.info("RECV", e.toString)
      a.tell(e, sender)
  }
}