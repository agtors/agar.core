//#full-example
package com.agar.core

import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.agar.core.Logger.Movement
import com.agar.core.Player._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps

//#test-classes
class AgarSpec(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {
  //#test-classes

  def this() = this(ActorSystem("AgarSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  //#first-test
  //#specification-example
  "A Player Actor" should {
    "move when Move is received" in {
      //#specification-example
      val testProbe = TestProbe()
      val player = system.actorOf(Player.props(0, (0,0), testProbe.ref))
      player ! Move
      testProbe.expectMsg(500 millis, Movement(0, (1, 1)))
    }
  }
  //#first-test
}

//#full-example
