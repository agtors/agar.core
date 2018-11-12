package com.agar.core.arbitrator

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.agar.core.arbritrator.Arbitrator
import com.agar.core.arbritrator.ArbitratorProtocol.AOISet
import com.agar.core.arbritrator.Player.{DestroyPlayer, MovePlayer, StartGameTurn, Tick}
import com.agar.core.context.AgarSystem
import com.agar.core.gameplay.player.AOI
import com.agar.core.region.Region.GetEntitiesAOISet
import com.agar.core.utils.Point2d
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps

//#test-classes
class ArbitratorSpec(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {
  //#test-classes

  def this() = this(ActorSystem("AgarSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "An Arbitrator Actor" should {

    "ask for AOISet when game starts" in {

      implicit val agarSystem: AgarSystem = () => 1 second

      val testProbe = TestProbe()
      val arbitrator = system.actorOf(Arbitrator.props(ActorRef.noSender, testProbe.ref))

      arbitrator ! StartGameTurn

      testProbe.expectMsg(500 millis, GetEntitiesAOISet)

    }

    "ask for player to move when AOISet is received" in {

      implicit val agarSystem: AgarSystem = () => 1 second

      val testProbe = TestProbe()
      val arbitrator = system.actorOf(Arbitrator.props(ActorRef.noSender, testProbe.ref))
      val player = system.actorOf(FakePlayer.props())

      arbitrator ! StartGameTurn

      testProbe.expectMsg(500 millis, GetEntitiesAOISet)
      // Simulate region response
      arbitrator ! AOISet(Map(player -> AOI(List(), List())))
      testProbe.expectMsg(500 millis, MovePlayer(player, Point2d(1, 1)))

    }

    "ask for player to move twice when AOISet is received" in {

      implicit val agarSystem: AgarSystem = () => 1 second

      val testProbe = TestProbe()
      val arbitrator = system.actorOf(Arbitrator.props(ActorRef.noSender, testProbe.ref))
      val player = system.actorOf(FakePlayer.props())

      arbitrator ! StartGameTurn

      testProbe.expectMsg(500 millis, GetEntitiesAOISet)
      // Simulate region response
      arbitrator ! AOISet(Map(player -> AOI(List(), List())))
      testProbe.expectMsg(500 millis, MovePlayer(player, Point2d(1, 1)))

      testProbe.expectMsg(1500 millis, GetEntitiesAOISet)
      // Simulate region response
      arbitrator ! AOISet(Map(player -> AOI(List(), List())))
      testProbe.expectMsg(500 millis, MovePlayer(player, Point2d(2, 2)))

    }

    "ask for player to die when AOISet is received and timeout reached" in {

      implicit val agarSystem: AgarSystem = () => 100 millis

      val testProbe = TestProbe()
      val arbitrator = system.actorOf(Arbitrator.props(ActorRef.noSender, testProbe.ref))
      val player = system.actorOf(FakePlayer.props(respond = false))

      arbitrator ! StartGameTurn
      testProbe.expectMsg(500 millis, GetEntitiesAOISet)

      // Simulate region response
      arbitrator ! AOISet(Map(player -> AOI(List(), List())))
      testProbe.expectMsg(500 millis, DestroyPlayer(player))

    }
  }

  // FAKE PLAYER

  object FakePlayer {
    def props(respond: Boolean = true): Props = Props(new FakePlayer(respond))
  }

  class FakePlayer(respond: Boolean) extends Actor {

    var position = Point2d(0, 0)

    def receive: PartialFunction[Any, Unit] = {
      case Tick(_) =>
        if (respond) {
          position = Point2d(position.x + 1, position.y + 1)
          sender ! MovePlayer(self, position)
        }
    }

  }


}

