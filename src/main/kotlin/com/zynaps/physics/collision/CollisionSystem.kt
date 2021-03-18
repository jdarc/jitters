package com.zynaps.physics.collision

import com.zynaps.physics.Settings
import com.zynaps.physics.dynamics.RigidBody

class CollisionSystem {
    val bodies = mutableSetOf<RigidBody>()

    fun detect(listener: CollisionListener) {
        for (body0 in bodies.filter { it.isActive }) {
            for (body1 in bodies.filter { !it.isActive || body0.id < it.id }) {
                if (body0.hitTest(body1)) {
                    val result = GjkEpaSolver.collide(body0.skin, body1.skin, Settings.COLLISION_TOLERANCE)
                    if (result.hasCollided) {
                        val r0 = result.pointA - body0.skin.origin
                        val r1 = result.pointB - body1.skin.origin
                        listener.collisionNotify(body0, body1, result.normal, arrayOf(CollisionPoints(r0, r1, result.depth)))
                    }
                }
            }
        }
    }
}
