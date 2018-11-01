package com.agar.core.context

import scala.concurrent.duration._

object DefaultAgarContext extends AgarContext {

  implicit override val system: AgarSystem = () => 5 seconds
  implicit override val position: AgarPosition = () => (0, 0)
  implicit override val algorithm: AgarAlgorithm = (p: (Int, Int)) => (p._1 + 1, p._2 + 1)

}
