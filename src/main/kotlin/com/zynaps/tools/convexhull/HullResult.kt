package com.zynaps.tools.convexhull

@Suppress("unused")
class HullResult(val vertices: Array<Point3D>, val indices: IntArray, val success: Boolean = false) {
    val numFaces = indices.size / 3
}
