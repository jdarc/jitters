package zynaps.graphics

import zynaps.math.Vector3
import zynaps.quickhull.QuickHull3D

class Model(private val parts: Map<Material, Mesh>) : Geometry {
    override val bounds = parts.values.fold(Aabb(), { acc, cur -> acc.aggregate(cur.bounds); acc })

    override fun render(device: Device) = parts.forEach { (material, part) ->
        device.material = material
        part.render(device)
    }

    fun extractPoints() = parts.values.fold(emptyArray<Vector3>(), { acc, cur -> acc + cur.extractPoints() })

    fun computeHull(): Model {
        val hull = QuickHull3D(extractPoints().fold(FloatArray(0), { acc, cur -> acc + floatArrayOf(cur.x, cur.y, cur.z) }))

        val assembler = Assembler()

        hull.vertices.toList().windowed(3, 3).forEach { (x, y, z) -> assembler.addVertex(x, y, z) }
        hull.faces.forEach { for (i in 1 until it.size - 1) assembler.createTriangle(it[0], it[i], it[i + 1]) }

        return assembler.useNormals(NormalType.SURFACE).changeMaterial(ColorMaterial(0xFF8800)).compile()
    }
}
