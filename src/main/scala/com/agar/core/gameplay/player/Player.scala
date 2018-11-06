package com.agar.core.gameplay.player

import akka.actor.{Actor, Props}
import com.agar.core.arbritrator.Arbitrator
import com.agar.core.context.AgarAlgorithm
import com.agar.core.gameplay.player.Player.{Init, Move}
import com.agar.core.utils.Point2d

object Player {
  def props(number: Int)(implicit algorithm: AgarAlgorithm): Props = Props(new Player(number, algorithm))

  case class Init(p: Point2d)

  case object Move

}

// TODO - Weight to be added
class Player(number: Int, implicit val algorithm: AgarAlgorithm) extends Actor {

  def receive: PartialFunction[Any, Unit] = {
    case Init(p) =>
      context.become(playing(p))
  }

  def playing(p: Point2d): PartialFunction[Any, Unit] = {
    case Move =>
      val np = algorithm.move(p)
      sender ! Arbitrator.Played(number, np)
      context.become(playing(np))
  }
}
