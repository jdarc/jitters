package zynaps.jitters.collision

interface CollisionDetector {
    fun collide(shape0: CollisionShape, shape1: CollisionShape, collisionMargin: Float = 0.001F): CollisionDetails
}
