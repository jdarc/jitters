package com.zynaps.tools.convexhull

data class Point3D(val x: Double, val y: Double, val z: Double) {
    internal constructor(v: Vec3) : this(v.x, v.y, v.z)
}
