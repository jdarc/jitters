package com.zynaps.physics.collision

import com.zynaps.math.Vector3

class CollisionPoints(val r0: Vector3, val r1: Vector3, val initialPenetration: Float) {
    var minSeparationVel = 0F
    var denominator = 0F
}
