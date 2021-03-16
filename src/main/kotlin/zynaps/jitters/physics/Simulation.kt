package zynaps.jitters.physics

import zynaps.jitters.JConfig
import zynaps.jitters.collision.CollisionListener
import zynaps.jitters.collision.CollisionPoints
import zynaps.jitters.collision.CollisionResult
import zynaps.jitters.collision.CollisionSystem
import zynaps.math.Vector3

class Simulation : CollisionListener {
    private val collisionSystem = CollisionSystem()
    private val collisions = mutableListOf<CollisionResult>()
    private val bodies = mutableSetOf<RigidBody>()

    var gravity = Vector3(0F, -9.8F, 0F)

    fun addBody(body: RigidBody) {
        bodies.add(body)
        collisionSystem.bodies.add(body)
    }

    fun removeBody(body: RigidBody) {
        bodies.remove(body)
        collisionSystem.bodies.remove(body)
    }

    fun removeAllBodies() {
        bodies.clear()
        collisionSystem.bodies.clear()
    }

    override fun collisionNotify(body0: RigidBody, body1: RigidBody, dirToBody0: Vector3, collPts: Array<CollisionPoints>) {
        collisions.add(CollisionResult(body0, body1, dirToBody0, collPts))
    }

    fun integrate(dt: Float) {
        val activeBodies = bodies.filter { it.isActive }

        for (body in activeBodies) {
            body.copyState()
            if (body.applyGravity) body.addWorldForce(gravity * body.mass)
            body.storeState()
            body.updateVelocity(dt)
            body.updatePosition(dt)
        }

        collisions.clear()
        collisionSystem.detect(this)

        for (body in activeBodies) body.restoreState()

        Solver.processCollisions(collisions, dt, JConfig.COLLISION_ITERATIONS, false)

        for (body in activeBodies) body.updateVelocity(dt)

        Solver.processCollisions(collisions, dt, JConfig.CONTACT_ITERATIONS, true)

        for (body in activeBodies) {
            body.updatePosition(dt)
            body.clearForces()
        }
    }
}
