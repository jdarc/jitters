/*
 * Copyright (c) 2021 Jean d'Arc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.zynaps.physics

import com.zynaps.math.Matrix4
import com.zynaps.math.Scalar.sqr
import com.zynaps.math.Vector3
import com.zynaps.physics.geometry.CollisionSkin

@Suppress("unused", "MemberVisibilityCanBePrivate")
class RigidBody(val skin: CollisionSkin) {
    val id = ID++

    private var bodyInertia = Matrix4.IDENTITY
    private var bodyInvInertia = Matrix4.IDENTITY
    private var invOrientation = Matrix4.IDENTITY

    private val storeState = PhysicsState()
    private val state = PhysicsState()

    var isMovable = true

    var isActive = true
        set(value) {
            field = isMovable && value
        }

    var mass = 1F
        set(value) {
            field = value.coerceAtLeast(Globals.TINY)
            bodyInertia = skin.calculateBodyInertia(field)
            bodyInvInertia = Matrix4.invert(bodyInertia)
            updateInertia()
        }

    val inverseMass get() = 1F / mass

    var position
        get() = state.position
        set(value) {
            state.position = value
        }

    var orientation
        get() = state.orientation
        set(value) {
            state.orientation = value
            updateInertia()
        }

    var linearVelocity
        get() = state.linearVelocity
        set(value) {
            state.linearVelocity = value
        }

    var linearVelocityDamping = Vector3(0.9995)
        set(value) {
            field = Vector3.clamp(value, Vector3.ZERO, Vector3.ONE)
        }

    var angularVelocity
        get() = state.angularVelocity
        set(value) {
            state.angularVelocity = value
        }

    var angularVelocityDamping = Vector3(0.9995)
        set(value) {
            field = Vector3.clamp(value, Vector3.ZERO, Vector3.ONE)
        }

    var force = Vector3.ZERO

    var torque = Vector3.ZERO

    var worldInertia = Matrix4.IDENTITY
        private set

    var worldInvInertia = Matrix4.IDENTITY
        private set

    fun storeState() = storeState.copy(state)

    fun restoreState() {
        state.copy(storeState)
        updateInertia()
    }

    fun hitTest(other: RigidBody) = (position - other.position).lengthSquared() <= sqr(skin.boundingSphere + other.skin.boundingSphere)

    fun moveTo(position: Vector3, orientation: Matrix4 = Matrix4.IDENTITY) {
        state.position = position
        state.orientation = orientation
        state.linearVelocity = Vector3.ZERO
        state.angularVelocity = Vector3.ZERO
    }

    fun clearForces() {
        force = Vector3.ZERO
        torque = Vector3.ZERO
    }

    fun addWorldForce(force: Vector3) {
        if (!isMovable) return
        this.force += force
    }

    fun addWorldForce(force: Vector3, position: Vector3) {
        if (!isMovable) return
        this.force += force
        torque += Vector3.cross(position - state.position, force)
    }

    fun addWorldTorque(torque: Vector3) {
        if (!isMovable) return
        this.torque += torque
    }

    fun applyWorldImpulse(impulse: Vector3, position: Vector3) {
        if (!isMovable) return
        linearVelocity += impulse * inverseMass
        angularVelocity += worldInvInertia * Vector3.cross(position - state.position, impulse)
    }

    fun addBodyForce(force: Vector3) {
        if (!isMovable) return
        this.force += orientation * force
    }

    fun addBodyForce(force: Vector3, position: Vector3) {
        if (!isMovable) return
        this.force += orientation * force
        torque += Vector3.cross(orientation * position, orientation * force)
    }

    fun addBodyTorque(torque: Vector3) {
        if (!isMovable) return
        this.torque += orientation * torque
    }

    fun applyBodyWorldImpulse(impulse: Vector3, delta: Vector3) {
        if (!isMovable) return
        linearVelocity += impulse * inverseMass
        angularVelocity += worldInvInertia * Vector3.cross(delta, impulse)
    }

    fun updateVelocity(dt: Float) {
        if (!isMovable || !isActive) return
        linearVelocity = linearVelocityDamping * (linearVelocity + force * (inverseMass * dt))
        angularVelocity = angularVelocityDamping * (angularVelocity + worldInvInertia * torque * dt)
    }

    fun updatePosition(dt: Float) {
        if (isMovable && isActive) {
            val angMomBefore = Matrix4.transformNormal(worldInertia, angularVelocity)

            position += linearVelocity * dt
            val ang = angularVelocity.length() * dt
            if (ang > Globals.TINY) orientation = Matrix4.createFromAxisAngle(angularVelocity, ang) * orientation

            updateInertia()
            angularVelocity = Matrix4.transformNormal(worldInvInertia, angMomBefore)
        }
        skin.origin = position
        skin.basis = orientation
    }

    fun velocityRelativeTo(position: Vector3) = linearVelocity + Vector3.cross(angularVelocity, position)

    private fun updateInertia() {
        invOrientation = Matrix4.transpose(orientation)
        worldInertia = orientation * bodyInertia * invOrientation
        worldInvInertia = orientation * bodyInvInertia * invOrientation
    }

    override fun equals(other: Any?) = when {
        this === other -> true
        javaClass != other?.javaClass -> false
        else -> id == (other as RigidBody).id
    }

    override fun hashCode() = id

    init {
        mass = 1F
    }

    private companion object {
        var ID = 0

        class PhysicsState {
            var position = Vector3.ZERO
            var orientation = Matrix4.IDENTITY
            var linearVelocity = Vector3.ZERO
            var angularVelocity = Vector3.ZERO

            fun copy(other: PhysicsState) {
                position = other.position
                orientation = other.orientation
                linearVelocity = other.linearVelocity
                angularVelocity = other.angularVelocity
            }
        }
    }
}
