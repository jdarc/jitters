package zynaps.jitters.collision

import zynaps.math.Matrix4
import zynaps.math.Vector3

interface CollisionShape {
    val origin: Vector3
    val basis: Matrix4
    fun getSupport(direction: Vector3): Vector3
}
