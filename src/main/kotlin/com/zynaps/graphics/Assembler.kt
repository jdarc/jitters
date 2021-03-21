/*
 * Copyright (c) 2021 Jean d'Arc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.zynaps.graphics

import com.zynaps.math.Vector3

@Suppress("MemberVisibilityCanBePrivate")
class Assembler {
    private val positions = mutableListOf<Vector3>()
    private val vertexNormals = mutableListOf<Vector3>()
    private val textureCoordinates = mutableListOf<Vector3>()
    private var averagedNormals = listOf<Vector3>()
    private val triangles = mutableListOf<DefaultModifier>()

    fun addVertex(data: FloatArray) = addVertex(data[0], data[1], data[2])
    fun addVertex(x: Float, y: Float, z: Float) {
        positions.add(Vector3(x, y, z))
    }

    fun addNormal(data: FloatArray) = addNormal(data[0], data[1], data[2])
    fun addNormal(x: Float, y: Float, z: Float) {
        vertexNormals.add(Vector3.normalize(Vector3(x, y, z)))
    }

    fun addUvCoordinate(data: FloatArray) = addUvCoordinate(data[0], data[1])
    fun addUvCoordinate(u: Float, v: Float) {
        textureCoordinates.add(Vector3(u, v, 0F))
    }

    fun createTriangle(a: Int, b: Int, c: Int): TriangleModifier = DefaultModifier(a, b, c).apply { triangles.add(this) }

    fun withNormalType(normalType: NormalType) = triangles.forEach { it.normalType = normalType }

    fun withMaterial(material: Material) = triangles.forEach { it.material = material }

    fun centerAndScale() {
        val cog = positions.fold(Vector3.ZERO, { acc, cur -> acc + cur }) / positions.size.toFloat()
        val min = positions.fold(Vector3.ZERO, { acc, cur -> Vector3.min(acc, cur) })
        val max = positions.fold(Vector3.ZERO, { acc, cur -> Vector3.max(acc, cur) })
        val size = max - min
        val resize = kotlin.math.max(size.x, kotlin.math.max(size.y, size.z))
        val scaled = positions.map { (it - cog) / resize }
        positions.clear()
        positions.addAll(scaled)
    }

    fun compile(): Model {
        averagedNormals = computeNormals()

        return Model(triangles.groupBy { it.material }.map { (material, triangles) ->
            material to optimize(triangles.fold(mutableListOf(), ::extractVertices))
        }.toMap())
    }

    private fun computeNormals(): List<Vector3> {
        val normals = Array(positions.size) { Vector3.ZERO }

        triangles.forEach {
            val a = it.a[0]
            val b = it.b[0]
            val c = it.c[0]
            it.surfaceNormal = Vector3.normalize(Vector3.cross(positions[b] - positions[a], positions[c] - positions[a]))
            normals[a] += it.surfaceNormal
            normals[b] += it.surfaceNormal
            normals[c] += it.surfaceNormal
        }

        return normals.map { Vector3.normalize(it) }
    }

    private fun extractVertices(vertices: MutableList<Vertex>, triangle: DefaultModifier): MutableList<Vertex> {
        vertices.add(buildVertex(triangle, triangle.a))
        vertices.add(buildVertex(triangle, triangle.b))
        vertices.add(buildVertex(triangle, triangle.c))
        return vertices
    }

    private fun buildVertex(modifier: DefaultModifier, indexer: IntArray) =
        Vertex(
            positions[indexer[0]], when (modifier.normalType) {
                NormalType.AVERAGED -> averagedNormals[indexer[0]]
                NormalType.VERTEX -> vertexNormals[indexer[1]]
                else -> modifier.surfaceNormal
            }, if (indexer[2] < textureCoordinates.size) textureCoordinates[indexer[2]] else Vector3.ZERO
        )

    private companion object {

        fun optimize(vb: List<Vertex>): Mesh {
            val vertices = mutableMapOf<Vertex, Int>()
            val indices = vb.indices.map { vertices.getOrPut(vb[it], { vertices.size }) }
            return Mesh(toVertexBuffer(vertices.keys.toTypedArray()), indices.toIntArray())
        }

        fun toVertexBuffer(vertices: Array<Vertex>) =
            vertices.fold(floatArrayOf(), { acc, vertex -> acc + vertex.toArray() }).toTypedArray().toFloatArray()

        data class Vertex(val position: Vector3, val normal: Vector3, val uv: Vector3) {
            fun toArray() = floatArrayOf(position.x, position.y, position.z, normal.x, normal.y, normal.z, uv.x, uv.y)
        }

        class DefaultModifier(a: Int, b: Int, c: Int) : TriangleModifier {
            val a = intArrayOf(a, 0, 0)
            val b = intArrayOf(b, 0, 0)
            val c = intArrayOf(c, 0, 0)

            override var material: Material = Material.DEFAULT
            override var normalType = NormalType.SURFACE

            var surfaceNormal = Vector3.ZERO

            override fun withNormalIndex(n0: Int, n1: Int, n2: Int): TriangleModifier {
                a[1] = n0; b[1] = n1; c[1] = n2
                normalType = NormalType.VERTEX
                return this
            }

            override fun withUvIndex(t0: Int, t1: Int, t2: Int): TriangleModifier {
                a[2] = t0; b[2] = t1; c[2] = t2
                return this
            }
        }
    }
}
