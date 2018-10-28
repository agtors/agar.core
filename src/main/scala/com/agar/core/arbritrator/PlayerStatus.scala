package com.agar.core.arbritrator

sealed trait PlayerStatus

case class WaitingPlayer(position: (Int, Int)) extends PlayerStatus

case object RunningPlayer extends PlayerStatus

