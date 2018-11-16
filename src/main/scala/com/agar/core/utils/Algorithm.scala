package com.agar.core.utils

object Algorithm {

  private val (minX, minY, maxX, maxY) = (0, 1, 2, 3)

  def width(square: List[Double]): Double = {
    square(maxX) - square(minX)
  }

  def height(square: List[Double]): Double = {
    square(maxY) - square(minY)
  }

  def clamp(position: Vector2d, square: List[Double]): Vector2d = {
    val (closestX: Double, closestY: Double) = clampPosition(position, square)
    Vector2d(closestX, closestY)
  }

  def isInSquare(position: Vector2d, weight: Double, square: List[Double]): Boolean = {
    val (closestX: Double, closestY: Double) = clampPosition(position, square)

    val distanceX: Double = position.x - closestX
    val distanceY: Double = position.y - closestY

    val distanceSquared: Double = distanceX * distanceX + distanceY * distanceY
    val radiusSquared: Double = (weight / 2) * (weight / 2)

    distanceSquared < radiusSquared
  }

  private def clampPosition(position: Vector2d, square: List[Double]) = {
    val closestX = clamp(position.x, square(minX), square(maxX))
    val closestY = clamp(position.y, square(minY), square(maxY))
    (closestX, closestY)
  }

  private def clamp(v: Double, min: Double, max: Double): Double = {
    if (v < min) min
    else if (v > max) max
    else v
  }
}
