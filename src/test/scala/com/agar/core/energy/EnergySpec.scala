package com.agar.core.energy

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.agar.core.energy.Energy.{Consume, Consumed}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps

//#test-classes
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
    "can be consumed" in {

      val energy = system.actorOf(Energy.props(10))

      energy ! Consume
      this.expectMsg(500 millis, Consumed(10))

    }

    "can be consumed only once" in {

      val energy = system.actorOf(Energy.props(10))

      energy ! Consume
      this.expectMsg(500 millis, Consumed(10))

      energy ! Consume
      this.expectNoMessage(500 millis)

    }
  }

}

