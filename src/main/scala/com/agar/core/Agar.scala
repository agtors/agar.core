package com.agar.core

import akka.actor.ActorSystem
import com.agar.core.arbritrator.Arbitrator
import com.agar.core.arbritrator.Protocol.StartGameTurn
import com.agar.core.cluster.{AgarCluster, Bridge}
import com.agar.core.context.AgarSystem
import com.agar.core.logger.Journal
import com.agar.core.region.Protocol.InitRegion
import com.agar.core.region.Region
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._
import scala.language.postfixOps

object Agar extends App {

  val regionName = args(0)

  // Setup the configuration (still ugly)
  val regionConfig = ConfigFactory.load("regions")

  println("Configuration {}", regionConfig.getAnyRef(s"agar.$regionName"))

  val nbPlayers = regionConfig.getInt(s"agar.$regionName.entities.players")
  val nbEnergies = regionConfig.getInt(s"agar.$regionName.entities.energies")

  val width = regionConfig.getInt(s"agar.$regionName.map.width")
  val height = regionConfig.getInt(s"agar.$regionName.map.height")
  val frontier = regionConfig.getInt(s"agar.$regionName.map.frontier")

  val remotePort = regionConfig.getInt(s"agar.$regionName.cluster.remote")
  System.setProperty("PORT", regionConfig.getInt(s"agar.$regionName.cluster.port").toString)

  val httpHost = regionConfig.getString(s"agar.$regionName.http.host")
  val httpPort = regionConfig.getInt(s"agar.$regionName.http.port")

  // Contexts creation
  val seedConfig = ConfigFactory.load("agar-seeds")
  implicit val system: ActorSystem = ActorSystem("agar", seedConfig)
  implicit val context: AgarSystem = () => 200 millis

  // Actors creation
  val journal = system.actorOf(Journal.props(httpHost, httpPort))
  val bridge = system.actorOf(Bridge.props(remotePort))
  val region = system.actorOf(Region.props(width, height, frontier)(journal, bridge))
  val arbitrator = system.actorOf(Arbitrator.props(region))
  val cluster = system.actorOf(AgarCluster.props(() => {
    region ! InitRegion(nbPlayers, nbEnergies)
    arbitrator ! StartGameTurn
  }))

}

