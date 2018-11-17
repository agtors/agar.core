package com.agar.core.tracer

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

object Tracer {

  def trace(actor: ActorRef, filter: Any => Boolean)(implicit system: ActorSystem): ActorRef =
    system.actorOf(Props(new Tracer(actor, filter)))

}

class Tracer(a: ActorRef, filter: Any => Boolean) extends Actor with ActorLogging {
  override def receive: Receive = {
    case e =>
      if (filter(e)) println(e.toString)
      a.tell(e, sender)
  }
}