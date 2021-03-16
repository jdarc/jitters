package zynaps.jitters.collision

import zynaps.math.Vector3
import zynaps.math.Vector3.Companion.isZero

object MprSolver : CollisionDetector {
    private const val OUTER_ITERATION_LIMIT = 15

    override fun collide(shape0: CollisionShape, shape1: CollisionShape, collisionMargin: Float): CollisionDetails {
        val result = Result()
        var v0 = shape1.origin - shape0.origin
        if (isZero(v0)) v0 = Vector3(0.00001F, 0F, 0F)

        var n = -v0
        var v11 = shape0.getSupport(v0)
        var v12 = shape1.getSupport(n)
        var v1 = v12 - v11

        if (Vector3.dot(v1, n) <= 0) {
            result.normal = n
            return result
        }

        n = Vector3.cross(v1, v0)
        if (isZero(n)) {
            n = Vector3.normalize(v1 - v0)
            result.normal = n
            result.pointA = v11
            result.pointB = v12
            result.collided = true
            return result
        }

        var tmp = -n
        var v21 = shape0.getSupport(tmp)
        var v22 = shape1.getSupport(n)
        var v2 = v22 - v21
        if (Vector3.dot(v2, n) <= 0) {
            result.normal = n
            return result
        }

        n = Vector3.cross((v1 - v0), v2 - v0)
        val dist = Vector3.dot(n, v0)

        if (dist > 0) {
            var temp = v1; v1 = v2; v2 = temp
            temp = v11; v11 = v21; v21 = temp
            temp = v12; v12 = v22; v22 = temp
            n = -n
        }

        // Identify a portal
        var phase1 = 0
        while (true) {
            if (phase1++ > OUTER_ITERATION_LIMIT) return result

            // Obtain the support point in a direction perpendicular to the existing plane
            // This point is guaranteed to lie off the plane
            tmp = -n
            var v31 = shape0.getSupport(tmp)
            var v32 = shape1.getSupport(n)
            var v3 = v32 - v31
            if (Vector3.dot(v3, n) <= 0) {
                result.normal = n
                return result
            }

            // If origin is outside (v1, v0, v3), then eliminate v2 and loop
            if (Vector3.dot(Vector3.cross(v1, v3), v0) < 0) {
                v2 = v3
                v21 = v31
                v22 = v32
                n = Vector3.cross(v1 - v0, v3 - v0)
                continue
            }

            // If origin is outside (v3,v0,v2), then eliminate v1 and loop
            if (Vector3.dot(Vector3.cross(v3, v2), v0) < 0) {
                v1 = v3
                v11 = v31
                v12 = v32
                n = Vector3.cross(v3 - v0, v2 - v0)
                continue
            }
            var hit = false

            // Phase Two: Refine the portal
            var phase2 = 0

            // We are now inside of a wedge...
            while (true) {
                phase2++

                // Compute normal of the wedge face
                n = Vector3.cross(v2 - v1, v3 - v1)

                if (isZero(n)) return result

                n = Vector3.normalize(n)

                // Compute distance from origin to wedge face
                val d = Vector3.dot(n, v1)

                // If the origin is inside the wedge, we have a hit
                if (d >= 0 && !hit) {
                    result.normal = n

                    // Compute the barycentric coordinates of the origin
                    var b0 = Vector3.dot(Vector3.cross(v1, v2), v3)
                    var b1 = Vector3.dot(Vector3.cross(v3, v2), v0)
                    var b2 = Vector3.dot(Vector3.cross(v0, v1), v3)
                    var b3 = Vector3.dot(Vector3.cross(v2, v1), v0)
                    var sum = b0 + b1 + b2 + b3
                    if (sum <= 0) {
                        b0 = 0F
                        b1 = Vector3.dot(Vector3.cross(v2, v3), n)
                        b2 = Vector3.dot(Vector3.cross(v3, v1), n)
                        b3 = Vector3.dot(Vector3.cross(v1, v2), n)
                        sum = b1 + b2 + b3
                    }
                    val inv = 1F / sum
                    result.pointA = (shape0.origin * b0 + v11 * b1 + v21 * b2 + v31 * b3) * inv
                    result.pointB = (shape1.origin * b0 + v12 * b1 + v22 * b2 + v32 * b3) * inv
                    hit = true
                }

                // Find the support point in the direction of the wedge face
                tmp = -n
                val v41 = shape0.getSupport(tmp)
                val v42 = shape1.getSupport(n)
                val v4 = v42 - v41
                val delta = Vector3.dot(v4 - v3, n)
                val separation = -Vector3.dot(v4, n)

                // If the boundary is thin enough or the origin is outside the support plane for the newly discovered vertex, then we can terminate
                if (delta <= collisionMargin || separation >= 0) {
                    result.normal = n
                    result.collided = hit
                    return result
                }

                // Compute the tetrahedron dividing face (v4,v0,v1)
                when {
                    Vector3.dot(Vector3.cross(v4, v1), v0) < 0 -> {
                        when {
                            Vector3.dot(Vector3.cross(v4, v2), v0) < 0 -> {
                                v1 = v4
                                v11 = v41
                                v12 = v42
                            }
                            else -> {
                                v3 = v4
                                v31 = v41
                                v32 = v42
                            }
                        }
                    }
                    else -> {
                        when {
                            Vector3.dot(Vector3.cross(v4, v3), v0) < 0 -> {
                                v2 = v4
                                v21 = v41
                                v22 = v42
                            }
                            else -> {
                                v1 = v4
                                v11 = v41
                                v12 = v42
                            }
                        }
                    }
                }
            }
        }
    }

    class Result : CollisionDetails {
        override var pointA = Vector3.ZERO
        override var pointB = Vector3.ZERO
        override var normal = Vector3.ZERO
        override var collided = false
        override val depth = 0F
    }
}
