package com.zynaps.graphics

import com.zynaps.math.Aabb
import com.zynaps.math.Vector3

class Model(private val parts: Map<Material, Mesh>) : Geometry {
    override val bounds = parts.values.fold(Aabb(), { acc, cur -> acc.aggregate(cur.bounds) })

    override fun render(device: Device) = parts.forEach { (material, part) ->
        device.material = material
        part.render(device)
    }

    fun extractPoints() = parts.values.fold(emptyArray<Vector3>(), { acc, cur -> acc + cur.extractPoints() })
}
