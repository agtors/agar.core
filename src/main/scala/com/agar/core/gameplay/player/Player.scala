package com.agar.core.gameplay.player

import akka.actor.{Actor, Props}

object Player {
  def props: Props = Props(new Player())
}

// TODO - Weight to be added
class Player() extends Actor {

  def receive: PartialFunction[Any, Unit] = {
    ??? // TODO
  }

}
