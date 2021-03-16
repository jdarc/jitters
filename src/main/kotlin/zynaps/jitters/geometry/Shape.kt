package zynaps.jitters.geometry

import zynaps.jitters.JConfig
import zynaps.jitters.collision.CollisionShape
import zynaps.jitters.collision.MaterialProperties
import zynaps.math.Matrix4
import zynaps.math.Vector3

abstract class Shape : CollisionShape {
    val material = MaterialProperties()

    override var origin = Vector3.ZERO

    override var basis = Matrix4.IDENTITY

    open val boundingSphere get() = JConfig.HUGE

    open fun calculateBodyInertia(mass: Float) = Matrix4.IDENTITY
}
