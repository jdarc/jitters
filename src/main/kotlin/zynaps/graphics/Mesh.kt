package zynaps.graphics

import zynaps.math.Vector3

class Mesh(val vertexBuffer: FloatArray, val indexBuffer: IntArray) {
    val bounds = computeLocalBounds()

    fun render(device: Device) {
        device.draw(vertexBuffer, indexBuffer, indexBuffer.size)
    }

    fun extractPoints() = vertexBuffer.toList().windowed(3, 8) { (x, y, z) -> Vector3(x, y, z) }.toTypedArray()

    private fun computeLocalBounds(): Aabb {
        val localBounds = Aabb()
        for (i in vertexBuffer.indices step 8) localBounds.aggregate(vertexBuffer[i], vertexBuffer[i + 1], vertexBuffer[i + 2])
        return localBounds
    }
}
