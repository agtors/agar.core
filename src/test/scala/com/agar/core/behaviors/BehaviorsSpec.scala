package com.agar.core.behaviors

import com.agar.core.behaviors.Behavior.TargetEntity
import com.agar.core.utils.Vector2d
import org.scalatest.{FlatSpec, Matchers, WordSpec}

import scala.language.postfixOps


class BehaviorsSpec() extends WordSpec with Matchers {

  val MAX_VELOCITY = 3:Short

  "An entity" should {
    "move toward the target with steer behavior" in {
      val target = new Vector2d(40, 70)
      val pos = new Vector2d(80, 60)
      val vel = new Vector2d(2, 2)
      Behavior.seek(target, pos, vel, MAX_VELOCITY) should be(new Vector2d(-4.910427500435995, -1.2723931248910012))
    }

    "run away from the target with flee behavior" in {
      val target = new Vector2d(90, 60)
      val pos = new Vector2d(80, 60)
      val vel = new Vector2d(2, 2)
      Behavior.flee(target, pos, vel, MAX_VELOCITY) should be(new Vector2d(-5, -2))
    }

    "be able to pursuit a target" in {
      val target = new TargetEntity(new Vector2d(40, 70), new Vector2d(2, 2))
      val pos = new Vector2d(80, 60)
      val vel = new Vector2d(2, 2)
      Behavior.pursuit(target, pos, vel, MAX_VELOCITY) should be(new Vector2d(-4.567868516922847, -3.5511451511049703))
    }

    "be evade from a pursuer" in {
      val target = new TargetEntity(new Vector2d(40, 70), new Vector2d(2, 2))
      val pos = new Vector2d(80, 60)
      val vel = new Vector2d(2, 2)
      Behavior.evade(target, pos, vel, MAX_VELOCITY) should be(new Vector2d(0.5678685169228461, -0.44885484889502947))
    }
  }
}