package com.agar.core.utils

class Vector2d(val x: Double, val y: Double) {

  def this() = this(0.0, 0.0)

  def this(p: Point2d) = this(p.x, p.y)

  /** Negates a vector. */
  def unary_- = new Vector2d(-x, -y)

  /** Returns the sum of two vectors. */
  def add(v: Vector2d): Vector2d = new Vector2d(x + v.x, y + v.y)

  /** Returns the sum of two vectors. */
  def +(v: Vector2d): Vector2d = this.add(v)

  /** Returns the difference of two vectors. */
  def subtract(v: Vector2d): Vector2d = this + (-v)

  /** Returns the difference of two vectors. */
  def -(v: Vector2d): Vector2d = this.subtract(v)

  /** Returns the scalar multiple of this vector by k. */
  def multiply(k: Double): Vector2d = new Vector2d(k * x, k * y)

  /** Returns the scalar multiple of this vector by 1/k. */
  def divide(k: Double): Vector2d = this.multiply(1.0 / k)

  /** Returns the scalar multiple of this vector by k. */
  def *(k: Double): Vector2d = this.multiply(k)

  /** Returns the scalar multiple of this vector by 1/k. */
  def /(k: Double): Vector2d = this.multiply(1.0 / k)

  /** Returns the dot product of two vectors. */
  def dot(v: Vector2d): Double = x * v.x + y * v.y

  /** Returns a unit vector in the direction of this vector.
    * x = ax / |a|
    * y = ay / |a|
    * z = az / |a|
    * */
  def normalize(): Vector2d = this / this.magnitude()

  /** Returns the projection of this vector onto v. */
  def proj(v: Vector2d): Vector2d = v * (this.dot(v) / v.dot(v))

  /** Returns the length of the vector |a| = sqrt((ax * ax) + (ay * ay) + (az * az)) */
  def magnitude(): Double = Math.sqrt((x * x) + (y * y))

  def canEqual(other: Any): Boolean =
    other.isInstanceOf[Vector2d]

  override def equals(other: Any): Boolean = {
    other match {
      case that: Vector2d =>
        that.canEqual(Vector2d.this) &&
          x == that.x &&
          y == that.y
      case _ => false
    }
  }

  override def toString(): String = {
    "Vector2d< x:" + x + ", y:" + y + ">"
  }
}