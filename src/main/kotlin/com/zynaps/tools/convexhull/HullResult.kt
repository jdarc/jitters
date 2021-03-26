package com.zynaps.tools.convexhull

@Suppress("unused")
class HullResult(val vertices: List<Point3D>, val indices: IntArray, val success: Boolean = false) {
    val numFaces = indices.size / 3
}
