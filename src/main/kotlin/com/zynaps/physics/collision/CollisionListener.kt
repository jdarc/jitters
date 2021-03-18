package com.zynaps.physics.collision

import com.zynaps.math.Vector3
import com.zynaps.physics.dynamics.RigidBody

interface CollisionListener {
    fun collisionNotify(body0: RigidBody, body1: RigidBody, normal: Vector3, points: Array<CollisionPoints>)
}
