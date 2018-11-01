package com.agar.core.logger

import akka.actor.{Actor, ActorLogging, Props}
import com.agar.core.arbritrator.Arbitrator

//#logger-companion

object Logger {

  def props: Props = Props[Arbitrator]

  final case class PlayerCreated(number: Int, position: (Int, Int))

  final case class PlayerMoved(number: Int, position: (Int, Int))

  final case class PlayerDestroyed(number: Int)

}

//#logger-companion

//#logger-actor

class Logger extends Actor with ActorLogging {

  import Logger._

  def receive: PartialFunction[Any, Unit] = {
    case PlayerMoved(number, position) =>
      log.info(s"player $number at $position")
  }
}

//#logger-actor
