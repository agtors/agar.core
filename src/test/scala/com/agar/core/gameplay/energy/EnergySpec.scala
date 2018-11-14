package com.agar.core.gameplay.energy

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.agar.core.gameplay.energy.Energy.{Consumed, TryConsume}
import com.agar.core.region.Protocol.Destroy
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps

class EnergySpec(_system: ActorSystem)
  extends TestKit(_system)
    with ImplicitSender
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("AgarSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "An Energy Actor" should {
    "be consumed" in {
      val region = TestProbe()
      val energy = system.actorOf(Energy.props(10)(region.ref))

      energy ! TryConsume
      this.expectMsg(500 millis, Consumed(10))
      region.expectMsg(500 millis, Destroy(energy))
    }

    "be consumed only once" in {
      val region = TestProbe()
      val energy = system.actorOf(Energy.props(10)(region.ref))

      energy ! TryConsume
      this.expectMsg(500 millis, Consumed(10))
      region.expectMsg(500 millis, Destroy(energy))

      energy ! TryConsume
      this.expectNoMessage(500 millis)
      region.expectNoMessage(500 millis)

    }
  }

}

