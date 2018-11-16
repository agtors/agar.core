package com.agar.core.utils

object RegionBoundaries {
  def apply(square: List[Double]): RegionBoundaries = apply(square(0), square(1), square(2), square(3))

  def apply(minX: Double, minY: Double, maxX: Double, maxY: Double): RegionBoundaries = new RegionBoundaries(minX, minY, maxX, maxY)
}

class RegionBoundaries(minX: Double, minY: Double, maxX: Double, maxY: Double) {

  def width: Double = maxX - minX

  def height: Double = maxY - minY

  def intersect(position: Vector2d, radius: Double): Boolean = {
    val closest = clamp(position)

    val distanceX: Double = position.x - closest.x
    val distanceY: Double = position.y - closest.y

    val distanceSquared: Double = distanceX * distanceX + distanceY * distanceY
    val radiusSquared: Double = radius * radius

    distanceSquared < radiusSquared

  }

  def clamp(position: Vector2d): Vector2d = {
    val closestX = clamp(position.x, minX, maxX)
    val closestY = clamp(position.y, minY, maxY)
    Vector2d(closestX, closestY)
  }

  private def clamp(v: Double, min: Double, max: Double): Double = {
    if (v < min) min
    else if (v > max) max
    else v
  }

}
