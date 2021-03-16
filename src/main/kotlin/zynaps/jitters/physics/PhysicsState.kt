package zynaps.jitters.physics

import zynaps.math.Matrix4
import zynaps.math.Vector3

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
