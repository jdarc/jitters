package zynaps.jitters.geometry

import zynaps.math.Matrix4
import zynaps.math.Vector3
import zynaps.math.Vector3.Companion.dot

class Hull(private val points: Array<Vector3>, private val scale: Float = 1F) : Shape() {
    override var origin = Vector3.ZERO
    override var basis = Matrix4.IDENTITY

    override fun getSupport(direction: Vector3) = origin + localGetSupporting(Vector3.normalize(basis * direction)) * basis

    override fun calculateBodyInertia(mass: Float): Matrix4 {
        val min = points.fold(Vector3.ZERO, { acc, cur -> Vector3.min(acc, cur) })
        val max = points.fold(Vector3.ZERO, { acc, cur -> Vector3.max(acc, cur) })
        return Matrix4.createScale(max - min)
    }

    private fun localGetSupporting(v: Vector3): Vector3 {
        var out = Vector3.ZERO
        var maxDot = -EPSILON
        for (p in points) {
            val dot = dot(v, p)
            if (dot > maxDot) {
                maxDot = dot
                out = p
            }
        }
        return out * scale + v * CONVEX_DISTANCE_MARGIN
    }

    private companion object {
        const val EPSILON = 1e15F
        const val CONVEX_DISTANCE_MARGIN = 0.05F
    }
}
