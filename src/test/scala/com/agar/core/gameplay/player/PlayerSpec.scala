package com.agar.core.gameplay.player

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.agar.core.arbritrator.Player.MovePlayer
import com.agar.core.context.AgarSystem
import com.agar.core.gameplay.energy.Energy.Consume
import com.agar.core.gameplay.player.Player.{Eat, Tick}
import com.agar.core.utils.Vector2d
import org.scalatest.{Matchers, WordSpecLike}

import scala.concurrent.duration._

class PlayerSpec (_system: ActorSystem)
  extends TestKit(_system)
    with WordSpecLike
    with Matchers {

  def this() = this(ActorSystem("AgarSpec"))

  "A player" should {
    "wander when he has nothing around" in {
      implicit val agarSystem: AgarSystem = () => 1 second

      val testProbe = TestProbe()
      val tracer = system.actorOf(Props(new Tracer(testProbe.ref)))

      val playerPosition = Vector2d(100, 100)
      val playerRef = system.actorOf(Props(new Player(playerPosition, 10)(tracer)))

      val aoi = AOI(List.empty, List.empty)

      playerRef ! Tick(aoi)
      testProbe.expectMsgPF() {
        case MovePlayer(ref, position) => playerRef == ref && position != playerPosition
      }
    }

    "run when a dangerous player enter in the AOI" in {
      implicit val agarSystem: AgarSystem = () => 1 second

      val testProbe = TestProbe()
      val tracer = system.actorOf(Props(new Tracer(testProbe.ref)))

      val playerPosition = Vector2d(100, 100)
      val playerWeigth = 10
      val playerRef = system.actorOf(Props(new Player(playerPosition, playerWeigth)(tracer)))

      val aoi = AOI(
        List(
          PlayerInfos(Vector2d(playerPosition.x + 100, playerPosition.y + 100), Vector2d(2,2), 90, ActorRef.noSender), // Most closest dangerous player
          PlayerInfos(Vector2d(playerPosition.x + 100, playerPosition.y + 100), Vector2d(2,2), 5, ActorRef.noSender), // should not pursuit this one
          PlayerInfos(Vector2d(playerPosition.x + 200, playerPosition.y + 200), Vector2d(2,2), 10, ActorRef.noSender), // do not worry
          PlayerInfos(Vector2d(playerPosition.x - 100, playerPosition.y - 150), Vector2d(2,2), 5, ActorRef.noSender), // Most weak player
        ),
        List(EnergyInfos(Vector2d(playerPosition.x + 50, playerPosition.y + 50), 10, ActorRef.noSender)) // do not worry
      )

      playerRef ! Tick(aoi)
      testProbe.expectMsg(500 millis, MovePlayer(playerRef, Vector2d(97.87867965644035, 97.87867965644035)))
    }

    "eat a player close to him and any Player is dangerous around him" in {
      implicit val agarSystem: AgarSystem = () => 1 second

      val testProbe = TestProbe()
      val tracer = system.actorOf(Props(new Tracer(testProbe.ref)))

      val playerPosition = Vector2d(100, 100)
      val playerWeigth = 40
      val playerRef = system.actorOf(Props(new Player(playerPosition, playerWeigth)(tracer)))

      // Only weak players in the AOI
      val aoi = AOI(
        List(
          PlayerInfos(Vector2d(playerPosition.x + 2, playerPosition.y + 2), Vector2d(2,2), playerWeigth / 2, tracer), // Most closest weakest player
          PlayerInfos(Vector2d(playerPosition.x + 10, playerPosition.y + 10), Vector2d(2,2), playerWeigth, ActorRef.noSender), // do not worry they have the same weight
          PlayerInfos(Vector2d(playerPosition.x - 3, playerPosition.y - 3), Vector2d(2,2), playerWeigth / 2, ActorRef.noSender), // weak player
        ),
        List(EnergyInfos(Vector2d(playerPosition.x + 50, playerPosition.y + 50), 10, ActorRef.noSender)) // do not worry
      )

      playerRef ! Tick(aoi)
      testProbe.expectMsg(500 millis, Eat)
      testProbe.expectMsg(500 millis, MovePlayer(playerRef, Vector2d(102.12132034355965, 102.12132034355965)))
    }

    "collect energy peacefully when nobody is around" in {
      implicit val agarSystem: AgarSystem = () => 1 second

      val testProbe = TestProbe()
      val tracer = system.actorOf(Props(new Tracer(testProbe.ref)))

      val playerPosition = Vector2d(100, 100)
      val playerWeigth = 10
      val playerRef = system.actorOf(Props(new Player(playerPosition, playerWeigth)(tracer)))

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
      testProbe.expectMsg(500 millis, Consume)
      testProbe.expectMsg(500 millis, MovePlayer(playerRef, Vector2d(102.12132034355965, 102.12132034355965)))
    }

    "wander when he has just players with same weight around" in {
      implicit val agarSystem: AgarSystem = () => 1 second

      val testProbe = TestProbe()
      val tracer = system.actorOf(Props(new Tracer(testProbe.ref)))

      val playerPosition = Vector2d(100, 100)
      val playerWeigth = 10
      val playerRef = system.actorOf(Props(new Player(playerPosition, playerWeigth)(tracer)))

      // Only players with the same weight in AOI
      val aoi = AOI(
        List(
          PlayerInfos(Vector2d(playerPosition.x + 3, playerPosition.y - 2), Vector2d(2,2), playerWeigth, ActorRef.noSender),
          PlayerInfos(Vector2d(playerPosition.x + 5, playerPosition.y + 10), Vector2d(2,2), playerWeigth, ActorRef.noSender),
          PlayerInfos(Vector2d(playerPosition.x - 3, playerPosition.y - 3), Vector2d(2,2), playerWeigth, ActorRef.noSender),
        ),
        List.empty
      )

      playerRef ! Tick(aoi)
      testProbe.expectMsgPF() {
        case MovePlayer(ref, position) => playerRef == ref && position != playerPosition
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