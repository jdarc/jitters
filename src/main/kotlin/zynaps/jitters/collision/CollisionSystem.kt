package zynaps.jitters.collision

import zynaps.jitters.physics.RigidBody

class CollisionSystem {
    val bodies = mutableSetOf<RigidBody>()

    fun detect(listener: CollisionListener) {
        for (body0 in bodies.filter { it.isActive }) {
            for (body1 in bodies.filter { !it.isActive || body0.id < it.id }) {
                if (body0.hitTest(body1)) handleCollision(body0, body1, listener)
            }
        }
    }

    private fun handleCollision(body0: RigidBody, body1: RigidBody, listener: CollisionListener) {
        val collide = GjkEpaSolver.collide(body0.skin, body1.skin)
        if (collide.collided) {
            val points0 = generateCollisionPoint(collide, body0, body1)

            val collide1 = MprSolver.collide(body0.skin, body1.skin)
            val points1 = generateCollisionPoint(collide1, body0, body1)

            points1.initialPenetration = points0.initialPenetration
            listener.collisionNotify(body0, body1, collide.normal, arrayOf(points0, points1))
        }
    }

    companion object {
        private fun generateCollisionPoint(collide: CollisionDetails, body0: RigidBody, body1: RigidBody): CollisionPoints {
            val r0 = collide.pointA - body0.skin.origin
            val r1 = collide.pointB - body1.skin.origin
            return CollisionPoints(r0, r1, collide.depth)
        }
    }
}
