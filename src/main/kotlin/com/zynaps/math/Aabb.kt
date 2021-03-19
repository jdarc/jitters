package com.zynaps.math

class Aabb {
    private var minX = Float.POSITIVE_INFINITY
    private var minY = Float.POSITIVE_INFINITY
    private var minZ = Float.POSITIVE_INFINITY
    private var maxX = Float.NEGATIVE_INFINITY
    private var maxY = Float.NEGATIVE_INFINITY
    private var maxZ = Float.NEGATIVE_INFINITY

    fun reset() {
        minX = Float.POSITIVE_INFINITY
        minY = Float.POSITIVE_INFINITY
        minZ = Float.POSITIVE_INFINITY
        maxX = Float.NEGATIVE_INFINITY
        maxY = Float.NEGATIVE_INFINITY
        maxZ = Float.NEGATIVE_INFINITY
    }

    fun pointsBehind(plane: Plane) = (if (plane.dot(minX, minY, minZ) < 0F) 1 else 0) +
                                     (if (plane.dot(minX, minY, maxZ) < 0F) 1 else 0) +
                                     (if (plane.dot(minX, maxY, minZ) < 0F) 1 else 0) +
                                     (if (plane.dot(minX, maxY, maxZ) < 0F) 1 else 0) +
                                     (if (plane.dot(maxX, minY, minZ) < 0F) 1 else 0) +
                                     (if (plane.dot(maxX, minY, maxZ) < 0F) 1 else 0) +
                                     (if (plane.dot(maxX, maxY, minZ) < 0F) 1 else 0) +
                                     (if (plane.dot(maxX, maxY, maxZ) < 0F) 1 else 0)

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

    fun aggregate(box: Aabb) = aggregate(box.minX, box.minY, box.minZ).aggregate(box.maxX, box.maxY, box.maxZ)

    fun aggregate(other: Aabb, matrix: Matrix4): Aabb {
        val a = matrix.m00 * other.minX
        val b = matrix.m10 * other.minX
        val c = matrix.m20 * other.minX
        val d = matrix.m01 * other.minY
        val e = matrix.m11 * other.minY
        val f = matrix.m21 * other.minY
        val g = matrix.m02 * other.minZ
        val h = matrix.m12 * other.minZ
        val i = matrix.m22 * other.minZ
        val j = matrix.m00 * other.maxX
        val k = matrix.m10 * other.maxX
        val l = matrix.m20 * other.maxX
        val m = matrix.m01 * other.maxY
        val n = matrix.m11 * other.maxY
        val o = matrix.m21 * other.maxY
        val p = matrix.m02 * other.maxZ
        val q = matrix.m12 * other.maxZ
        val r = matrix.m22 * other.maxZ
        aggregate(a + m + g + matrix.m03, b + n + h + matrix.m13, c + o + i + matrix.m23)
        aggregate(j + m + g + matrix.m03, k + n + h + matrix.m13, l + o + i + matrix.m23)
        aggregate(j + d + g + matrix.m03, k + e + h + matrix.m13, l + f + i + matrix.m23)
        aggregate(a + d + g + matrix.m03, b + e + h + matrix.m13, c + f + i + matrix.m23)
        aggregate(a + m + p + matrix.m03, b + n + q + matrix.m13, c + o + r + matrix.m23)
        aggregate(j + m + p + matrix.m03, k + n + q + matrix.m13, l + o + r + matrix.m23)
        aggregate(j + d + p + matrix.m03, k + e + q + matrix.m13, l + f + r + matrix.m23)
        aggregate(a + d + p + matrix.m03, b + e + q + matrix.m13, c + f + r + matrix.m23)
        return this
    }
}
