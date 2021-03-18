package com.zynaps.physics.dynamics

import com.zynaps.math.Scalar.max
import com.zynaps.math.Scalar.min
import com.zynaps.math.Vector3
import com.zynaps.physics.Settings
import com.zynaps.physics.collision.CollisionListener
import com.zynaps.physics.collision.CollisionPoints
import com.zynaps.physics.collision.CollisionSystem

@Suppress("unused", "MemberVisibilityCanBePrivate")
class Simulation : CollisionListener {
    private val collisionSystem = CollisionSystem()
    private val collisions = mutableListOf<Collision>()
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

    override fun collisionNotify(body0: RigidBody, body1: RigidBody, normal: Vector3, points: Array<CollisionPoints>) {
        collisions.add(Collision(body0, body1, normal, points))
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

        processCollisions(collisions, dt, Settings.COLLISION_ITERATIONS, false)

        for (body in activeBodies) body.updateVelocity(dt)

        processCollisions(collisions, dt, Settings.CONTACT_ITERATIONS, true)

        for (body in activeBodies) {
            body.updatePosition(dt)
            body.clearForces()
        }
    }

    private fun processCollisions(collisions: List<Collision>, dt: Float, iterations: Int, forceInelastic: Boolean) {
        for (collision in collisions) {
            preProcessCollision(collision, dt)
            if (!forceInelastic) continue
            collision.restitution = 0F
            collision.satisfied = false
        }

        for (step in 0 until iterations) {
            for (collision in collisions) {
                if (!collision.satisfied) processCollision(collision)
                preProcessCollision(collision, dt)
            }
        }
    }

    private fun preProcessCollision(collision: Collision, dt: Float) {
        collision.satisfied = false

        val tolerance = 1F / (Settings.TINY + Settings.ALLOWED_PENETRATION)
        for (points in collision.points) {
            points.denominator = 0F
            if (collision.body0.isMovable) {
                val v = Vector3.cross(points.r0, collision.normal) * collision.body0.worldInvInertia
                points.denominator += collision.body0.inverseMass + Vector3.dot(collision.normal, Vector3.cross(v, points.r0))
            }

            if (collision.body1.isMovable) {
                val v = Vector3.cross(points.r1, collision.normal) * collision.body1.worldInvInertia
                points.denominator += collision.body1.inverseMass + Vector3.dot(collision.normal, Vector3.cross(v, points.r1))
            }

            val diffPenetration = points.initialPenetration - Settings.ALLOWED_PENETRATION
            if (points.initialPenetration > Settings.ALLOWED_PENETRATION) {
                points.minSeparationVel = diffPenetration / dt
            } else {
                val approachScale = -0.1F * diffPenetration * tolerance
                points.minSeparationVel = approachScale.coerceIn(Settings.TINY, 1F) * diffPenetration / max(dt, Settings.TINY)
            }

            points.denominator = max(points.denominator, Settings.TINY)
            points.minSeparationVel = min(points.minSeparationVel, MAX_VEL_MAG)
        }
    }

    private fun processCollision(collision: Collision) {
        for (points in collision.points) {
            val relativeTo = collision.body0.velocityRelativeTo(points.r0) - collision.body1.velocityRelativeTo(points.r1)
            val normalVel = Vector3.dot(relativeTo, collision.normal)
            if (normalVel > points.minSeparationVel) continue

            var finalNormalVel = -collision.restitution * normalVel
            if (finalNormalVel < MIN_VEL_FOR_PROCESSING) finalNormalVel = points.minSeparationVel

            val deltaVel = finalNormalVel - normalVel
            if (deltaVel <= MIN_VEL_FOR_PROCESSING) continue

            if (points.denominator < Settings.TINY) points.denominator = Settings.TINY
            val normalImpulse = deltaVel / points.denominator

            val impulse = collision.normal * normalImpulse
            collision.body0.applyBodyWorldImpulse(impulse, points.r0)
            collision.body1.applyBodyWorldImpulse(-impulse, points.r1)

            var vrNew = collision.body0.velocityRelativeTo(points.r0)
            vrNew -= collision.body1.velocityRelativeTo(points.r1)

            var tangentVel = collision.normal * Vector3.dot(vrNew, collision.normal) - vrNew
            val tangentSpeed = tangentVel.length
            if (tangentSpeed < MIN_VEL_FOR_PROCESSING) continue
            tangentVel /= tangentSpeed

            var denominator = 0F
            if (collision.body0.isMovable) {
                val v = Vector3.cross(points.r0, tangentVel) * collision.body0.worldInvInertia
                denominator += collision.body0.inverseMass + Vector3.dot(tangentVel, Vector3.cross(v, points.r0))
            }

            if (collision.body1.isMovable) {
                val v = Vector3.cross(points.r1, tangentVel) * collision.body1.worldInvInertia
                denominator += collision.body1.inverseMass + Vector3.dot(tangentVel, Vector3.cross(v, points.r1))
            }

            if (denominator < Settings.TINY) continue

            val impulseToReserve = tangentSpeed / denominator
            val i = if (impulseToReserve < collision.friction * normalImpulse) impulseToReserve else collision.friction * normalImpulse
            collision.body0.applyBodyWorldImpulse(tangentVel * +i, points.r0)
            collision.body1.applyBodyWorldImpulse(tangentVel * -i, points.r1)
        }
        collision.satisfied = true
    }

    private companion object {
        const val MAX_VEL_MAG = 0.5F
        const val MIN_VEL_FOR_PROCESSING = 0.0001F

        class Collision(val body0: RigidBody, val body1: RigidBody, val normal: Vector3, val points: Array<CollisionPoints>) {
            val friction = (body0.skin.friction + body1.skin.friction) * 0.5F
            var restitution = (body0.skin.restitution + body1.skin.restitution) * 0.5F
            var satisfied = false
        }
    }
}
