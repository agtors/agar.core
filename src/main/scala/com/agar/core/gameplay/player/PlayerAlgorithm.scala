package com.agar.core.gameplay.player

trait PlayerAlgorithm {

  def move(p: (Int,Int)): (Int, Int)

}