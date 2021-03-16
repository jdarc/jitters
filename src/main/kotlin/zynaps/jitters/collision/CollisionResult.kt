package zynaps.jitters.collision

import zynaps.jitters.physics.RigidBody
import zynaps.math.Vector3

class CollisionResult(val body0: RigidBody, val body1: RigidBody, val dirToBody0: Vector3, val points: Array<CollisionPoints>) {
    val friction = (body0.skin.material.friction + body1.skin.material.friction) / 2F
    var restitution = (body0.skin.material.restitution + body1.skin.material.restitution) / 2F
    var satisfied = false
}
