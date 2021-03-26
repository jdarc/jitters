package com.zynaps.tools.convexhull

import kotlin.math.*

@Suppress("DuplicatedCode")
class HullMaker {
    private val tris = mutableListOf<Tri?>()

    fun build(points: List<Point3D>, maxPoints: Int = 1024, normalEpsilon: Double = 0.001): HullResult {
        val vertexSource = Array(points.size.coerceAtLeast(8)) { Vec3() }
        val scale = Vec3()
        val outVertexCount = IntArray(1)
        val cloud = points.map { Vec3(it.x, it.y, it.z) }.toList()
        if (cleanupVertices(points.size, cloud, outVertexCount, vertexSource, normalEpsilon, scale)) {
            for (i in 0 until outVertexCount[0]) vertexSource[i].mul(scale)
            val hr = PHullResult()
            if (computeHull(outVertexCount[0], vertexSource, hr, maxPoints)) {
                val vertexScratch = Array(hr.vertexCount) { Vec3() }
                val vertexCount = bringOutYourDead(hr.vertices, hr.indices, vertexScratch)
                val hullPoints = (0 until vertexCount).map { Point3D(vertexScratch[it]) }
                val indices = (0 until hr.indexCount).map { hr.indices[it] }.toIntArray()
                return HullResult(hullPoints, indices, hr.faceCount, true)
            }
        }
        return HullResult(emptyList(), IntArray(0), 0, false)
    }

    private fun computeHull(numVertices: Int, vertices: Array<Vec3>, result: PHullResult, vertexLimit: Int): Boolean {
        val numTris = IntArray(1)
        if (calcHull(vertices, numVertices, result.indices, numTris, vertexLimit) == 0) return false
        result.indexCount = numTris[0] * 3
        result.faceCount = numTris[0]
        result.vertices = vertices
        result.vertexCount = numVertices
        return true
    }

    private fun calcHull(vertices: Array<Vec3>, numVertices: Int, trisOut: MutableList<Int>, numTris: IntArray, vLimit: Int): Int {
        val rc = calcHullGen(vertices, numVertices, vLimit)
        if (rc == 0) return 0
        val ts = mutableListOf<Int>()
        for (tri in tris) {
            if (tri != null) {
                for (j in 0..2) {
                    ts.add(tri[j])
                }
                tris[tri.id] = null
            }
        }
        numTris[0] = ts.size / 3
        trisOut.clear()
        for (i in ts.indices) trisOut.add(ts[i])
        tris.clear()
        return 1
    }

    private fun calcHullGen(vertices: Array<Vec3>, numVertices: Int, vLimit: Int): Int {
        var vlimit = vLimit
        if (numVertices < 4) return 0
        val tmp = Vec3()
        val tmp1 = Vec3()
        val tmp2 = Vec3()
        if (vlimit == 0) vlimit = 1000000000
        val bmin = Vec3(vertices[0])
        val bmax = Vec3(vertices[0])
        val isextreme = IntArray(numVertices)
        val allow = IntArray(numVertices)
        for (j in 0 until numVertices) {
            allow[j] = 1
            isextreme[j] = 0
            Vec3.setMin(bmin, vertices[j])
            Vec3.setMax(bmax, vertices[j])
        }
        tmp.sub(bmax, bmin)
        val epsilon = tmp.length() * 0.001

        val p = findSimplex(vertices, numVertices, allow, Int4())
        if (p.x == -1) return 0 // simplex failed

        val center = Vec3()
        Vec3.add(center, vertices[p[0]], vertices[p[1]], vertices[p[2]], vertices[p[3]])
        center.scale(0.25)
        Tri(p[2], p[3], p[1], tris.size).apply { n.set(2, 3, 1); tris.add(this) }
        Tri(p[3], p[2], p[0], tris.size).apply { n.set(3, 2, 0); tris.add(this) }
        Tri(p[0], p[1], p[3], tris.size).apply { n.set(0, 1, 3); tris.add(this) }
        Tri(p[1], p[0], p[2], tris.size).apply { n.set(1, 0, 2); tris.add(this) }
        isextreme[p[0]] = 1
        isextreme[p[1]] = 1
        isextreme[p[2]] = 1
        isextreme[p[3]] = 1
        val n = Vec3()
        for (t in tris) {
            triNormal(vertices[t!![0]], vertices[t[1]], vertices[t[2]], n)
            t.vmax = maxDirSterId(vertices, numVertices, n, allow)
            tmp.sub(vertices[t.vmax], vertices[t[0]])
            t.rise = n.dot(tmp)
        }
        var te: Tri? = null
        vlimit -= 4
        while (vlimit > 0 && extrudable(tris, epsilon).also { te = it } != null) {
            val v = te!!.vmax
            assert(v != -1)
            assert(isextreme[v] == 0)
            isextreme[v] = 1
            var j = tris.size
            while (j-- != 0) {
                if (tris[j] == null) continue
                val t = tris[j]!!
                if (above(vertices, t, vertices[v], 0.01 * epsilon)) extrude(tris, tris[j]!!, v)
            }
            j = tris.size
            while (j-- != 0) {
                if (tris[j] == null) continue
                if (!hasVert(tris[j]!!, v)) break
                val nt: Int3? = tris[j]
                tmp1.sub(vertices[nt!![1]], vertices[nt[0]])
                tmp2.sub(vertices[nt[2]], vertices[nt[1]])
                tmp.cross(tmp1, tmp2)
                if (above(vertices, nt, center, 0.01 * epsilon) || tmp.length() < epsilon * epsilon * 0.1) {
                    val nb = tris[tris[j]!!.n[0]]!!
                    assert(!hasVert(nb, v))
                    assert(nb.id < j)
                    extrude(tris, nb, v)
                    j = tris.size
                }
            }
            j = tris.size
            while (j-- != 0) {
                val t = tris[j] ?: continue
                if (t.vmax >= 0) break
                triNormal(vertices[t[0]], vertices[t[1]], vertices[t[2]], n)
                t.vmax = maxDirSterId(vertices, numVertices, n, allow)
                if (isextreme[t.vmax] != 0) t.vmax = -1 // already done that vertex - algorithm needs to be able to terminate.
                else {
                    tmp.sub(vertices[t.vmax], vertices[t[0]])
                    t.rise = n.dot(tmp)
                }
            }
            vlimit--
        }
        return 1
    }

    private fun cleanupVertices(
        svcount: Int,
        svertices: List<Vec3>,
        vcount: IntArray,
        vertices: Array<Vec3>,
        normalEpsilon: Double,
        scale: Vec3
    ): Boolean {
        if (svcount == 0) return false
        vcount[0] = 0
        scale.set(1.0, 1.0, 1.0)
        val bmin = doubleArrayOf(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
        val bmax = doubleArrayOf(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)
        for (p in svertices) {
            for (j in 0..2) {
                if (p[j] < bmin[j]) bmin[j] = p[j]
                if (p[j] > bmax[j]) bmax[j] = p[j]
            }
        }
        val recip = DoubleArray(3)
        var dx = bmax[0] - bmin[0]
        var dy = bmax[1] - bmin[1]
        var dz = bmax[2] - bmin[2]
        val center = Vec3(dx * 0.5 + bmin[0], dy * 0.5 + bmin[1], dz * 0.5 + bmin[2])
        if (dx < EPSILON || dy < EPSILON || dz < EPSILON || svcount < 3) {
            var len = Double.MAX_VALUE
            if (dx > EPSILON && dx < len) len = dx
            if (dy > EPSILON && dy < len) len = dy
            if (dz > EPSILON && dz < len) len = dz
            if (len == Double.MAX_VALUE) {
                dz = 0.01
                dy = dz
                dx = dy
            } else {
                if (dx < EPSILON) dx = len * 0.05
                if (dy < EPSILON) dy = len * 0.05
                if (dz < EPSILON) dz = len * 0.05
            }
            vertices[0].set(center.x - dx, center.y - dy, center.z - dz)
            vertices[1].set(center.x + dx, center.y - dy, center.z - dz)
            vertices[2].set(center.x + dx, center.y + dy, center.z - dz)
            vertices[3].set(center.x - dx, center.y + dy, center.z - dz)
            vertices[4].set(center.x - dx, center.y - dy, center.z + dz)
            vertices[5].set(center.x + dx, center.y - dy, center.z + dz)
            vertices[6].set(center.x + dx, center.y + dy, center.z + dz)
            vertices[7].set(center.x - dx, center.y + dy, center.z + dz)
            vcount[0] = 8
            return true
        } else {
            scale.x = dx
            scale.y = dy
            scale.z = dz
            recip[0] = 1 / dx
            recip[1] = 1 / dy
            recip[2] = 1 / dz
            center.x *= recip[0]
            center.y *= recip[1]
            center.z *= recip[2]
        }
        for (p in svertices) {
            val px = p.x * recip[0]
            val py = p.y * recip[1]
            val pz = p.z * recip[2]
            var j = 0
            while (j < vcount[0]) {
                val v = vertices[j]
                dx = abs(v.x - px)
                dy = abs(v.y - py)
                dz = abs(v.z - pz)
                if (dx < normalEpsilon && dy < normalEpsilon && dz < normalEpsilon) {
                    if ((px - center.x).pow(2) + (py - center.y).pow(2) + (pz - center.z).pow(2) >
                        (v.x - center.x).pow(2) + (v.y - center.y).pow(2) + (v.z - center.z).pow(2)
                    ) {
                        v.x = px
                        v.y = py
                        v.z = pz
                    }
                    break
                }
                j++
            }
            if (j == vcount[0]) {
                vertices[vcount[0]++].set(px, py, pz)
            }
        }

        dx = bmax[0] - bmin[0]
        dy = bmax[1] - bmin[1]
        dz = bmax[2] - bmin[2]
        if (dx < EPSILON || dy < EPSILON || dz < EPSILON || vcount[0] < 3) {
            val cx = dx * 0.5 + bmin[0]
            val cy = dy * 0.5 + bmin[1]
            val cz = dz * 0.5 + bmin[2]
            var len = Double.MAX_VALUE
            if (dx >= EPSILON && dx < len) len = dx
            if (dy >= EPSILON && dy < len) len = dy
            if (dz >= EPSILON && dz < len) len = dz
            if (len == Double.MAX_VALUE) {
                dz = 0.01
                dy = dz
                dx = dy
            } else {
                if (dx < EPSILON) dx = len * 0.05
                if (dy < EPSILON) dy = len * 0.05
                if (dz < EPSILON) dz = len * 0.05
            }
            vertices[0].set(cx - dx, cy - dy, cz - dz)
            vertices[1].set(cx + dx, cy - dy, cz - dz)
            vertices[2].set(cx + dx, cy + dy, cz - dz)
            vertices[3].set(cx - dx, cy + dy, cz - dz)
            vertices[4].set(cx - dx, cy - dy, cz + dz)
            vertices[5].set(cx + dx, cy - dy, cz + dz)
            vertices[6].set(cx + dx, cy + dy, cz + dz)
            vertices[7].set(cx - dx, cy + dy, cz + dz)
            vcount[0] = 8
            return true
        }
        return true
    }

    private companion object {
        const val RADS_PER_DEG = PI / 180.0
        const val EPSILON = 0.00000001

        fun hasVert(t: Int3, v: Int) = t[0] == v || t[1] == v || t[2] == v

        fun orthonormalize(v: Vec3, out: Vec3) = (if (v.y * v.y + v.x * v.x > v.z * v.z + v.x * v.x)
            out.set(v.y, -v.x, 0.0) else out.set(-v.z, 0.0, v.x)).normalize()

        fun maxDirFiltered(p: Array<Vec3>, count: Int, dir: Vec3, allow: IntArray): Int {
            var m = -1
            for (i in 0 until count) {
                if (allow[i] != 0 && (m == -1 || p[i].dot(dir) > p[m].dot(dir))) m = i
            }
            return m
        }

        fun above(vertices: Array<Vec3>, t: Int3, p: Vec3, epsilon: Double): Boolean {
            val b = vertices[t[0]]
            val triNormal = triNormal(vertices[t[0]], vertices[t[1]], vertices[t[2]], Vec3())
            return triNormal.x * (p.x - b.x) + triNormal.y * (p.y - b.y) + triNormal.z * (p.z - b.z) > epsilon
        }

        fun triNormal(v0: Vec3, v1: Vec3, v2: Vec3, out: Vec3): Vec3 {
            val x1 = v1.x - v0.x
            val y1 = v1.y - v0.y
            val z1 = v1.z - v0.z
            val x2 = v2.x - v1.x
            val y2 = v2.y - v1.y
            val z2 = v2.z - v1.z
            val x = y1 * z2 - z1 * y2
            val y = x2 * z1 - z2 * x1
            val z = x1 * y2 - y1 * x2
            val m = sqrt(x * x + y * y + z * z)
            return if (m == 0.0) out.set(1.0, 0.0, 0.0) else out.set(x, y, z).scale(1.0 / m)
        }

        fun bringOutYourDead(vertices: Array<Vec3>, indices: MutableList<Int>, outVertices: Array<Vec3>): Int {
            var count = 0
            val used = IntArray(vertices.size)
            for (i in 0 until indices.size) {
                val v = indices[i]
                if (used[v] != 0) {
                    indices[i] = used[v] - 1
                } else {
                    indices[i] = count
                    outVertices[count].set(vertices[v])
                    used[v] = ++count
                }
            }
            return count
        }

        fun removeB2b(tris: MutableList<Tri?>, s: Tri, t: Tri) {
            for (i in 0..2) {
                val a = s[(i + 1) % 3]
                val b = s[(i + 2) % 3]
                tris[s.neib(a, b).get()]?.neib(b, a)?.set(t.neib(b, a).get())
                tris[t.neib(b, a).get()]?.neib(a, b)?.set(s.neib(a, b).get())
            }
            tris[s.id] = null
            tris[t.id] = null
        }

        fun extrudable(tris: List<Tri?>, epsilon: Double): Tri? {
            var t: Tri? = null
            for (tri in tris) {
                if (t == null || tri != null && t.rise < tri.rise) {
                    t = tri
                }
            }
            return if (t?.rise ?: Double.NEGATIVE_INFINITY > epsilon) t else null
        }

        fun extrude(tris: MutableList<Tri?>, tri: Tri, v: Int) {
            val t = Int3(tri)
            val n = tris.size

            val ta = Tri(v, t[1], t[2], tris.size).apply {
                tris.add(this)
                this.n.set(tri.n[0], n + 1, n + 2)
                tris[tri.n[0]]?.neib(t[1], t[2])?.set(n + 0)
            }

            val tb = Tri(v, t[2], t[0], tris.size).apply {
                tris.add(this)
                this.n.set(tri.n[1], n + 2, n)
                tris[tri.n[1]]?.neib(t[2], t[0])?.set(n + 1)
            }

            val tc = Tri(v, t[0], t[1], tris.size).apply {
                tris.add(this)
                this.n.set(tri.n[2], n, n + 1)
                tris[tri.n[2]]?.neib(t[0], t[1])?.set(n + 2)
            }

            if (hasVert(tris[ta.n[0]]!!, v)) removeB2b(tris, ta, tris[ta.n[0]]!!)
            if (hasVert(tris[tb.n[0]]!!, v)) removeB2b(tris, tb, tris[tb.n[0]]!!)
            if (hasVert(tris[tc.n[0]]!!, v)) removeB2b(tris, tc, tris[tc.n[0]]!!)

            tris[tri.id] = null
        }

        private fun findSimplex(vertices: Array<Vec3>, numVertices: Int, allow: IntArray, out: Int4): Int4 {
            val tmp0 = Vec3()

            val basis = arrayOf(Vec3(0.01, 0.02, 1.0), Vec3(), Vec3())
            val p0 = maxDirSterId(vertices, numVertices, basis[0], allow)
            val p1 = maxDirSterId(vertices, numVertices, tmp0.negate(basis[0]), allow)
            basis[0].sub(vertices[p0], vertices[p1])
            if (p0 == p1 || basis[0].x == 0.0 && basis[0].y == 0.0 && basis[0].z == 0.0) return out.set(-1, -1, -1, -1)

            basis[1].cross(tmp0.set(1.0, 0.02, 0.0), basis[0])
            basis[2].cross(tmp0.set(-0.02, 1.0, 0.0), basis[0])
            if (basis[1].length() > basis[2].length()) basis[1].normalize() else {
                basis[1].set(basis[2]).normalize()
            }
            var p2 = maxDirSterId(vertices, numVertices, basis[1], allow)
            if (p2 == p0 || p2 == p1) p2 = maxDirSterId(vertices, numVertices, tmp0.negate(basis[1]), allow)
            if (p2 == p0 || p2 == p1) return out.set(-1, -1, -1, -1)

            basis[1].sub(vertices[p2], vertices[p0])
            basis[2].cross(basis[1], basis[0]).normalize()
            var p3 = maxDirSterId(vertices, numVertices, basis[2], allow)
            if (p3 == p0 || p3 == p1 || p3 == p2) p3 = maxDirSterId(vertices, numVertices, tmp0.negate(basis[2]), allow)
            if (p3 == p0 || p3 == p1 || p3 == p2) return out.set(-1, -1, -1, -1)

            val tmp1 = Vec3().sub(vertices[p1], vertices[p0])
            if (tmp1.sub(vertices[p3], vertices[p0]).dot(tmp0.cross(tmp1, tmp0.sub(vertices[p2], vertices[p0]))) < 0) {
                out.set(p0, p1, p3, p2)
            } else {
                out.set(p0, p1, p2, p3)
            }
            return out
        }

        fun maxDirSterId(p: Array<Vec3>, count: Int, dir: Vec3, allow: IntArray): Int {
            val tmp0 = Vec3()
            val tmp1 = Vec3()
            val tmp2 = Vec3()
            val u = Vec3()
            val v = Vec3()
            while (true) {
                val m = maxDirFiltered(p, count, dir, allow)
                if (allow[m] == 3) return m
                orthonormalize(dir, u)
                v.cross(u, dir)
                var ma = -1
                var x = 0.0
                while (x <= 360.0) {
                    var s = sin(RADS_PER_DEG * x)
                    var c = cos(RADS_PER_DEG * x)
                    tmp0.add(tmp1.scale(s, u), tmp2.scale(c, v)).scale(0.025).add(dir)
                    val mb = maxDirFiltered(p, count, tmp0, allow)
                    if (ma == m && mb == m) {
                        allow[m] = 3
                        return m
                    }
                    if (ma != -1 && ma != mb) {
                        var mc = ma
                        var xx = x - 40.0
                        while (xx <= x) {
                            s = sin(RADS_PER_DEG * xx)
                            c = cos(RADS_PER_DEG * xx)
                            tmp0.add(tmp1.scale(s, u), tmp2.scale(c, v)).scale(0.025).add(dir)
                            val md = maxDirFiltered(p, count, tmp0, allow)
                            if (mc == m && md == m) {
                                allow[m] = 3
                                return m
                            }
                            mc = md
                            xx += 5.0
                        }
                    }
                    ma = mb
                    x += 45.0
                }
                allow[m] = 0
            }
        }
    }
}
