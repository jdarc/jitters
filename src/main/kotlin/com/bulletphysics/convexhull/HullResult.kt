package com.bulletphysics.convexhull

class HullResult(val vertices: List<Point3D>, val indices: IntArray, val numFaces: Int = 0, val success: Boolean = false) {
    val numVertices = vertices.size
    val numIndices = indices.size
}
