package com.agar.core.behaviors

import com.agar.core.utils.Vector2d

object Behavior {
  type Steering = Vector2d

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
  // TODO: please change the signature of target... really please
  def pursuit(target: (Vector2d, Vector2d) , position: Vector2d, velocity: Vector2d, maxVelocity: Short): Steering = {
    val futurePosition = target._1 + target._2 * maxVelocity;
    seek(futurePosition, position, velocity, maxVelocity)
  }

  // The evade behavior is the opposite of the pursuit behavior.
  // Instead of seeking the target's future position, in the evade behavior the character will flee that position:
  // TODO: please change the signature of target... really please
  def evade(target: (Vector2d, Vector2d) , position: Vector2d, velocity: Vector2d, maxVelocity: Short): Steering = {
    val distance = target._1 - position
    var updatesAhead = distance.magnitude() / maxVelocity;
    val futurePosition = target._1 + target._2 * maxVelocity;
    flee(futurePosition, position, velocity, maxVelocity)
  }
}
