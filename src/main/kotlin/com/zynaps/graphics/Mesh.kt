package com.zynaps.graphics

import com.zynaps.math.Aabb
import com.zynaps.math.Vector3

class Mesh(private val vertexBuffer: FloatArray, private val indexBuffer: IntArray) : Geometry {
    override val bounds = extractPoints().fold(Aabb(), { acc, point -> acc.aggregate(point.x, point.y, point.z) })

    override fun render(device: Device) = device.draw(vertexBuffer, indexBuffer, indexBuffer.size)

    fun extractPoints() = vertexBuffer.toList().windowed(3, 8) { (x, y, z) -> Vector3(x, y, z) }.toTypedArray()
}
