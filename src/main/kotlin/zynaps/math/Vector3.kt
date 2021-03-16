package zynaps.math

import zynaps.math.Scalar.max
import zynaps.math.Scalar.min
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

data class Vector3(val x: Float, val y: Float, val z: Float) {

    val lengthSquared get() = dot(this, this)

    val length get() = sqrt(lengthSquared)

    operator fun unaryMinus() = Vector3(-x, -y, -z)

    operator fun plus(v: Vector3) = Vector3(x + v.x, y + v.y, z + v.z)

    operator fun minus(v: Vector3) = Vector3(x - v.x, y - v.y, z - v.z)

    operator fun div(s: Float) = Vector3(x / s, y / s, z / s)

    operator fun div(v: Vector3) = Vector3(x / v.x, y / v.y, z / v.z)

    operator fun times(s: Float) = Vector3(x * s, y * s, z * s)

    operator fun times(v: Vector3) = Vector3(x * v.x, y * v.y, z * v.z)

    operator fun times(m: Matrix4) = Vector3(
        x * m.m00 + y * m.m10 + z * m.m20 + m.m30,
        x * m.m01 + y * m.m11 + z * m.m21 + m.m31,
        x * m.m02 + y * m.m12 + z * m.m22 + m.m32
    )

    companion object {
        private const val EPSILON = 0.00001F
        private val RND = Random(System.nanoTime())

        val ONE = Vector3(1F, 1F, 1F)
        val ZERO = Vector3(0F, 0F, 0F)

        val UNIT_X = Vector3(1F, 0F, 0F)
        val UNIT_Y = Vector3(0F, 1F, 0F)
        val UNIT_Z = Vector3(0F, 0F, 1F)

        fun isZero(v: Vector3) = abs(v.x) < EPSILON && abs(v.y) < EPSILON && abs(v.z) < EPSILON

        fun abs(value: Vector3) = Vector3(abs(value.x), abs(value.y), abs(value.z))

        fun min(a: Vector3, b: Vector3) = Vector3(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z))

        fun max(a: Vector3, b: Vector3) = Vector3(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))

        fun clamp(value: Vector3, min: Vector3, max: Vector3) = min(max, max(value, min))

        fun dot(a: Vector3, b: Vector3) = a.x * b.x + a.y * b.y + a.z * b.z

        fun cross(a: Vector3, b: Vector3) = Vector3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)

        fun normalize(v: Vector3) = v * Scalar.invSqrt(dot(v, v))

        fun random() = normalize(Vector3(RND.nextFloat() * 2 - 1, RND.nextFloat() * 2 - 1, RND.nextFloat() * 2 - 1))

        fun transformNormal(v: Vector3, m: Matrix4) = Vector3(
            v.x * m.m00 + v.y * m.m10 + v.z * m.m20,
            v.x * m.m01 + v.y * m.m11 + v.z * m.m21,
            v.x * m.m02 + v.y * m.m12 + v.z * m.m22
        )
    }
}
