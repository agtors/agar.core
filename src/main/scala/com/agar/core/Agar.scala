package com.agar.core


import akka.actor.{ActorRef, ActorSystem}
import com.agar.core.context.{AgarSystem, DefaultAgarSystem}
import com.agar.core.arbritrator.Arbitrator
import com.agar.core.arbritrator.Player.StartGameTurn
import com.agar.core.logger.Journal
import com.agar.core.region.Region
import com.agar.core.region.Region.InitRegion

//#main-class

object Agar extends App {

  implicit val context: AgarSystem = DefaultAgarSystem

  implicit val system: ActorSystem = ActorSystem("Agar")
  val journal: ActorRef = system.actorOf(Journal.props)

  // A region has a size of 4 screen 1920x1080
  val region = system.actorOf(Region.props(journal, 7680, 4320), "region")

  val arbitrator: ActorRef = system.actorOf(Arbitrator.props(region),"arbitrator")
}

//#main-class
