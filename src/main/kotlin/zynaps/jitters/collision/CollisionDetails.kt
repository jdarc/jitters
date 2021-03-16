package zynaps.jitters.collision

import zynaps.math.Vector3

interface CollisionDetails {
    val pointA: Vector3
    val pointB: Vector3
    val normal: Vector3
    val collided: Boolean
    val depth: Float
}
