package com.github.quickhull

internal class HalfEdge(val vertex: Vertex, var face: Face) {
    var next: HalfEdge? = null
    var prev: HalfEdge? = null

    private var oppositeEdge: HalfEdge? = null
    var opposite
        get() = oppositeEdge
        set(value) {
            oppositeEdge = value
            oppositeEdge?.oppositeEdge = this
        }

    val tail get() = prev?.vertex

    val oppositeFace get() = opposite?.face

    val oppFaceDistance get() = face.distanceToPlane(opposite!!.face.centroid)

    val vertexString get() = "${prev?.vertex?.index ?: '?'}-${vertex.index}"

    val lengthSquared get() = prev?.vertex?.run { vertex.point.distanceSquared(point) } ?: -1F
}
