package com.agar.core.context

import scala.concurrent.duration._

object DefaultAgarSystem extends AgarSystem {

  override def timeout(): FiniteDuration = 1 second

}
