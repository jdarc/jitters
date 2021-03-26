package com.zynaps.tools.convexhull

internal class PHullResult {
    var vertices = emptyArray<Vec3>()
    var indices = mutableListOf<Int>()
    var vertexCount = 0
    var indexCount = 0
    var faceCount = 0
}
