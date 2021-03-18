package com.zynaps.physics.geometry

import com.zynaps.math.Matrix4
import com.zynaps.math.Vector3
import com.zynaps.physics.Settings
import com.zynaps.physics.collision.CollisionShape

abstract class Shape : CollisionShape {

    override var origin = Vector3.ZERO

    override var basis = Matrix4.IDENTITY

    open val boundingSphere get() = Settings.HUGE

    var restitution = 0.2F
        set(value) { field = value.coerceIn(0F, 1F) }

    var friction = 0.5F
        set(value) { field = value.coerceIn(0F, 1F) }

    var volume = 1F
        protected set

    open fun calculateBodyInertia(mass: Float) = Matrix4.IDENTITY
}
