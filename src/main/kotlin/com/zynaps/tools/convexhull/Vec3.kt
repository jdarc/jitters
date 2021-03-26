package com.zynaps.tools.convexhull

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

internal data class Vec3(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0) {

    constructor(v1: Vec3) : this(v1.x, v1.y, v1.z)

    operator fun get(index: Int) = when (index) {
        0 -> x
        1 -> y
        2 -> z
        else -> throw IllegalArgumentException()
    }

    fun set(x: Double, y: Double, z: Double): Vec3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun length() = sqrt(dot(this))

    fun set(v: Vec3) = set(v.x, v.y, v.z)

    fun cross(a: Vec3, b: Vec3) = set(a.y * b.z - a.z * b.y, b.x * a.z - b.z * a.x, a.x * b.y - a.y * b.x)

    fun dot(v: Vec3) = x * v.x + y * v.y + z * v.z

    fun normalize(v: Vec3) = set(v).normalize()

    fun normalize() = scale(1.0 / sqrt((x * x + y * y + z * z)))

    fun add(a: Vec3, v: Vec3) = set(a.x + v.x, a.y + v.y, a.z + v.z)

    fun add(v: Vec3) = set(x + v.x, y + v.y, z + v.z)

    fun sub(a: Vec3, b: Vec3) = set(a.x - b.x, a.y - b.y, a.z - b.z)

    fun mul(scale: Vec3) = set(x * scale.x, y * scale.y, z * scale.z)

    fun negate(v: Vec3) = set(-v.x, -v.y, -v.z)

    fun scale(s: Double, v: Vec3) = set(v).scale(s)

    fun scale(s: Double) = set(x * s, y * s, z * s)

    companion object {

        fun add(dest: Vec3, v1: Vec3, v2: Vec3) {
            dest.x = v1.x + v2.x
            dest.y = v1.y + v2.y
            dest.z = v1.z + v2.z
        }

        fun add(dest: Vec3, v1: Vec3, v2: Vec3, v3: Vec3) {
            dest.x = v1.x + v2.x + v3.x
            dest.y = v1.y + v2.y + v3.y
            dest.z = v1.z + v2.z + v3.z
        }

        fun add(dest: Vec3, v1: Vec3, v2: Vec3, v3: Vec3, v4: Vec3) {
            dest.x = v1.x + v2.x + v3.x + v4.x
            dest.y = v1.y + v2.y + v3.y + v4.y
            dest.z = v1.z + v2.z + v3.z + v4.z
        }

        fun setMin(a: Vec3, b: Vec3) {
            a.x = min(a.x, b.x)
            a.y = min(a.y, b.y)
            a.z = min(a.z, b.z)
        }

        fun setMax(a: Vec3, b: Vec3) {
            a.x = max(a.x, b.x)
            a.y = max(a.y, b.y)
            a.z = max(a.z, b.z)
        }
    }
}
