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

package com.zynaps.physics.dynamics

import com.zynaps.math.Matrix4
import com.zynaps.math.Vector3
import com.zynaps.physics.Settings
import com.zynaps.physics.geometry.Shape
import kotlin.math.max

@Suppress("unused", "MemberVisibilityCanBePrivate")
class RigidBody(val skin: Shape) {
    val id = ID++

    private var bodyInertia = Matrix4.IDENTITY
    private var bodyInvInertia = Matrix4.IDENTITY
    private var invOrientation = Matrix4.IDENTITY

    private val storeState = PhysicsState()
    private val newState = PhysicsState()
    private val oldState = PhysicsState()

    var applyGravity = true

    var isMovable = true

    var isActive = true
        set(value) {
            field = isMovable && value
        }

    var mass = 0F
        set(value) {
            field = value
            bodyInertia = skin.calculateBodyInertia(field)
            bodyInvInertia = Matrix4.invert(bodyInertia)
            updateInertia()
        }

    val inverseMass get() = 1F / mass

    var position
        get() = newState.position
        set(value) {
            newState.position = value
        }

    var orientation
        get() = newState.orientation
        set(value) {
            newState.orientation = value
            updateInertia()
        }

    var linearVelocity
        get() = newState.linearVelocity
        set(value) {
            newState.linearVelocity = value
        }

    var linearVelocityDamping = Vector3(0.999F, 0.999F, 0.999F)
        set(value) {
            field = Vector3.clamp(value, Vector3.ZERO, Vector3.ONE)
        }

    var maxLinearVelocities = Vector3(Settings.HUGE, Settings.HUGE, Settings.HUGE)
        set(value) {
            field = Vector3.abs(value)
        }

    var angularVelocity
        get() = newState.angularVelocity
        set(value) {
            newState.angularVelocity = value
        }

    var angularVelocityDamping = Vector3(0.999F, 0.999F, 0.999F)
        set(value) {
            field = Vector3.clamp(value, Vector3.ZERO, Vector3.ONE)
        }

    var maxAngularVelocities = Vector3(Settings.HUGE, Settings.HUGE, Settings.HUGE)
        set(value) {
            field = Vector3.abs(value)
        }

    var velocityChanged = true
        private set

    var force = Vector3.ZERO

    var torque = Vector3.ZERO

    var worldInertia = Matrix4.IDENTITY
        private set

    var worldInvInertia = Matrix4.IDENTITY
        private set

    fun copyState() = oldState.copy(newState)

    fun storeState() = storeState.copy(newState)

    fun restoreState() {
        newState.copy(storeState)
        updateInertia()
    }

    fun hitTest(other: RigidBody): Boolean {
        val sumRadius = skin.boundingSphere + other.skin.boundingSphere
        return (newState.position - other.newState.position).lengthSquared <= sumRadius * sumRadius
    }

    fun moveTo(position: Vector3, orientation: Matrix4) {
        newState.position = position
        newState.orientation = orientation
        newState.linearVelocity = Vector3.ZERO
        newState.angularVelocity = Vector3.ZERO
        copyState()
    }

    fun clearForces() {
        force = Vector3.ZERO
        torque = Vector3.ZERO
    }

    fun addWorldForce(force: Vector3) {
        if (!isMovable) return
        this.force += force
        velocityChanged = true
    }

    fun addWorldForce(force: Vector3, position: Vector3) {
        if (!isMovable) return
        this.force += force
        torque += Vector3.cross(position - newState.position, force)
        velocityChanged = true
    }

    fun addWorldTorque(torque: Vector3) {
        if (!isMovable) return
        this.torque += torque
        velocityChanged = true
    }

    fun applyWorldImpulse(impulse: Vector3, position: Vector3) {
        if (!isMovable) return
        newState.linearVelocity += impulse * inverseMass
        newState.angularVelocity += worldInvInertia * Vector3.cross(position - newState.position, impulse)
        velocityChanged = true
    }

    fun addBodyForce(force: Vector3) {
        if (!isMovable) return
        this.force += newState.orientation * force
        velocityChanged = true
    }

    fun addBodyForce(force: Vector3, position: Vector3) {
        if (!isMovable) return
        val acc = newState.orientation * force
        val pos = newState.orientation * position
        this.force += acc
        torque += Vector3.cross(pos, acc)
        velocityChanged = true
    }

    fun addBodyTorque(torque: Vector3) {
        if (!isMovable) return
        this.torque += newState.orientation * torque
        velocityChanged = true
    }

    fun applyBodyWorldImpulse(impulse: Vector3, delta: Vector3) {
        if (!isMovable) return
        newState.linearVelocity += impulse * inverseMass
        newState.angularVelocity += worldInvInertia * Vector3.cross(delta, impulse)
        velocityChanged = true
    }

    fun updateVelocity(dt: Float) {
        if (!isMovable || !isActive) return
        newState.linearVelocity += force * (dt * inverseMass)
        newState.angularVelocity += worldInvInertia * torque * dt
        newState.angularVelocity *= 0.999F
    }

    fun updatePosition(dt: Float) {
        if (isMovable && isActive) {
            clampLinearVelocity()
            clampAngularVelocity()

            val angMomBefore = worldInertia * newState.angularVelocity

            newState.position += newState.linearVelocity * dt
            newState.orientation = computeAngularTransform(newState.angularVelocity, dt) * newState.orientation

            invOrientation = Matrix4.transpose(newState.orientation)
            worldInvInertia = newState.orientation * bodyInvInertia * invOrientation
            worldInertia = newState.orientation * bodyInertia * invOrientation
            angularVelocity = worldInvInertia * angMomBefore
        }
        skin.origin = position
        skin.basis = orientation
    }

    fun velocityRelativeTo(position: Vector3) = newState.linearVelocity + Vector3.cross(newState.angularVelocity, position)

    private fun clampLinearVelocity() {
        newState.linearVelocity = Vector3.clamp(newState.linearVelocity, -maxLinearVelocities, maxLinearVelocities)
    }

    private fun clampAngularVelocity() {
        val v = Vector3.abs(newState.angularVelocity) / maxAngularVelocities
        val f = max(v.x, max(v.y, v.z))
        if (f < 1F) return
        newState.angularVelocity /= f
    }

    private fun updateInertia() {
        invOrientation = Matrix4.transpose(newState.orientation)
        worldInertia = bodyInertia * newState.orientation * invOrientation
        worldInvInertia = bodyInvInertia * newState.orientation * invOrientation
    }

    private fun computeAngularTransform(angularVelocity: Vector3, dt: Float): Matrix4 {
        val ang = angularVelocity.length
        if (ang < Settings.TINY) return orientation
        return Matrix4.orthonormalise(Matrix4.createFromAxisAngle(angularVelocity, ang * dt))
    }

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
