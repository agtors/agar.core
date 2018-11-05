package com.agar.core.arbritrator

import com.agar.core.utils.Point2d

sealed trait PlayerStatus

case class WaitingPlayer(position: Point2d) extends PlayerStatus

case object RunningPlayer extends PlayerStatus

