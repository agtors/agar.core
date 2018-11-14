package com.agar.core.gameplay.player

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.TestKit
import com.agar.core.context.AgarSystem
import com.agar.core.region.State.{EnergyState, PlayerState}
import com.agar.core.utils.Vector2d
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.duration._

class AreaOfInterestSpec(_system: ActorSystem)
  extends TestKit(_system)
    with WordSpecLike
    with Matchers {

  def this() = this(ActorSystem("AgarSpec"))

  "An AreaOfInterest" should {
    "get the list of AOI for each players" in {
      implicit val context: AgarSystem = () => 2 seconds

      val playerStub1 = system.actorOf(Props(new StubActor()))
      val playerStub2 = system.actorOf(Props(new StubActor()))
      val playerStub3 = system.actorOf(Props(new StubActor()))

      val energyStub1 = system.actorOf(Props(new StubActor()))
      val energyStub2 = system.actorOf(Props(new StubActor()))

      val players = Map(
        playerStub1 -> PlayerState(Vector2d(100, 100), 10, Vector2d(2, 2)), // close to playerStub2
        playerStub2 -> PlayerState(Vector2d(300, 400), 10, Vector2d(2, 2)), // close to playerStub1
        playerStub3 -> PlayerState(Vector2d(10000, 10000), 10, Vector2d(2, 2)), // alone
      )

      val energies = Map(
        energyStub1 -> EnergyState(Vector2d(200, 200), 10), // energy for playerStub1, playerStub2
        energyStub2 -> EnergyState(Vector2d(9800, 9800), 10), // energy for playerStub3
      )

      val areaOfInterestSet = AreaOfInterest.getPlayersAOISet(players, energies)

      areaOfInterestSet.get(playerStub1) should be(
        Some(
          AOI(
            List(PlayerInfos(Vector2d(300, 400), Vector2d(2, 2), 10, playerStub2)),
            List(EnergyInfos(Vector2d(200, 200), 10, energyStub1))
          )
        )
      )

      areaOfInterestSet.get(playerStub2) should be(
        Some(
          AOI(
            List(PlayerInfos(Vector2d(100, 100), Vector2d(2, 2), 10, playerStub1)),
            List(EnergyInfos(Vector2d(200, 200), 10, energyStub1))
          )
        )
      )

      areaOfInterestSet.get(playerStub3) should be(
        Some(
          AOI(
            List.empty,
            List(EnergyInfos(Vector2d(9800, 9800), 10, energyStub2))
          )
        )
      )
    }
  }

  class StubActor() extends Actor {
    override def receive: Receive = {
      case e => sender ! e
    }
  }

}