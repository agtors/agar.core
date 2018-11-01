package com.agar.core.player

import akka.actor.{Actor, Props}
import com.agar.core.arbritrator.Arbitrator
import com.agar.core.context.AgarAlgorithm
import com.agar.core.player.Player.{Init, Move}

//#player-companion

object Player {
  def props(number: Int)(implicit algorithm: AgarAlgorithm): Props = Props(new Player(number, algorithm))

  //player-messages
  case class Init(p: (Int, Int))

  case object Move

}

//#player-companion

//#player-actor

// TODO - Weight to be added
class Player(number: Int, implicit val algorithm: AgarAlgorithm) extends Actor {

  def receive: PartialFunction[Any, Unit] = {
    case Init(p) =>
      context.become(playing(p))
  }

  def playing(p: (Int, Int)): PartialFunction[Any, Unit] = {
    case Move =>
      val np = algorithm.move(p)
      sender ! Arbitrator.Played(number, np)
      context.become(playing(np))
  }
}

//#player-actor
