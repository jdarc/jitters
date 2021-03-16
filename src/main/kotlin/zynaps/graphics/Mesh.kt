package zynaps.graphics

import zynaps.math.Vector3

class Mesh(private val vertexBuffer: FloatArray, private val indexBuffer: IntArray) {
    val bounds = computeLocalBounds()

    fun render(device: Device) = device.draw(vertexBuffer, indexBuffer, indexBuffer.size)

    fun extractPoints() = vertexBuffer.toList().windowed(3, 8) { (x, y, z) -> Vector3(x, y, z) }.toTypedArray()

    private fun computeLocalBounds() = extractPoints().fold(Aabb(), { acc, cur -> acc.aggregate(cur.x, cur.y, cur.z) })
}
