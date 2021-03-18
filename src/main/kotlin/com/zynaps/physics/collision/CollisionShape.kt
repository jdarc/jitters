package com.zynaps.physics.collision

import com.zynaps.math.Matrix4
import com.zynaps.math.Vector3

interface CollisionShape {
    val origin: Vector3
    val basis: Matrix4
    fun getSupport(direction: Vector3): Vector3
}
