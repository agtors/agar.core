package com.agar.core.arbitrator

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.agar.core.arbritrator.Arbitrator
import com.agar.core.arbritrator.Arbitrator.Start
import com.agar.core.context.{AgarAlgorithm, AgarContext, AgarPosition, AgarSystem}
import com.agar.core.logger.Logger.{PlayerCreated, PlayerDestroyed, PlayerMoved}
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
    "by created when a game is started" in {

      implicit val context: AgarContext = new AgarContext {
        override val system: AgarSystem = () => 5 seconds
        override val position: AgarPosition = () => (0, 0)
        override val algorithm: AgarAlgorithm = p => (p._1 + 1, p._2 + 1)
      }

      val testProbe = TestProbe()
      val arbitrator = system.actorOf(Arbitrator.props(testProbe.ref), "arbitrator1")

      arbitrator ! Start(1)

      testProbe.expectMsg(500 millis, PlayerCreated(0, (0, 0)))
    }
  }

  "A Player Actor" should {
    "move once a game has been started" in {

      implicit val context: AgarContext = new AgarContext {
        override val system: AgarSystem = () => 5 seconds
        override val position: AgarPosition = () => (0, 0)
        override val algorithm: AgarAlgorithm = p => (p._1 + 1, p._2 + 1)
      }

      val testProbe = TestProbe()
      val arbitrator = system.actorOf(Arbitrator.props(testProbe.ref), "arbitrator2")

      arbitrator ! Start(1)

      testProbe.expectMsg(500 millis, PlayerCreated(0, (0, 0)))
      testProbe.expectMsg(500 millis, PlayerMoved(0, (1, 1)))
    }
  }

  "A Player Actor" should {
    "be create and destroyed due to timeout" in {

      implicit val context: AgarContext = new AgarContext {
        override val system: AgarSystem = () => 100 millis
        override val position: AgarPosition = () => (0, 0)
        override val algorithm: AgarAlgorithm = p => {
          Thread.sleep(1000)
          p
        }
      }

      val testProbe = TestProbe()
      val tracer = system.actorOf(Props(new Tracer(testProbe.ref)))
      val arbitrator = system.actorOf(Arbitrator.props(tracer), "arbitrator3")

      arbitrator ! Start(1)

      testProbe.expectMsg(500 millis, PlayerCreated(0, (0, 0)))
      testProbe.expectMsg(500 millis, PlayerDestroyed(0))
    }
  }

  class Tracer(a: ActorRef) extends Actor {
    override def receive: Receive = {
      case e =>
        a ! e
    }
  }

}

