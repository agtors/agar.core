package com.agar.core.player

import akka.actor.{Actor, ActorRef, Props}
import com.agar.core.arbritrator.Arbitrator.Played
import com.agar.core.logger.Logger.Movement
import com.agar.core.player.Player.{Init, Move}

//#player-companion

object Player {
  def props(number: Int, loggerActor: ActorRef): Props = Props(new Player(number, loggerActor))

  //player-messages
  case class Init(p: (Int, Int))

  case object Move

}

//#player-companion

//#player-actor

// TODO - Weight to be added
class Player(number: Int, loggerActor: ActorRef) extends Actor {

  def receive: PartialFunction[Any, Unit] = {
    case Init(p) =>
      context.become(playing(p))
  }

  def playing(p: (Int, Int)): PartialFunction[Any, Unit] = {
    case Move =>
      val np = (p._1 + 1, p._2 + 1)
      loggerActor ! Movement(number, np) // Log movement
      sender() ! Played(number, np) // Notify played movement
      context.become(playing(np))
  }
}

//#player-actor
