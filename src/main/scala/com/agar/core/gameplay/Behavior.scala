package com.agar.core.gameplay

import com.agar.core.utils.Vector2d

object Behavior {
  type Steering = Vector2d

  case class TargetEntity(position: Vector2d, velocity: Vector2d)

  // Get the steering force that make the character run toward the target
  def seek(target: Vector2d, position: Vector2d, velocity: Vector2d, maxVelocity: Short): Steering = {
    val desiredVelocity = (target - position).normalize() * maxVelocity
    desiredVelocity - velocity
  }

  // Get the steering force that make the character run away from the target
  // flee desired velocity = -seek desired velocity
  def flee(target: Vector2d, position: Vector2d, velocity: Vector2d, maxVelocity: Short): Steering = {
    // The desired_velocity in that case represents the easiest escaping route the character can use to run away from the target.
    val desiredVelocity = (position - target).normalize() * maxVelocity
    desiredVelocity - velocity
  }

  // The pursuit behavior works pretty much the same way seek does,
  // the only difference is that the pursuer will not seek the target itself, but its position in the near future.
  def pursuit(target: TargetEntity, position: Vector2d, velocity: Vector2d, maxVelocity: Short): Steering = {
    val targetFuturePosition = getFuturPositionAccordingTo(target, position, maxVelocity)
    seek(targetFuturePosition, position, velocity, maxVelocity)
  }

  // The evade behavior is the opposite of the pursuit behavior.
  // Instead of seeking the target's future position, in the evade behavior the character will flee that position:
  def evade(target: TargetEntity, position: Vector2d, velocity: Vector2d, maxVelocity: Short): Steering = {
    val targetFuturePosition = getFuturPositionAccordingTo(target, position, maxVelocity)
    flee(targetFuturePosition, position, velocity, maxVelocity)
  }

  // Produce small random displacements and apply to the character's current direction vector
  def wander(currentVelocity: Vector2d, wanderDistance: Int, wanderRadius: Int): Steering = {
    var circleCenter = currentVelocity.copy()
    circleCenter = circleCenter.normalize()
    circleCenter = circleCenter * wanderDistance

    var displacement = Vector2d(0, -1)
    displacement = displacement * wanderRadius

    // wander force
    circleCenter + displacement
  }

  // Get the futur position of an entity according to the position of another entity
  private def getFuturPositionAccordingTo(target: TargetEntity, position: Vector2d, maxVelocity: Short): Vector2d = {
    val distance = target.position - position
    val updatesNeeded = distance.magnitude() / maxVelocity
    val tv = target.velocity * updatesNeeded
    target.velocity + tv
  }
}
