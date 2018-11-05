package com.agar.core.context

import com.agar.core.utils.Point2d

import scala.concurrent.duration._
import scala.util.Random

object DefaultAgarContext extends AgarContext {

  val MAX = 1000

  implicit override val system: AgarSystem = () => 100 millis

  implicit override val position: AgarPosition = () =>
    Point2d(Random.nextInt(1000 - 1), Random.nextInt(1000 - 1))

  implicit override val algorithm: AgarAlgorithm = (p: Point2d) => {
    def move(x: Double): Double = {
      val dx = Random.nextInt(2) - 1
      Math.min(Math.max(0, x + dx), MAX)
    }

    Point2d(move(p.x), move(p.y))
  }

}
