package com.agar.core.logger

import akka.actor.{Actor, ActorLogging, Props}
import com.agar.core.arbritrator.Arbitrator

//#logger-companion

object Logger {

  def props: Props = Props[Logger]

  final case class PlayerCreated(number: Int, position: (Int, Int))

  final case class PlayerMoved(number: Int, position: (Int, Int))

  final case class PlayerDestroyed(number: Int)

}

//#logger-companion

//#logger-actor

class Logger extends Actor with ActorLogging {

  import Logger._

  def receive: PartialFunction[Any, Unit] = {
    case PlayerCreated(number, position) =>
      // log.info(s"player $number created at $position")

    case PlayerMoved(number, position) =>
      // log.info(s"player $number move at $position")

    case PlayerDestroyed(number) =>
      log.info(s"player $number destroyed")
  }
}

//#logger-actor
