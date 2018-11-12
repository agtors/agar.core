package com.agar.core


import akka.actor.{ActorRef, ActorSystem}
import com.agar.core.arbritrator.Arbitrator
<<<<<<< HEAD
import com.agar.core.arbritrator.Protocol.StartGameTurn
import com.agar.core.context.AgarSystem
import com.agar.core.logger.Journal
import com.agar.core.region.Protocol.InitRegion
import com.agar.core.region.Region

import scala.concurrent.duration._
=======
import com.agar.core.arbritrator.Player.StartGameTurn
import com.agar.core.bridge.Bridge
import com.agar.core.context.{AgarSystem, DefaultAgarSystem}
import com.agar.core.logger.Logger
import com.agar.core.region.Region
import com.agar.core.region.Region.InitRegion
import com.typesafe.config.ConfigFactory
>>>>>>> Add first definitions and bridge actor for clustering

object Agar extends App {

<<<<<<< HEAD
  val nbPlayer = getArg(0).getOrElse("2").toInt
  val nbEnergy = getArg(1).getOrElse("0").toInt

<<<<<<< HEAD
  implicit val system: ActorSystem = ActorSystem("agar")
  implicit val context: AgarSystem = () => 1 second

  // Actors creation
  val journal: ActorRef = system.actorOf(Journal.props)
  val region = system.actorOf(Region.props(journal, 7680, 4320), "region")
  val arbitrator = system.actorOf(Arbitrator.props(region), "arbitrator")

  // Initialize and start the game
  region ! InitRegion(nbPlayer, nbEnergy)
  arbitrator ! StartGameTurn

  def getArg(index: Int): Option[String] = {
    if (index < args.length) {
      Option(args(index))
    } else {
      Option.empty
    }
  }
=======
=======
>>>>>>> Review test and change arguments according to the new protocol
  val nbPlayer = args(0).toInt
  val nbEnergy = args(1).toInt
  val remotePort = args(2).toInt

  val seedConfig = ConfigFactory.load("seed")

  implicit val system: ActorSystem = ActorSystem("agar", seedConfig)
  implicit val context: AgarSystem = DefaultAgarSystem

  val journal = system.actorOf(Logger.props, "logger")
  val bridge = system.actorOf(Bridge.props(remotePort))
  val region = system.actorOf(Region.props(bridge, journal, 7680, 4320), "region")
  val arbitrator: ActorRef = system.actorOf(Arbitrator.props(bridge, region), "arbitrator")

  region ! InitRegion(nbPlayer, nbEnergy)
  arbitrator ! StartGameTurn
>>>>>>> Add first definitions and bridge actor for clustering

}
