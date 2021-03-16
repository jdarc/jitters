package zynaps.jitters.collision

import zynaps.math.Vector3

class CollisionPoints(var r0: Vector3 = Vector3.ZERO, var r1: Vector3 = Vector3.ZERO, var initialPenetration: Float = 0F) {
    var minSeparationVel = 0F
    var denominator = 0F
}
