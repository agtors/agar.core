package com.agar.core

import akka.actor.{ActorRef, ActorSystem}
import com.agar.core.arbritrator.Arbitrator
import com.agar.core.arbritrator.Protocol.StartGameTurn
import com.agar.core.cluster.{AgarCluster, Bridge}
import com.agar.core.context.AgarSystem
import com.agar.core.logger.Journal
import com.agar.core.region.Protocol.{InitRegion, Virtual}
import com.agar.core.region.Region
import com.agar.core.tracer.Tracer
import com.agar.core.utils.RegionBoundaries
import com.typesafe.config.{Config, ConfigFactory}

import scala.collection.JavaConverters._
import scala.concurrent.duration._
import scala.language.postfixOps

object Agar extends App {

  val regionName = args(0)

  if (isSingleMode)
    single(regionName)
  else
    cluster(regionName)

  def single(regionName: String) = {

    println("[AGAR] Starting in single mode")

    // Setup the configuration (still ugly)
    val regionConfig = ConfigFactory.load("regions")

    // Contexts creation
    implicit val system: ActorSystem = ActorSystem("agar")

    // Actors creation
    val journal: ActorRef = createJournal(regionName, regionConfig)
    val region = createRegion(single = true, regionName, regionConfig, journal, journal)
    val arbitrator = createArbitrator(region, 200 millis)

    startGame(region, arbitrator, regionConfig)
  }

  def cluster(regionName: String) = {

    println("[AGAR] Starting in cluster mode")

    // Setup the configuration
    val regionConfig = ConfigFactory.load("regions")
    val localPort = regionConfig.getValue(s"agar.$regionName.cluster.port")
    val remotePort = regionConfig.getInt(s"agar.$regionName.cluster.remote")

    val systemConfig = ConfigFactory.parseResources("agar-seeds.conf")
      .withValue("PORT", localPort)
      .resolve()

    // Contexts creation
    implicit val system: ActorSystem = ActorSystem("agar", systemConfig)

    // Actors creation

    val bridge = createBridge(remotePort, regionConfig)
    val journal = createJournal(regionName, regionConfig)
    val region = createRegion(single = false, regionName, regionConfig, journal, bridge)
    val arbitrator = createArbitrator(region, 200 millis)

    system.actorOf(AgarCluster.props(remotePort, () => {
      startGame(region, arbitrator, regionConfig)
    }))

  }

  private def startGame(region: ActorRef, arbitrator: ActorRef, regionConfig: Config): Unit = {
    println("[AGAR] Starting game ...")
    val nbPlayers = regionConfig.getInt(s"agar.$regionName.entities.players")
    val nbEnergies = regionConfig.getInt(s"agar.$regionName.entities.energies")
    region ! InitRegion(nbPlayers, nbEnergies)
    arbitrator ! StartGameTurn
  }

  private def createJournal(regionName: String, regionConfig: Config)(implicit system: ActorSystem): ActorRef = {
    val httpHost = regionConfig.getString(s"agar.$regionName.http.host")
    val httpPort = regionConfig.getInt(s"agar.$regionName.http.port")
    val journal = system.actorOf(Journal.props(httpHost, httpPort))
    journal
  }

  private def createBridge(remotePort: Int, regionConfig: Config)(implicit system: ActorSystem) = {
    system.actorOf(Bridge.props(remotePort))
  }

  private def createArbitrator(region: ActorRef, timeout: FiniteDuration)(implicit system: ActorSystem) = {
    implicit val context: AgarSystem = () => timeout

    system.actorOf(Arbitrator.props(region))
  }

  private def createRegion(single: Boolean, regionName: String, regionConfig: Config, journal: ActorRef, bridge: ActorRef)(implicit system: ActorSystem) = {
    val (worldSquare, regionSquare, frontierSquare) = getBoundaries(single, regionName, regionConfig)
    Tracer.trace(system.actorOf(Region.props(worldSquare, regionSquare, frontierSquare)(journal, bridge), "region"), {
      case Virtual(_) => true
      case _ => false
    })
  }

  private def getBoundaries(single: Boolean, regionName: String, regionConfig: Config) = {
    val regionSquare = RegionBoundaries(regionConfig.getDoubleList(s"agar.$regionName.map.region").asScala.toList.map {
      _.toDouble
    })

    val worldSquare =
      if (single)
        regionSquare
      else
        RegionBoundaries(regionConfig.getDoubleList(s"agar.world.map.region").asScala.toList.map {
          _.toDouble
        })

    val frontierSquare =
      if (single)
        RegionBoundaries(Double.MinValue, Double.MinValue, Double.MinValue, Double.MinValue)
      else
        RegionBoundaries(regionConfig.getDoubleList(s"agar.$regionName.map.frontier").asScala.toList.map {
          _.toDouble
        })

    (worldSquare, regionSquare, frontierSquare)
  }

  private def isSingleMode = {
    args.length == 2 && args(1) == "single"
  }

}

