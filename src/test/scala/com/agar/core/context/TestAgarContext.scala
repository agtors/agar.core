package com.agar.core.context

import java.util.concurrent.TimeUnit

import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration

object TestAgarContext extends AgarContext {

  object TestAgarSystem extends AgarSystem {
    override def timeout: FiniteDuration = duration.FiniteDuration(1, TimeUnit.SECONDS)
  }

  object TestAgarAlgorithm extends AgarAlgorithm {
    override def move(p: (Int, Int)): (Int, Int) = (p._1 + 1, p._2 + 1)
  }

  override val system: AgarSystem = TestAgarSystem
  override val algorithm: AgarAlgorithm = TestAgarAlgorithm

}
