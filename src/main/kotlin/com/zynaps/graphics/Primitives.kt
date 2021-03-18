package com.zynaps.graphics

import com.zynaps.math.Matrix4
import com.zynaps.math.Vector3

object Primitives {

    fun createBox(width: Float, height: Float, depth: Float): Assembler {
        val w = 0.5F * width.coerceAtLeast(0.00001F)
        val h = 0.5F * height.coerceAtLeast(0.00001F)
        val d = 0.5F * depth.coerceAtLeast(0.00001F)
        val assembler = Assembler()

        assembler.addVertex(-w,-h, d)
        assembler.addVertex( w,-h, d)
        assembler.addVertex( w, h, d)
        assembler.addVertex(-w, h, d)
        assembler.addVertex(-w,-h,-d)
        assembler.addVertex( w,-h,-d)
        assembler.addVertex( w, h,-d)
        assembler.addVertex(-w, h,-d)

        assembler.addUvCoordinate(0F, 1F)
        assembler.addUvCoordinate(1F, 1F)
        assembler.addUvCoordinate(1F, 0F)
        assembler.addUvCoordinate(0F, 0F)

        assembler.createTriangle(0, 1, 2).uvs(0, 1, 2)
        assembler.createTriangle(2, 3, 0).uvs(2, 3, 0)
        assembler.createTriangle(3, 2, 6).uvs(0, 1, 2)
        assembler.createTriangle(6, 7, 3).uvs(2, 3, 0)
        assembler.createTriangle(5, 4, 7).uvs(0, 1, 2)
        assembler.createTriangle(7, 6, 5).uvs(2, 3, 0)
        assembler.createTriangle(4, 5, 1).uvs(0, 1, 2)
        assembler.createTriangle(1, 0, 4).uvs(2, 3, 0)
        assembler.createTriangle(4, 0, 3).uvs(0, 1, 2)
        assembler.createTriangle(3, 7, 4).uvs(2, 3, 0)
        assembler.createTriangle(1, 5, 6).uvs(0, 1, 2)
        assembler.createTriangle(6, 2, 1).uvs(2, 3, 0)

        assembler.useNormals(NormalType.SURFACE)
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
