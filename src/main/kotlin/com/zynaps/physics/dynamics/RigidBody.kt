package com.zynaps.physics.dynamics

import com.zynaps.physics.Settings
import com.zynaps.physics.geometry.Shape
import com.zynaps.math.Matrix4
import com.zynaps.math.Scalar.max
import com.zynaps.math.Vector3
import kotlin.math.cos
import kotlin.math.sin

@Suppress("unused", "MemberVisibilityCanBePrivate")
class RigidBody(val skin: Shape) {
    val id = UID++

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

    init {
        mass = 1F
        moveTo(Vector3.ZERO, Matrix4.IDENTITY)
    }

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
        newState.angularVelocity += Vector3.cross(position - newState.position, impulse) * worldInvInertia
        velocityChanged = true
    }

    fun addBodyForce(force: Vector3) {
        if (!isMovable) return
        this.force += force * newState.orientation
        velocityChanged = true
    }

    fun addBodyForce(force: Vector3, position: Vector3) {
        if (!isMovable) return
        val acc = force * newState.orientation
        val pos = position * newState.orientation
        this.force += acc
        torque += Vector3.cross(newState.position + pos - newState.position, acc)
        velocityChanged = true
    }

    fun addBodyTorque(torque: Vector3) {
        if (!isMovable) return
        this.torque += torque * newState.orientation
        velocityChanged = true
    }

    fun applyBodyWorldImpulse(impulse: Vector3, delta: Vector3) {
        if (!isMovable) return
        newState.linearVelocity += impulse * inverseMass
        newState.angularVelocity += Vector3.cross(delta, impulse) * worldInvInertia
        velocityChanged = true
    }

    fun updateVelocity(dt: Float) {
        if (!isMovable || !isActive) return
        newState.linearVelocity += force * (dt * inverseMass)
        newState.angularVelocity += torque * dt * worldInvInertia
        newState.angularVelocity *= 0.999F
    }

    fun updatePosition(dt: Float) {
        if (isMovable && isActive) {
            clampLinearVelocity()
            clampAngularVelocity()

            val angMomBefore = Vector3.transformNormal(newState.angularVelocity, worldInertia)

            newState.position += newState.linearVelocity * dt
            newState.orientation = addAngularVelocityToOrientation(newState.angularVelocity, newState.orientation, dt)

            // invOrientation = Matrix.Transpose(transform.Orientation);
            invOrientation = Matrix4.transpose(newState.orientation)

            // worldInvInertia = transform.Orientation * bodyInvInertia * invOrientation;
            worldInvInertia = invOrientation * newState.orientation * bodyInvInertia

            // worldInertia = transform.Orientation * bodyInertia * invOrientation;
            worldInertia = invOrientation * bodyInertia * newState.orientation

            // angularVelocity = Vector3.Transform(angMomBefore, worldInvInertia);
            newState.angularVelocity = Vector3.transformNormal(angMomBefore, worldInvInertia)
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
        worldInertia = invOrientation * newState.orientation * bodyInertia
        worldInvInertia = invOrientation * newState.orientation * bodyInvInertia
    }

    private fun addAngularVelocityToOrientation(angularVelocity: Vector3, orientation: Matrix4, dt: Float): Matrix4 {
        val ang = angularVelocity.length
        if (ang <= Settings.TINY) return orientation
        val dir = angularVelocity / ang
        val rad = -ang * dt
        val c = cos(rad)
        val s = sin(rad)
        val t = 1F - c
        val r0 = c + dir.x * dir.x * t
        val r5 = c + dir.y * dir.y * t
        val ra = c + dir.z * dir.z * t
        val r4 = dir.x * dir.y * t + dir.z * s
        val r1 = dir.x * dir.y * t - dir.z * s
        val r8 = dir.x * dir.z * t - dir.y * s
        val r2 = dir.x * dir.z * t + dir.y * s
        val r9 = dir.y * dir.z * t + dir.x * s
        val r6 = dir.y * dir.z * t - dir.x * s
        val m11 = r0 * orientation.m00 + r4 * orientation.m01 + r8 * orientation.m02
        val m12 = r1 * orientation.m00 + r5 * orientation.m01 + r9 * orientation.m02
        val m13 = r2 * orientation.m00 + r6 * orientation.m01 + ra * orientation.m02
        val m21 = r0 * orientation.m10 + r4 * orientation.m11 + r8 * orientation.m12
        val m22 = r1 * orientation.m10 + r5 * orientation.m11 + r9 * orientation.m12
        val m23 = r2 * orientation.m10 + r6 * orientation.m11 + ra * orientation.m12
        val m31 = r0 * orientation.m20 + r4 * orientation.m21 + r8 * orientation.m22
        val m32 = r1 * orientation.m20 + r5 * orientation.m21 + r9 * orientation.m22
        val m33 = r2 * orientation.m20 + r6 * orientation.m21 + ra * orientation.m22
        return Matrix4(m11, m12, m13, 0F, m21, m22, m23, 0F, m31, m32, m33, 0F, 0F, 0F, 0F, 1F)
    }

    init {
        mass = 1F
    }

    private companion object {
        var UID = 0

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