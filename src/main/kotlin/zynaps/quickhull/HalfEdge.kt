package zynaps.quickhull

internal class HalfEdge(val vertex: Vertex, var face: Face) {
    var next: HalfEdge? = null
    var prev: HalfEdge? = null
    var opposite: HalfEdge? = null

    val tail get() = prev?.vertex

    val oppositeFace get() = opposite?.face

    val oppFaceDistance get() = face.distanceToPlane(opposite!!.face.centroid)

    val vertexString get() = "${prev?.vertex?.index ?: '?'}-${vertex.index}"

    val lengthSquared get() = prev?.vertex?.run { vertex.point.distanceSquared(point) } ?: -1F

    @JvmName("setOpposite1")
    fun setOpposite(edge: HalfEdge) {
        opposite = edge
        edge.opposite = this
    }
}
