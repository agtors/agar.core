package com.agar.core.region

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.agar.core.context.{AgarAlgorithm, AgarContext, AgarPosition, AgarSystem}
import com.agar.core.region.Region.{InitRegion, Initialized}
import com.agar.core.utils.Point2d
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

class RegionTest (_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("AgarSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "A Region Actor" should {

    "initialize a fresh region with fresh entities" in {
      implicit val context: AgarContext = new AgarContext {
        override val system: AgarSystem = () => 2 seconds
        override val position: AgarPosition = () => Point2d(0, 0)
        override val algorithm: AgarAlgorithm = p => Point2d(p.x + 1, p.y + 1)
      }

      val testProbe = TestProbe()
      val tracer = system.actorOf(Props(new Tracer(testProbe.ref)))

      val region = system.actorOf(Region.props(ActorRef.noSender, tracer, 7680, 4320), "region")

      region ! InitRegion(2, 2)

      val expectedCorrectInit = testProbe.expectMsgPF() {
        case Initialized(players, energies) => {
          players.size === 2 && energies.size === 2
        }
      }

      expectedCorrectInit should be (true)
    }
  }

  class Tracer(a: ActorRef) extends Actor {
    override def receive: Receive = {
      case e =>
        a ! e
    }
  }
}
