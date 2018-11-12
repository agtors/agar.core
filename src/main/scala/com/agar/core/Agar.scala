package com.agar.core

import akka.actor.{ActorRef, ActorSystem}
import com.agar.core.arbritrator.Arbitrator
import com.agar.core.arbritrator.Player.StartGameTurn
import com.agar.core.bridge.Bridge
import com.agar.core.context.{AgarSystem, DefaultAgarSystem}
import com.agar.core.logger.Logger
import com.agar.core.region.Region
import com.agar.core.region.Region.InitRegion
import com.typesafe.config.ConfigFactory

object Agar extends App {

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

}
