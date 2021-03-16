package zynaps.jitters.physics

import zynaps.jitters.JConfig
import zynaps.jitters.collision.CollisionResult
import zynaps.math.Scalar.max
import zynaps.math.Vector3

object Solver {
    private const val MAX_VEL_MAG = 0.5F
    private const val MIN_VEL_FOR_PROCESSING = 0.0001F

    fun processCollisions(collisions: List<CollisionResult>, dt: Float, iterations: Int, forceInelastic: Boolean) {
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

    private fun preProcessCollision(collision: CollisionResult, dt: Float) {
        collision.satisfied = false

        for (points in collision.points) {
            points.denominator = 0F
            if (collision.body0.isMovable) {
                val v = Vector3.cross(points.r0, collision.dirToBody0) * collision.body0.worldInvInertia
                points.denominator += collision.body0.inverseMass + Vector3.dot(collision.dirToBody0, Vector3.cross(v, points.r0))
            }

            if (collision.body1.isMovable == true) {
                val v = Vector3.cross(points.r1, collision.dirToBody0) * collision.body1.worldInvInertia
                points.denominator += collision.body1.inverseMass + Vector3.dot(collision.dirToBody0, Vector3.cross(v, points.r1))
            }

            val diffPenetration = points.initialPenetration - JConfig.ALLOWED_PENETRATION
            if (points.initialPenetration > JConfig.ALLOWED_PENETRATION) {
                points.minSeparationVel = diffPenetration / dt
            } else {
                val approachScale = -0.1F * diffPenetration / (JConfig.TINY + JConfig.ALLOWED_PENETRATION)
                points.minSeparationVel = approachScale.coerceIn(JConfig.TINY, 1F) * diffPenetration / max(dt, JConfig.TINY)
            }

            points.denominator = max(points.denominator, JConfig.TINY)
            points.minSeparationVel = kotlin.math.min(points.minSeparationVel, MAX_VEL_MAG)
        }
    }

    private fun processCollision(collision: CollisionResult) {
        for (points in collision.points) {
            val relativeTo = collision.body0.velocityRelativeTo(points.r0) - collision.body1.velocityRelativeTo(points.r1)
            val normalVel = Vector3.dot(relativeTo, collision.dirToBody0)
            if (normalVel > points.minSeparationVel) continue

            var finalNormalVel = -collision.restitution * normalVel
            if (finalNormalVel < MIN_VEL_FOR_PROCESSING) finalNormalVel = points.minSeparationVel

            val deltaVel = finalNormalVel - normalVel
            if (deltaVel <= MIN_VEL_FOR_PROCESSING) continue

            if (points.denominator < JConfig.TINY) points.denominator = JConfig.TINY
            val normalImpulse = deltaVel / points.denominator

            val impulse = collision.dirToBody0 * normalImpulse
            collision.body0.applyBodyWorldImpulse(impulse, points.r0)
            collision.body1.applyBodyWorldImpulse(-impulse, points.r1)

            var vrNew = collision.body0.velocityRelativeTo(points.r0)
            vrNew -= collision.body1.velocityRelativeTo(points.r1)

            var tangentVel = collision.dirToBody0 * Vector3.dot(vrNew, collision.dirToBody0) - vrNew
            val tangentSpeed = tangentVel.length
            if (tangentSpeed < MIN_VEL_FOR_PROCESSING) continue
            tangentVel /= tangentSpeed

            var denominator = 0F
            if (collision.body0.isMovable) {
                val v = Vector3.cross(points.r0, tangentVel) * collision.body0.worldInvInertia
                denominator += collision.body0.inverseMass + Vector3.dot(tangentVel, Vector3.cross(v, points.r0))
            }

            if (collision.body1.isMovable == true) {
                val v = Vector3.cross(points.r1, tangentVel) * collision.body1.worldInvInertia
                denominator += collision.body1.inverseMass + Vector3.dot(tangentVel, Vector3.cross(v, points.r1))
            }

            if (denominator < JConfig.TINY) continue

            val impulseToReserve = tangentSpeed / denominator
            val t =
                tangentVel * if (impulseToReserve < collision.friction * normalImpulse) impulseToReserve else collision.friction * normalImpulse
            collision.body0.applyBodyWorldImpulse(t, points.r0)
            collision.body1.applyBodyWorldImpulse(-t, points.r1)
        }
        collision.satisfied = true
    }
}

