package com.agar.core.region

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import com.agar.core.context.AgarSystem
import com.agar.core.logger.Journal.WorldState
import com.agar.core.region.Protocol._
import com.agar.core.region.State.PlayerState
import com.agar.core.utils.Vector2d
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._
import scala.language.postfixOps

class RegionTest(_system: ActorSystem)
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
      implicit val context: AgarSystem = () => 2 seconds

      val journal = TestProbe()
      val bridge = TestProbe()
      val region = system.actorOf(Region.props(7680, 4320, 0)(journal.ref, bridge.ref))

      region ! InitRegion(2, 2)

      val expectedCorrectInit = journal.expectMsgPF() {
        case Initialized(players, energies) => {
          players.size === 2 && energies.size === 2
        }
      }

      expectedCorrectInit should be(true)
    }

    "initialize a fresh region with one fresh player in the frontier" in {
      implicit val context: AgarSystem = () => 2 seconds

      val journal = TestProbe()
      val bridge = TestProbe()
      val region = system.actorOf(Region.props(10, 10, 10)(journal.ref, bridge.ref))

      region ! InitRegion(1, 0)

      val expectedCorrectInit = journal.expectMsgPF() {
        case Initialized(players, energies) => {
          players.size === 1 && energies.size === 0
        }
      }

      expectedCorrectInit should be(true)

      bridge.expectMsgPF(500 millis) {
        case Virtual(RegisterPlayer(_, _)) => true
      }

    }
  }

  "move a player in the frontier" in {
    implicit val context: AgarSystem = () => 2 seconds

    val journal = TestProbe()
    val bridge = TestProbe()
    val region = system.actorOf(Region.props(10, 10, 5)(journal.ref, bridge.ref))

    region ! InitRegion(0, 0)

    journal.expectMsgPF(500 millis) {
      case Initialized(_, _) => ()
    }

    region ! CreatePlayer(PlayerState(Vector2d(10, 10), 0, Vector2d(0, 0)))
    region ! GetEntitiesAOISet

    val player = journal.expectMsgPF(500 millis) { case WorldState(newPlayers, _) => newPlayers.toList.head._1 }

    region ! Move(player, Vector2d(10, 0), 0)

    val expectedCorrectMessage = bridge.expectMsgPF(500 millis) {
      case Virtual(RegisterPlayer(p, PlayerState(Vector2d(10, 0), 0, Vector2d(_, _), false))) => player === p
    }

    expectedCorrectMessage should be(true)
  }

  "move a player already in the frontier" in {
    implicit val context: AgarSystem = () => 2 seconds

    val journal = TestProbe()
    val bridge = TestProbe()
    val region = system.actorOf(Region.props(10, 10, 5)(journal.ref, bridge.ref))

    region ! InitRegion(0, 0)

    journal.expectMsgPF(500 millis) {
      case Initialized(_, _) => ()
    }

    region ! CreatePlayer(PlayerState(Vector2d(10, 4), 0, Vector2d(0, 0)))
    region ! GetEntitiesAOISet

    val player = journal.expectMsgPF(500 millis) { case WorldState(newPlayers, _) => newPlayers.toList.head._1 }

    region ! Move(player, Vector2d(10, -2), 0)

    bridge.expectMsgPF(500 millis) {
      case Virtual(RegisterPlayer(p, PlayerState(Vector2d(10, 4), 0, Vector2d(0, 0), false))) => player === p
    }

    val expectedCorrectMessage = bridge.expectMsgPF(500 millis) {
      case Virtual(Move(p, Vector2d(10, -2), 0)) => player === p
    }

    expectedCorrectMessage should be(true)
  }

  "move a player out the frontier" in {
    implicit val context: AgarSystem = () => 2 seconds

    val journal = TestProbe()
    val bridge = TestProbe()
    val region = system.actorOf(Region.props(10, 10, 5)(journal.ref, bridge.ref))

    region ! InitRegion(0, 0)

    journal.expectMsgPF(500 millis) {
      case Initialized(_, _) => ()
    }

    region ! CreatePlayer(PlayerState(Vector2d(10, 4), 0, Vector2d(0, 0)))
    region ! GetEntitiesAOISet

    val player = journal.expectMsgPF(500 millis) { case WorldState(newPlayers, _) => newPlayers.toList.head._1 }

    region ! Move(player, Vector2d(10, 6), 0)

    bridge.expectMsgPF(500 millis) {
      case Virtual(RegisterPlayer(p, PlayerState(Vector2d(10, 4), 0, Vector2d(0, 0), false))) => player === p
    }

    val expectedCorrectMessage = bridge.expectMsgPF(500 millis) {
      case Virtual(Destroy(p)) => player === p
    }

    expectedCorrectMessage should be(true)
  }

  "kill a player in the frontier" in {
    implicit val context: AgarSystem = () => 2 seconds

    val journal = TestProbe()
    val bridge = TestProbe()
    val region = system.actorOf(Region.props(10, 10, 5)(journal.ref, bridge.ref))

    region ! InitRegion(0, 0)

    journal.expectMsgPF(500 millis) {
      case Initialized(_, _) => ()
    }

    region ! CreatePlayer(PlayerState(Vector2d(10, 4), 0, Vector2d(0, 0)))
    region ! GetEntitiesAOISet

    val player = journal.expectMsgPF(500 millis) { case WorldState(newPlayers, _) => newPlayers.toList.head._1 }

    region ! Killed(player)

    bridge.expectMsgPF(500 millis) {
      case Virtual(RegisterPlayer(p, PlayerState(Vector2d(10, 4), 0, Vector2d(0, 0), false))) => player === p
    }

    val expectedCorrectMessage = bridge.expectMsgPF(500 millis) {
      case Virtual(Killed(p)) => player === p
    }

    expectedCorrectMessage should be(true)
  }

  "kill a player in the region not in the frontier" in {
    implicit val context: AgarSystem = () => 2 seconds

    val journal = TestProbe()
    val bridge = TestProbe()
    val region = system.actorOf(Region.props(10, 10, 5)(journal.ref, bridge.ref))

    region ! InitRegion(0, 0)

    journal.expectMsgPF(500 millis) {
      case Initialized(_, _) => ()
    }

    region ! CreatePlayer(PlayerState(Vector2d(10, 6), 0, Vector2d(0, 0)))
    region ! GetEntitiesAOISet

    val player = journal.expectMsgPF(500 millis) { case WorldState(newPlayers, _) => newPlayers.toList.head._1 }

    region ! Killed(player)

    bridge.expectNoMessage(500 millis)

  }

  "move a player out the frontier and the region" in {
    implicit val context: AgarSystem = () => 2 seconds

    val journal = TestProbe()
    val bridge = TestProbe()
    val region = system.actorOf(Region.props(10, 10, 5)(journal.ref, bridge.ref))

    region ! InitRegion(0, 0)

    journal.expectMsgPF(500 millis) {
      case Initialized(_, _) => ()
    }

    region ! CreatePlayer(PlayerState(Vector2d(10, 7), 0, Vector2d(0, 0)))
    region ! GetEntitiesAOISet

    val player = journal.expectMsgPF() { case WorldState(newPlayers, _) => newPlayers.toList.head._1 }

    region ! Move(player, Vector2d(10, -7), 0)

    val expectedCorrectMessage = bridge.expectMsgPF(500 millis) {
      case Virtual(CreatePlayer(PlayerState(Vector2d(10, -7), 0, Vector2d(0, 0), false))) => true
    }

    expectedCorrectMessage should be(true)
  }

  "initialize a fresh region with one fresh energy in the frontier" in {
    implicit val context: AgarSystem = () => 2 seconds

    val journal = TestProbe()
    val bridge = TestProbe()
    val region = system.actorOf(Region.props(10, 10, 10)(journal.ref, bridge.ref))

    region ! InitRegion(0, 1)

    val expectedCorrectInit = journal.expectMsgPF() {
      case Initialized(players, energies) => {
        players.size === 0 && energies.size === 1
      }
    }

    expectedCorrectInit should be(true)

    bridge.expectMsgPF(500 millis) {
      case Virtual(RegisterEnergy(_, _)) => true
    }

  }

  "consume an energy in a frontier" in {
    implicit val context: AgarSystem = () => 2 seconds

    val journal = TestProbe()
    val bridge = TestProbe()
    val region = system.actorOf(Region.props(10, 10, 10)(journal.ref, bridge.ref))

    region ! InitRegion(0, 1)

    val expectedCorrectInit = journal.expectMsgPF() {
      case Initialized(players, energies) => {
        players.size === 0 && energies.size === 1
      }
    }

    expectedCorrectInit should be(true)

    val energy = bridge.expectMsgPF(500 millis) { case Virtual(RegisterEnergy(p, _)) => p }

    region ! Destroy(energy)

    val expectedCorrectMessage = bridge.expectMsgPF(500 millis) {
      case Virtual(Destroy(p)) => energy === p
    }

    expectedCorrectMessage should be(true)
  }

  class Tracer(a: ActorRef) extends Actor {
    override def receive: Receive = {
      case e =>
        a ! e
    }
  }

}
