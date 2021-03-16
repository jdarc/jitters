package zynaps.graphics

import zynaps.math.Matrix4
import zynaps.math.Vector3

object Primitives {

    fun createPlane(): Assembler {
        val assembler = Assembler()

        assembler.addVertex(-1F, 0F, 1F)
        assembler.addVertex(1F, 0F, 1F)
        assembler.addVertex(1F, 0F, -1F)
        assembler.addVertex(-1F, 0F, -1F)

        assembler.addNormal(0F, 1F, 0F)

        assembler.addUvCoordinate(0F, 0F)
        assembler.addUvCoordinate(1F, 0F)
        assembler.addUvCoordinate(1F, 1F)
        assembler.addUvCoordinate(0F, 1F)

        assembler.createTriangle(0, 1, 2).normals(0, 0, 0).uvs(0, 1, 2)
        assembler.createTriangle(2, 3, 0).normals(0, 0, 0).uvs(2, 3, 0)

        return assembler
    }

    fun createSphere(radius: Float, stacks: Int, slices: Int): Assembler {
        val assembler = Assembler()
        val stackAngle = (kotlin.math.PI / stacks).toFloat()
        val sliceAngle = (2 * kotlin.math.PI / slices).toFloat()

        val curve = mutableListOf<Vector3>()
        for (stack in 0..stacks) {
            curve.add(Vector3.UNIT_Y * Matrix4.createRotationZ(stackAngle * stack))
        }

        for (slice in 0..slices) {
            val aboutY = Matrix4.createRotationY(sliceAngle * slice)
            for ((v, point) in curve.withIndex()) {
                val vertex = point * aboutY
                assembler.addVertex(vertex.x * radius, vertex.y * radius, vertex.z * radius)
                assembler.addNormal(vertex.x, vertex.y, vertex.z)
                assembler.addUvCoordinate(slice / slices.toFloat(), v / curve.size.toFloat())
            }
        }

        for (slice in 0 until slices) {
            var tindex = slice * (stacks + 1)
            for (stack in 0 until stacks - 1) {
                val ma = tindex + stacks + 2
                val mb = tindex
                val mc = tindex + 1
                val me = tindex + stacks + 3
                assembler.createTriangle(ma, mb, mc).uvs(ma, mb, mc).normals(ma, mb, mc)
                assembler.createTriangle(mc, me, ma).uvs(mc, me, ma).normals(mc, me, ma)
                tindex++
            }
        }

        return assembler
    }

//    fun sphere(radius: Float, stacks: Int, slices: Int): Mesh {
//        val stackAngle = (kotlin.math.PI / stacks).toFloat()
//        val sliceAngle = (2.0 * kotlin.math.PI / slices).toFloat()
//
//        val curve = mutableListOf<Vector3>()
//        for (stack in 1 until stacks) curve.add(Vector3.UNIT_Y * Matrix4.rotationZ(stackAngle * stack))
//
//        val vertices = mutableListOf<Vector3>()
//        for (slice in 0..slices) {
//            val aboutY = Matrix4.rotationY(sliceAngle * slice)
//            for (point in curve) vertices.add(point * aboutY * radius)
//        }
//
//        val faces = mutableListOf<Int>()
//        for (slice in 0 until slices) {
//            val offset = slice * curve.size
//            for (stack in 0 until curve.size - 1) {
//                val index = offset + stack
//                faces.add(4)
//                faces.add(index)
//                faces.add(index + 1)
//                faces.add(index + curve.size + 1)
//                faces.add(index + curve.size)
//            }
//        }
//
//        vertices.add(Vector3(0F, radius, 0F))
//        vertices.add(Vector3(0F, -radius, 0F))
//        for (slice in 0 until slices - 1) {
//            faces.add(3)
//            faces.add(vertices.size - 2)
//            faces.add(slice * curve.size)
//            faces.add((slice + 1) * curve.size)
//            faces.add(3)
//            faces.add(vertices.size - 1)
//            faces.add((slice + 1) * curve.size + curve.size - 1)
//            faces.add(slice * curve.size + curve.size - 1)
//        }
//
//        return Mesh(vertices.toTypedArray(), faces.toIntArray())
//    }
}
