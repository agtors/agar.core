package com.agar.core

import akka.actor.ActorSystem
import com.agar.core.arbritrator.Arbitrator
import com.agar.core.arbritrator.Protocol.StartGameTurn
import com.agar.core.cluster.{AgarCluster, Bridge}
import com.agar.core.context.AgarSystem
import com.agar.core.logger.Journal
import com.agar.core.region.Protocol.InitRegion
import com.agar.core.region.Region
import com.agar.core.utils.RegionBoundaries
import com.typesafe.config.ConfigFactory

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.postfixOps

object Agar extends App {

  val regionName = args(0)
  if (args.length == 2 && args(1) == "single") single(regionName) else cluster(regionName)

  def single(regionName: String) = {

    // Setup the configuration (still ugly)
    val regionConfig = ConfigFactory.load("regions")

    println("Configuration {}", regionConfig.getAnyRef(s"agar.$regionName"))

    val nbPlayers = regionConfig.getInt(s"agar.$regionName.entities.players")
    val nbEnergies = regionConfig.getInt(s"agar.$regionName.entities.energies")

    val regionSquare = RegionBoundaries(regionConfig.getDoubleList(s"agar.$regionName.map.region").asScala.toList.map {
      _.toDouble
    })
    val frontierSquare = RegionBoundaries(Double.MinValue, Double.MinValue, Double.MinValue, Double.MinValue)

    val httpHost = regionConfig.getString(s"agar.$regionName.http.host")
    val httpPort = regionConfig.getInt(s"agar.$regionName.http.port")

    // Contexts creation
    implicit val system: ActorSystem = ActorSystem("agar")
    implicit val context: AgarSystem = () => 200 millis

    // Actors creation
    val journal = system.actorOf(Journal.props(httpHost, httpPort))
    val region = system.actorOf(Region.props(regionSquare, regionSquare, frontierSquare)(journal, journal), "region")
    val arbitrator = system.actorOf(Arbitrator.props(region))

    region ! InitRegion(nbPlayers, nbEnergies)
    arbitrator ! StartGameTurn

  }

  def cluster(regionName: String) = {

    // Setup the configuration (still ugly)
    val regionConfig = ConfigFactory.load("regions")

    println("Configuration {}", regionConfig.getAnyRef(s"agar.$regionName"))

    val nbPlayers = regionConfig.getInt(s"agar.$regionName.entities.players")
    val nbEnergies = regionConfig.getInt(s"agar.$regionName.entities.energies")

    val worldSquare = RegionBoundaries(regionConfig.getDoubleList(s"agar.world.map.region").asScala.toList.map {
      _.toDouble
    })
    val regionSquare = RegionBoundaries(regionConfig.getDoubleList(s"agar.$regionName.map.region").asScala.toList.map {
      _.toDouble
    })
    val frontierSquare = RegionBoundaries(regionConfig.getDoubleList(s"agar.$regionName.map.frontier").asScala.toList.map {
      _.toDouble
    })

    val remotePort = regionConfig.getInt(s"agar.$regionName.cluster.remote")
    System.setProperty("PORT", regionConfig.getInt(s"agar.$regionName.cluster.port").toString)

    val httpHost = regionConfig.getString(s"agar.$regionName.http.host")
    val httpPort = regionConfig.getInt(s"agar.$regionName.http.port")

    // Contexts creation
    implicit val system: ActorSystem = ActorSystem("agar", ConfigFactory.load("agar-seeds"))
    implicit val context: AgarSystem = () => 200 millis

    // Actors creation
    val journal = system.actorOf(Journal.props(httpHost, httpPort))
    val bridge = system.actorOf(Bridge.props(remotePort))
    val region = system.actorOf(Region.props(worldSquare, regionSquare, frontierSquare)(journal, bridge), "region")
    val arbitrator = system.actorOf(Arbitrator.props(region))

    val cluster = system.actorOf(AgarCluster.props(() => {
      region ! InitRegion(nbPlayers, nbEnergies)
      arbitrator ! StartGameTurn
    }))

  }
}

