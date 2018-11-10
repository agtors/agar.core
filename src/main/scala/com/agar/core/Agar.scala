package com.agar.core

import akka.actor.{ActorRef, ActorSystem}
import com.agar.core.context.{AgarSystem, DefaultAgarSystem}
import com.agar.core.arbritrator.Arbitrator
import com.agar.core.logger.Logger
import com.agar.core.region.Region

//#main-class

object Agar extends App {

  implicit val context: AgarSystem = DefaultAgarSystem

  val system: ActorSystem = ActorSystem("Agar")
  val logger: ActorRef = system.actorOf(Logger.props, "logger")
  val arbitrator: ActorRef = system.actorOf(Arbitrator.props(logger),"arbitrator")

  // A region has a size of 4 screen 1920x1080
  val region = system.actorOf(Region.props(arbitrator, logger, 7680, 4320), "region")

}

//#main-class
