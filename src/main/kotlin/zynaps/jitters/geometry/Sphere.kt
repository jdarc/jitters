package zynaps.jitters.geometry

import zynaps.math.Matrix4
import zynaps.math.Vector3

class Sphere(radius: Float) : Shape() {

    var radius = radius.coerceIn(Float.MIN_VALUE, Float.MAX_VALUE)
        set(value) {
            field = value.coerceIn(Float.MIN_VALUE, Float.MAX_VALUE)
        }

    override val boundingSphere get() = radius

    override fun calculateBodyInertia(mass: Float) = Matrix4.createScale(0.4F * mass * radius * radius)

    override fun getSupport(direction: Vector3) = origin + direction * (radius / direction.length)
}
