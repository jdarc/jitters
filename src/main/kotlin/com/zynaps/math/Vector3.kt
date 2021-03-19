package com.zynaps.math

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

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

    companion object {
        val ONE = Vector3(1F, 1F, 1F)
        val ZERO = Vector3(0F, 0F, 0F)

        val UNIT_X = Vector3(1F, 0F, 0F)
        val UNIT_Y = Vector3(0F, 1F, 0F)
        val UNIT_Z = Vector3(0F, 0F, 1F)

        fun abs(value: Vector3) = Vector3(abs(value.x), abs(value.y), abs(value.z))

        fun min(a: Vector3, b: Vector3) = Vector3(min(a.x, b.x), min(a.y, b.y), min(a.z, b.z))

        fun max(a: Vector3, b: Vector3) = Vector3(max(a.x, b.x), max(a.y, b.y), max(a.z, b.z))

        fun clamp(value: Vector3, min: Vector3, max: Vector3) = min(max, max(value, min))

        fun dot(a: Vector3, b: Vector3) = a.x * b.x + a.y * b.y + a.z * b.z

        fun cross(a: Vector3, b: Vector3) = Vector3(a.y * b.z - a.z * b.y, a.z * b.x - a.x * b.z, a.x * b.y - a.y * b.x)

        fun normalize(v: Vector3) = v / sqrt(dot(v, v))
    }
}
