package zynaps.jitters.collision

import zynaps.jitters.physics.RigidBody
import zynaps.math.Vector3

interface CollisionListener {
    fun collisionNotify(body0: RigidBody, body1: RigidBody, dirToBody0: Vector3, collPts: Array<CollisionPoints>)
}
