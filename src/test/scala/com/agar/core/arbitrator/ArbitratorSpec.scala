package com.agar.core.arbitrator

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.agar.core.context.AgarSystem
import com.agar.core.arbritrator.Arbitrator
import com.agar.core.arbritrator.Arbitrator.NewGameTurn
import com.agar.core.arbritrator.Player.{DestroyPlayer, MovePlayer, Tick}
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

  "A Player Actor" should {
    "move once a game has been started" in {

      implicit val agarSystem: AgarSystem = () => 1 second

      val testProbe = TestProbe()
      val arbitrator = system.actorOf(Arbitrator.props(testProbe.ref))
      val player = system.actorOf(FakePlayer.props())

      arbitrator ! NewGameTurn(Map(player -> ()))

      testProbe.expectMsg(500 millis, MovePlayer(player, Point2d(1, 1)))
    }

    "is destroyed when timeout is reached" in {

      implicit val agarSystem: AgarSystem = () => 100 millis

      val testProbe = TestProbe()
      val arbitrator = system.actorOf(Arbitrator.props(testProbe.ref), "arbitrator3")
      val player = system.actorOf(FakePlayer.props(respond = false))

      arbitrator ! NewGameTurn(Map(player -> ()))

      testProbe.expectMsg(500 millis, DestroyPlayer(player))
    }
  }

  // FAKE PLAYER

  object FakePlayer {
    def props(respond: Boolean = true): Props = Props(new FakePlayer(respond))
  }

  class FakePlayer(respond: Boolean) extends Actor {

    def receive: PartialFunction[Any, Unit] = {
      case Tick(_) =>
        if (respond) {
          sender ! MovePlayer(self, Point2d(1, 1))
        }
    }

  }


}

