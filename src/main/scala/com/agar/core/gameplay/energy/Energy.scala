package com.agar.core.gameplay.energy

import akka.actor.{Actor, Props}
import com.agar.core.gameplay.energy.Energy.{Consume, Consumed}

object Energy {
  def props(value: Int): Props = Props(new Energy(value))

  case object Consume

  case class Consumed(value: Int)

}

class Energy(value: Int) extends Actor {

  def receive: PartialFunction[Any, Unit] = {
    case Consume =>
      sender ! Consumed(value)
      context become consumed
  }

  def consumed: PartialFunction[Any, Unit] = {
    case Consume =>
      ()
  }

}
