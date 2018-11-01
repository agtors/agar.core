package com.agar.core.context

import scala.concurrent.duration.FiniteDuration

trait AgarSystem {

  def timeout: FiniteDuration

}
