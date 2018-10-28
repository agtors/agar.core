package com.agar.core.logger

import akka.actor.{Actor, ActorLogging, Props}
import com.agar.core.arbritrator.Arbitrator

//#logger-companion

object Logger {

  def props: Props = Props[Arbitrator]

  final case class Movement(number: Int, position: (Int, Int))

}

//#logger-companion

//#logger-actor

class Logger extends Actor with ActorLogging {

  import Logger._

  def receive = {
    case Movement(number, position) =>
      log.info(s"player $number at $position")
  }
}

//#logger-actor
