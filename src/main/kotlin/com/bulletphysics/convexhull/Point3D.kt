package com.bulletphysics.convexhull

data class Point3D(val x: Float, val y: Float, val z: Float) {
    internal constructor(v: Vec3) : this(v.x, v.y, v.z)
}
