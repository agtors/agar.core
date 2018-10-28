package com.agar.core

import akka.actor.{Actor, ActorLogging, Props}

//#logger-companion

object Logger {

  def props: Props = Props[Arbitrator]

  //#printer-messages
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
