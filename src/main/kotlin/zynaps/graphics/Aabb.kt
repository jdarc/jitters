package zynaps.graphics

import zynaps.math.Matrix4
import zynaps.math.Vector3

class Aabb {
    private var minX = Float.POSITIVE_INFINITY
    private var minY = Float.POSITIVE_INFINITY
    private var minZ = Float.POSITIVE_INFINITY
    private var maxX = Float.NEGATIVE_INFINITY
    private var maxY = Float.NEGATIVE_INFINITY
    private var maxZ = Float.NEGATIVE_INFINITY

    val min get() = Vector3(minX, minY, minZ)
    val max get() = Vector3(maxX, maxY, maxZ)

    fun reset() {
        minX = Float.POSITIVE_INFINITY
        minY = Float.POSITIVE_INFINITY
        minZ = Float.POSITIVE_INFINITY
        maxX = Float.NEGATIVE_INFINITY
        maxY = Float.NEGATIVE_INFINITY
        maxZ = Float.NEGATIVE_INFINITY
    }

    fun aggregate(x: Float, y: Float, z: Float): Aabb {
        if (x.isFinite()) {
            if (x < minX) minX = x
            if (x > maxX) maxX = x
        }

        if (y.isFinite()) {
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }

        if (z.isFinite()) {
            if (z < minZ) minZ = z
            if (z > maxZ) maxZ = z
        }
        return this
    }

    fun aggregate(other: Aabb) = aggregate(other.minX, other.minY, other.minZ).aggregate(other.maxX, other.maxY, other.maxZ)

    fun aggregate(other: Aabb, matrix: Matrix4): Aabb {
        val a = matrix.m00 * other.minX
        val b = matrix.m10 * other.maxY
        val c = matrix.m20 * other.minZ
        val d = matrix.m01 * other.minX
        val e = matrix.m11 * other.maxY
        val f = matrix.m21 * other.minZ
        val g = matrix.m02 * other.minX
        val h = matrix.m12 * other.maxY
        val i = matrix.m22 * other.minZ
        val j = matrix.m00 * other.maxX
        val k = matrix.m01 * other.maxX
        val l = matrix.m02 * other.maxX
        val m = matrix.m10 * other.minY
        val n = matrix.m11 * other.minY
        val o = matrix.m12 * other.minY
        val p = matrix.m20 * other.maxZ
        val q = matrix.m21 * other.maxZ
        val r = matrix.m22 * other.maxZ
        aggregate(a + b + c + matrix.m30, d + e + f + matrix.m31, g + h + i + matrix.m32)
        aggregate(j + b + c + matrix.m30, k + e + f + matrix.m31, l + h + i + matrix.m32)
        aggregate(j + m + c + matrix.m30, k + n + f + matrix.m31, l + o + i + matrix.m32)
        aggregate(a + m + c + matrix.m30, d + n + f + matrix.m31, g + o + i + matrix.m32)
        aggregate(a + b + p + matrix.m30, d + e + q + matrix.m31, g + h + r + matrix.m32)
        aggregate(j + b + p + matrix.m30, k + e + q + matrix.m31, l + h + r + matrix.m32)
        aggregate(j + m + p + matrix.m30, k + n + q + matrix.m31, l + o + r + matrix.m32)
        aggregate(a + m + p + matrix.m30, d + n + q + matrix.m31, g + o + r + matrix.m32)
        return this
    }
}

