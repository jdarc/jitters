package zynaps.graphics

import zynaps.math.Vector3

class Assembler {
    private val vertices = mutableListOf<Vector3>()
    private val vertexNormals = mutableListOf<Vector3>()
    private val triangleNormals = mutableListOf<Vector3>()
    private val uvCoordinates = mutableListOf<Vector3>()
    private val triangles = mutableListOf<DefaultModifier>()

    fun addVertex(x: Float, y: Float, z: Float) {
        vertices.add(Vector3(x, y, z))
        vertexNormals.add(Vector3.ZERO)
    }

    fun addVertex(data: FloatArray) = addVertex(data[0], data[1], data[2])

    fun addNormal(x: Float, y: Float, z: Float) {
        triangleNormals.add(Vector3.normalize(Vector3(x, y, z)))
    }

    fun addNormal(data: FloatArray) = addNormal(data[0], data[1], data[2])

    fun addUvCoordinate(u: Float, v: Float) {
        uvCoordinates.add(Vector3(u, v, 0F))
    }

    fun addUvCoordinate(data: FloatArray) = addUvCoordinate(data[0], data[1])

    fun createTriangle(a: Int, b: Int, c: Int): TriangleModifier {
        val triangle = DefaultModifier(a, b, c)
        triangles.add(triangle)
        return triangle
    }

    fun useNormals(normalType: NormalType): Assembler {
        triangles.forEach { it.useNormals(normalType) }
        return this
    }

    fun changeMaterial(material: Material): Assembler {
        triangles.forEach { it.changeMaterial(material) }
        return this
    }

    fun centerAndScale() {
        val cog = vertices.fold(Vector3.ZERO, { acc, cur -> acc + cur }) / vertices.size.toFloat()
        val min = vertices.fold(Vector3.ZERO, { acc, cur -> Vector3.min(acc, cur) })
        val max = vertices.fold(Vector3.ZERO, { acc, cur -> Vector3.max(acc, cur) })
        val size = max - min
        val resize = kotlin.math.max(size.x, kotlin.math.max(size.y, size.z))
        val newVertices = vertices.map { (it - cog) / resize }
        vertices.clear()
        vertices.addAll(newVertices)
    }

    fun compile(): Model {
        (0 until vertexNormals.size).forEach { vertexNormals[it] = Vector3.ZERO }

        for (triangle in triangles) {
            val a = vertices[triangle.b.v] - vertices[triangle.a.v]
            val b = vertices[triangle.c.v] - vertices[triangle.b.v]

            triangle.surfaceNormal = Vector3.normalize(Vector3.cross(a, b))

            vertexNormals[triangle.a.v] += triangle.surfaceNormal
            vertexNormals[triangle.b.v] += triangle.surfaceNormal
            vertexNormals[triangle.c.v] += triangle.surfaceNormal
        }

        (0 until vertexNormals.size).forEach { vertexNormals[it] = Vector3.normalize(vertexNormals[it]) }

        return Model(compileBuffers())
    }

    private fun compileBuffers(): Map<Material, Mesh> {
        val matBuckets = mutableMapOf<Material, MutableList<DefaultModifier>>()
        for (triangle in triangles) {
            matBuckets.getOrPut(triangle.material, { mutableListOf() }).add(triangle)
        }

        val meshByMaterial = mutableMapOf<Material, Mesh>()
        for (bucket in matBuckets) {
            val triangles = bucket.value
            val elementCount = triangles.size * 3
            val ib = IntArray(elementCount)
            val vb = arrayOfNulls<Vertex>(elementCount)
            var idx = 0
            for (triangle in triangles) {
                vb[idx] = compileTriangle(triangle, triangle.a)
                ib[idx] = idx++

                vb[idx] = compileTriangle(triangle, triangle.b)
                ib[idx] = idx++

                vb[idx] = compileTriangle(triangle, triangle.c)
                ib[idx] = idx++
            }

            meshByMaterial[bucket.key] = optimize(vb.requireNoNulls(), ib)
        }

        return meshByMaterial
    }

    private fun compileTriangle(triangleModifier: DefaultModifier, indexer: Indexer): Vertex {
        val normal = when (triangleModifier.normalType) {
            NormalType.AVERAGED -> vertexNormals[indexer.v]
            NormalType.VERTEX -> triangleNormals[indexer.vn]
            else -> triangleModifier.surfaceNormal
        }
        return Vertex(vertices[indexer.v], normal, if (indexer.vt < uvCoordinates.size) uvCoordinates[indexer.vt] else Vector3.ZERO)
    }

    private companion object {

        fun optimize(vb: Array<Vertex>, ib: IntArray): Mesh {
            val vertexMap = mutableMapOf<Vertex, Int>()
            val optTriangles = ib.indices.map { vertexMap.getOrPut(vb[ib[it]], { vertexMap.size }) }
            return Mesh(toVertexBuffer(vertexMap.keys.toTypedArray()), optTriangles.toIntArray())
        }

        fun toVertexBuffer(vertices: Array<Vertex>): FloatArray {
            var i = 0
            val vertexBuffer = FloatArray(vertices.size * 8)
            for (vertex in vertices) {
                vertexBuffer[i + 0] = vertex.position.x
                vertexBuffer[i + 1] = vertex.position.y
                vertexBuffer[i + 2] = vertex.position.z
                vertexBuffer[i + 3] = vertex.normal.x
                vertexBuffer[i + 4] = vertex.normal.y
                vertexBuffer[i + 5] = vertex.normal.z
                vertexBuffer[i + 6] = vertex.uv.x
                vertexBuffer[i + 7] = vertex.uv.y
                i += 8
            }
            return vertexBuffer
        }

        data class Vertex(val position: Vector3, val normal: Vector3, val uv: Vector3)

        data class Indexer(var v: Int = 0, var vn: Int = 0, var vt: Int = 0)

        class DefaultModifier(a: Int, b: Int, c: Int) : TriangleModifier {
            val a = Indexer().apply { v = a }
            val b = Indexer().apply { v = b }
            val c = Indexer().apply { v = c }

            var material: Material = Material.DEFAULT
            var normalType = NormalType.SURFACE
            var surfaceNormal = Vector3.ZERO

            override fun normals(n0: Int, n1: Int, n2: Int): TriangleModifier {
                a.vn = n0
                b.vn = n1
                c.vn = n2
                normalType = NormalType.VERTEX
                return this
            }

            override fun uvs(t0: Int, t1: Int, t2: Int): TriangleModifier {
                a.vt = t0
                b.vt = t1
                c.vt = t2
                return this
            }

            override fun useNormals(normalType: NormalType): TriangleModifier {
                this.normalType = normalType
                return this
            }

            override fun changeMaterial(material: Material): TriangleModifier {
                this.material = material
                return this
            }
        }
    }
}
