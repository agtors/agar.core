package com.agar.core.gameplay.arbritrator

sealed trait PlayerStatus

case object WaitingPlayer extends PlayerStatus

case object RunningPlayer extends PlayerStatus

