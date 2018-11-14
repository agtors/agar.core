package com.agar.core.region

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.TestKit
import com.agar.core.arbritrator.Protocol.MovePlayer
import com.agar.core.gameplay.player.Player.Tick
import com.agar.core.region.State.PlayerState
import com.agar.core.utils.Vector2d
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class ConflictsTest(_system: ActorSystem)
  extends TestKit(_system)
    with Matchers
    with WordSpecLike
    with BeforeAndAfterAll {

  def this() = this(ActorSystem("AgarSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "A Conflict resolution" should {

    "returns nothing when no player is active" in {
      val (active, eaten) = Conflicts.solvePlayers(Map())

      active should be(Map())
      eaten should be(List())
    }

    "returns single active when there is only one" in {
      val player = system.actorOf(FakePlayer.props()) -> PlayerState(Vector2d(0, 0), 0, Vector2d(0, 0))
      val (active, eaten) = Conflicts.solvePlayers(Map(player))

      active should be(Map(player._1 -> player._2))
      eaten should be(List())
    }

    "returns single active and single eaten when it's in the radius" in {
      val player1 = system.actorOf(FakePlayer.props()) -> PlayerState(Vector2d(0, 0), 10, Vector2d(0, 0))
      val player2 = system.actorOf(FakePlayer.props()) -> PlayerState(Vector2d(0, 0), 5, Vector2d(0, 0))
      val (active, eaten) = Conflicts.solvePlayers(Map(player1, player2))

      active should be(Map(player1._1 -> (player1._2 increaseWeight player2._2.weight)))
      eaten should be(List(player2._1))
    }

    "returns single active and single eaten when it's in the radius (2nd)" in {
      val player1 = system.actorOf(FakePlayer.props()) -> PlayerState(Vector2d(0, 0), 5, Vector2d(0, 0))
      val player2 = system.actorOf(FakePlayer.props()) -> PlayerState(Vector2d(0, 0), 10, Vector2d(0, 0))
      val (active, eaten) = Conflicts.solvePlayers(Map(player1, player2))

      active should be(Map(player2._1 -> (player2._2 increaseWeight player1._2.weight)))
      eaten should be(List(player1._1))
    }

    "returns two active players" in {
      val player1 = system.actorOf(FakePlayer.props()) -> PlayerState(Vector2d(0, 0), 5, Vector2d(0, 0))
      val player2 = system.actorOf(FakePlayer.props()) -> PlayerState(Vector2d(100, 100), 10, Vector2d(0, 0))
      val (active, eaten) = Conflicts.solvePlayers(Map(player1, player2))

      active should be(Map(player1, player2))
      eaten should be(List())
    }
  }

  // FAKE PLAYER

  object FakePlayer {
    def props(respond: Boolean = true): Props = Props(new FakePlayer(respond))
  }

  class FakePlayer(respond: Boolean) extends Actor {

    var position = Vector2d(0, 0)

    def receive: PartialFunction[Any, Unit] = {
      case Tick(_) =>
        if (respond) {
          position = Vector2d(position.x + 1, position.y + 1)
          sender ! MovePlayer(position)
        }
    }

  }

}
