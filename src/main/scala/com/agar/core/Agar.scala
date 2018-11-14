package com.agar.core


import akka.actor.{ActorRef, ActorSystem}
import com.agar.core.arbritrator.Arbitrator
import com.agar.core.arbritrator.Protocol.StartGameTurn
import com.agar.core.context.AgarSystem
import com.agar.core.logger.Journal
import com.agar.core.region.Protocol.InitRegion
import com.agar.core.region.Region

import scala.concurrent.duration._

object Agar extends App {

  val nbPlayer = getArg(0).getOrElse("2").toInt
  val nbEnergy = getArg(1).getOrElse("0").toInt

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

}
