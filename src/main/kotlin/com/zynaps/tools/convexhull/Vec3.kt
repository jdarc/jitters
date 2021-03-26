package com.zynaps.tools.convexhull

import kotlin.math.sqrt

internal data class Vec3(var x: Double = 0.0, var y: Double = 0.0, var z: Double = 0.0) {

    constructor(v: Vec3) : this(v.x, v.y, v.z)

    fun length() = sqrt(dot(this))

    fun set(x: Double, y: Double, z: Double): Vec3 {
        this.x = x
        this.y = y
        this.z = z
        return this
    }

    fun set(v: Vec3) = set(v.x, v.y, v.z)

    fun negate(v: Vec3) = set(-v.x, -v.y, -v.z)

    fun add(v: Vec3) = set(x + v.x, y + v.y, z + v.z)

    fun sub(v: Vec3) = set(x - v.x, y - v.y, z - v.z)

    fun scale(s: Double) = set(x * s, y * s, z * s)

    fun scale(v: Vec3) = set(x * v.x, y * v.y, z * v.z)

    fun dot(v: Vec3) = x * v.x + y * v.y + z * v.z

    fun normalize() = scale(1.0 / sqrt((x * x + y * y + z * z)))

    fun cross(a: Vec3, b: Vec3) = set(a.y * b.z - a.z * b.y, b.x * a.z - b.z * a.x, a.x * b.y - a.y * b.x)
}
