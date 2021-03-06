package com.agar.core.gameplay.player

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.agar.core.arbritrator.Protocol.MovePlayer
import com.agar.core.context.AgarSystem
import com.agar.core.gameplay.energy.Energy.TryConsume
import com.agar.core.gameplay.player.Player.{Tick, TryKill}
import com.agar.core.utils.{RegionBoundaries, Vector2d}
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps

class PlayerSpec(_system: ActorSystem)
  extends TestKit(_system)
    with ImplicitSender
    with WordSpecLike
    with Matchers {

  def this() = this(ActorSystem("AgarSpec"))

  "A player" should {
    "wander when he has nothing around" in {
      implicit val agarSystem: AgarSystem = () => 1 second

      val region = TestProbe()
      val playerPosition = Vector2d(100, 100)
      val playerRef = system.actorOf(Player.props(RegionBoundaries(0, 0, 1000, 1000), playerPosition, 10)(region.ref))

      val aoi = AOI(List.empty, List.empty)

      playerRef ! Tick(aoi)
      this.expectMsgPF(500 millis) { case MovePlayer(position, weight) =>
        position != playerPosition && weight == 10 && lastSender == playerRef
      }
    }

    "run when a dangerous player enter in the AOI" in {
      implicit val agarSystem: AgarSystem = () => 1 second

      val region = TestProbe()
      val playerPosition = Vector2d(100, 100)
      val playerWeight = 10
      val playerRef = system.actorOf(Player.props(RegionBoundaries(0, 0, 1000, 1000), playerPosition, playerWeight)(region.ref))

      val aoi = AOI(
        List(
          PlayerInfos(Vector2d(playerPosition.x + 100, playerPosition.y + 100), Vector2d(2, 2), 90, ActorRef.noSender), // Most closest dangerous player
          PlayerInfos(Vector2d(playerPosition.x + 100, playerPosition.y + 100), Vector2d(2, 2), 5, ActorRef.noSender), // should not pursuit this one
          PlayerInfos(Vector2d(playerPosition.x + 200, playerPosition.y + 200), Vector2d(2, 2), 10, ActorRef.noSender), // do not worry
          PlayerInfos(Vector2d(playerPosition.x - 100, playerPosition.y - 150), Vector2d(2, 2), 5, ActorRef.noSender), // Most weak player
        ),
        List(EnergyInfos(Vector2d(playerPosition.x + 50, playerPosition.y + 50), 10, ActorRef.noSender)) // do not worry
      )

      playerRef ! Tick(aoi)

      this.expectMsg(500 millis, MovePlayer(Vector2d(97.87867965644035, 97.87867965644035), playerWeight))
    }

    "eat a player close to him and any Player is dangerous around him" in {
      implicit val agarSystem: AgarSystem = () => 1 second

      val testProbe = TestProbe()
      val tracer = system.actorOf(Props(new Tracer(testProbe.ref)))

      val region = TestProbe()
      val playerPosition = Vector2d(100, 100)
      val playerWeight = 40
      val playerRef = system.actorOf(Player.props(RegionBoundaries(0, 0, 1000, 1000), playerPosition, playerWeight)(region.ref))

      // Only weak players in the AOI
      val aoi = AOI(
        List(
          PlayerInfos(Vector2d(playerPosition.x + 2, playerPosition.y + 2), Vector2d(2, 2), playerWeight / 2, tracer), // Most closest weakest player
          PlayerInfos(Vector2d(playerPosition.x + 10, playerPosition.y + 10), Vector2d(2, 2), playerWeight, ActorRef.noSender), // do not worry they have the same weight
          PlayerInfos(Vector2d(playerPosition.x - 3, playerPosition.y - 3), Vector2d(2, 2), playerWeight / 2, ActorRef.noSender), // weak player
        ),
        List(EnergyInfos(Vector2d(playerPosition.x + 50, playerPosition.y + 50), 10, ActorRef.noSender)) // do not worry
      )

      playerRef ! Tick(aoi)

      testProbe.expectMsg(500 millis, TryKill)
      this.expectMsg(500 millis, MovePlayer(Vector2d(100, 100), playerWeight))
    }

    "collect energy peacefully when nobody is around" in {
      implicit val agarSystem: AgarSystem = () => 1 second

      val testProbe = TestProbe()

      val region = TestProbe()
      val playerPosition = Vector2d(100, 100)
      val playerWeight = 10
      val playerRef = system.actorOf(Player.props(RegionBoundaries(0, 0, 1000, 1000), playerPosition, playerWeight)(region.ref))

      val energyTracer = system.actorOf(Props(new Tracer(testProbe.ref)))

      // Only energies in the AOI
      val aoi = AOI(
        List.empty,
        List(
          EnergyInfos(Vector2d(playerPosition.x + 2, playerPosition.y + 2), 10, energyTracer), // closest energy
          EnergyInfos(Vector2d(playerPosition.x + 4, playerPosition.y + 4), 2, ActorRef.noSender),
        )
      )

      playerRef ! Tick(aoi)
      testProbe.expectMsg(500 millis, TryConsume)
      this.expectMsg(500 millis, MovePlayer(Vector2d(100, 100), playerWeight))
    }

    "wander when he has just players with same weight around" in {
      implicit val agarSystem: AgarSystem = () => 1 second

      val testProbe = TestProbe()
      val tracer = system.actorOf(Props(new Tracer(testProbe.ref)))

      val region = TestProbe()
      val playerPosition = Vector2d(100, 100)
      val playerWeight = 10
      val playerRef = system.actorOf(Player.props(RegionBoundaries(0, 0, 1000, 1000), playerPosition, playerWeight)(region.ref))

      // Only players with the same weight in AOI
      val aoi = AOI(
        List(
          PlayerInfos(Vector2d(playerPosition.x + 3, playerPosition.y - 2), Vector2d(2, 2), playerWeight, ActorRef.noSender),
          PlayerInfos(Vector2d(playerPosition.x + 5, playerPosition.y + 10), Vector2d(2, 2), playerWeight, ActorRef.noSender),
          PlayerInfos(Vector2d(playerPosition.x - 3, playerPosition.y - 3), Vector2d(2, 2), playerWeight, ActorRef.noSender),
        ),
        List.empty
      )

      playerRef ! Tick(aoi)

      this.expectMsgPF(500 millis) { case MovePlayer(position, weight) =>
        position != playerPosition && weight == playerWeight && lastSender == playerRef
      }
    }
  }

  class Tracer(a: ActorRef) extends Actor {
    override def receive: Receive = {
      case e =>
        a ! e
    }
  }

}