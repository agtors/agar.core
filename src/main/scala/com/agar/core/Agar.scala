package com.agar.core

import akka.actor.{ActorRef, ActorSystem}
import com.agar.core.arbritrator.Arbitrator
import com.agar.core.arbritrator.Arbitrator.Start
import com.agar.core.context.{AgarContext, DefaultAgarContext}
import com.agar.core.logger.Logger

//#main-class

object Agar extends App {

  implicit val context: AgarContext = DefaultAgarContext

  val system: ActorSystem = ActorSystem("agar")
  val logger: ActorRef = system.actorOf(Logger.props, "logger")
  val arbitrator: ActorRef = system.actorOf(Arbitrator.props(logger), "arbitrator")

  arbitrator ! Start(100)
}

//#main-class
