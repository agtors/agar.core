package com.agar.core.context

import scala.concurrent.duration._
import scala.util.Random

object DefaultAgarContext extends AgarContext {

  val MAX = 1000

  implicit override val system: AgarSystem = () => 100 millis

  implicit override val position: AgarPosition = () =>
    (Random.nextInt(1000 - 1), Random.nextInt(1000 - 1))

  implicit override val algorithm: AgarAlgorithm = (p: (Int, Int)) => {
    def move(x: Int): Int = {
      val dx = Random.nextInt(2) - 1
      Math.min(Math.max(0, x + dx), MAX)
    }

    (move(p._1), move(p._2))
  }

}
