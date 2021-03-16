package zynaps.quickhull

internal class Vertex {
    val point = Vec3()
    var face: Face? = null
    var index = 0
    var prev: Vertex? = null
    var next: Vertex? = null
}
