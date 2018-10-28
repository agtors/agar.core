package com.agar.core

import akka.actor.{Actor, ActorRef, Props}
import com.agar.core.Arbitrator.Played
import com.agar.core.Logger.Movement
import com.agar.core.Player.Move

//#player-companion

object Player {
  def props(number: Int, position: (Int, Int), loggerActor: ActorRef): Props = Props(new Player(number, position, loggerActor))

  //player-messages
  case object Move

}

//#player-companion

//#player-actor

// TODO - Weight to be added
class Player(number: Int, val initialPosition: (Int, Int), loggerActor: ActorRef) extends Actor {

  private var position: (Int, Int) = initialPosition

  def receive = {
    case Move =>
      position = (position._1 + 1, position._2 + 1) // TODO -- Move
      loggerActor ! Movement(number, position) // Log movement
      sender() ! Played(number, position) // Notify played movement
  }
}

//#player-actor
