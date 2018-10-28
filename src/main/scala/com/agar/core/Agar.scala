package com.agar.core

import akka.actor.{ActorRef, ActorSystem}
import com.agar.core.Arbitrator._

//#main-class
object Agar extends App {

  val system: ActorSystem = ActorSystem("agar")

  val logger: ActorRef = system.actorOf(Logger.props, "logger")
  val arbitrator: ActorRef = system.actorOf(Arbitrator.props(logger), "arbitrator")

  arbitrator ! Start(100)
}

//#main-class
