package com.agar.core.context

import java.util.concurrent.TimeUnit

import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration

object DefaultAgarContext extends AgarContext {

  object DefaultAgarSystem extends AgarSystem {
    override def timeout: FiniteDuration = duration.FiniteDuration(5, TimeUnit.SECONDS)
  }

  object DefaultAgarAlgorithm extends AgarAlgorithm {
    override def move(p: (Int, Int)): (Int, Int) = (p._1 + 1, p._2 + 1)
  }

  implicit val system: AgarSystem = DefaultAgarSystem
  implicit val algorithm: AgarAlgorithm = DefaultAgarAlgorithm

}
