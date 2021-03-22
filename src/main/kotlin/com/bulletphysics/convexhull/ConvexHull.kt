package com.bulletphysics.convexhull

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Suppress("DuplicatedCode")
class ConvexHull {
    private val vertexIndexMapping = mutableListOf<Int>()
    private val tris = mutableListOf<Tri?>()

    fun build(points: List<Point3D>, maxPoints: Int = 4096, normalEpsilon: Float = 0.001f): HullResult {
        val vcount = points.size.coerceAtLeast(8)
        val vertexSource = List(vcount) { Vec3() }.toMutableList()
        val scale = Vec3()
        val outVertexCount = IntArray(1)
        val cloud = points.map { Vec3(it.x, it.y, it.z) }.toList()
        if (cleanupVertices(points.size, cloud, outVertexCount, vertexSource, normalEpsilon, scale)) {
            for (i in 0 until outVertexCount[0]) {
                val v = vertexSource[i]
                v.x = v.x * scale.x
                v.y = v.y * scale.y
                v.z = v.z * scale.z
            }
            val hr = PHullResult()
            if (computeHull(outVertexCount[0], vertexSource, hr, maxPoints)) {
                // re-index triangle mesh so it refers to only used vertices, rebuild a new vertex table.
                val vertexScratch = List(hr.vertexCount) { Vec3() }
                bringOutYourDead(hr.vertices, hr.vertexCount, vertexScratch, outVertexCount, hr.indices, hr.indexCount)
                val hullPoints = (0 until outVertexCount[0]).map { Point3D(vertexScratch[it]) }
                val indices = (0 until hr.indexCount).map { hr.indices[it] }.toIntArray()
                return HullResult(hullPoints, indices, hr.faceCount, true)
            }
        }
        return HullResult(emptyList(), IntArray(0), 0, false)
    }

    private fun computeHull(numVertices: Int, vertices: MutableList<Vec3>, result: PHullResult, vertexLimit: Int): Boolean {
        val numTris = IntArray(1)
        val ret = calcHull(vertices, numVertices, result.indices, numTris, vertexLimit)
        if (ret == 0) return false
        result.indexCount = numTris[0] * 3
        result.faceCount = numTris[0]
        result.vertices = vertices
        result.vertexCount = numVertices
        return true
    }

    private fun allocateTriangle(a: Int, b: Int, c: Int): Tri {
        val tr = Tri(a, b, c)
        tr.id = tris.size
        tris.add(tr)
        return tr
    }

    private fun deAllocateTriangle(tri: Tri) {
        assert(tris[tri.id] == tri)
        tris[tri.id] = null
    }

    private fun removeB2b(s: Tri, t: Tri) {
        for (i in 0..2) {
            val a = s[(i + 1) % 3]
            val b = s[(i + 2) % 3]
            assert(tris[s.neib(a, b).get()]!!.neib(b, a).get() == s.id)
            assert(tris[t.neib(a, b).get()]!!.neib(b, a).get() == t.id)
            tris[s.neib(a, b).get()]?.neib(b, a)?.set(t.neib(b, a).get())
            tris[t.neib(b, a).get()]?.neib(a, b)?.set(s.neib(a, b).get())
        }
        deAllocateTriangle(s)
        deAllocateTriangle(t)
    }

    private fun checkIt(t: Tri) {
        assert(tris[t.id] == t)
        for (i in 0..2) {
            val i1 = (i + 1) % 3
            val i2 = (i + 2) % 3
            val a = t[i1]
            val b = t[i2]
            assert(a != b)
            assert(tris[t.n[i]]!!.neib(b, a).get() == t.id)
        }
    }

    private fun extrudable(epsilon: Float): Tri? {
        var t: Tri? = null
        for (tri in tris) {
            if (t == null || tri != null && t.rise < tri.rise) {
                t = tri
            }
        }
        return if (t?.rise ?: Float.NEGATIVE_INFINITY > epsilon) t else null
    }

    private fun calcHull(vertices: MutableList<Vec3>, numVertices: Int, trisOut: MutableList<Int>, numTris: IntArray, vLimit: Int): Int {
        val rc = calcHullGen(vertices, numVertices, vLimit)
        if (rc == 0) return 0
        val ts = mutableListOf<Int>()
        for (tri in tris) if (tri != null) {
            for (j in 0..2) ts.add(tri[j])
            deAllocateTriangle(tri)
        }
        numTris[0] = ts.size / 3
        trisOut.clear()
        for (i in ts.indices) trisOut.add(ts[i])
        tris.clear()
        return 1
    }

    private fun calcHullGen(vertices: List<Vec3>, numVertices: Int, vLimit: Int): Int {
        var vlimit = vLimit
        if (numVertices < 4) return 0
        val tmp = Vec3()
        val tmp1 = Vec3()
        val tmp2 = Vec3()
        if (vlimit == 0) vlimit = 1000000000
        val bmin = Vec3(vertices[0])
        val bmax = Vec3(vertices[0])
        val isextreme = mutableListOf<Int>()
        val allow = mutableListOf<Int>()
        for (j in 0 until numVertices) {
            allow.add(1)
            isextreme.add(0)
            Vec3.setMin(bmin, vertices[j])
            Vec3.setMax(bmax, vertices[j])
        }
        tmp.sub(bmax, bmin)
        val epsilon = tmp.length() * 0.001f
        assert(epsilon != 0f)
        val p = findSimplex(vertices, numVertices, allow, Int4())
        // a valid interior point
        if (p.x == -1) return 0 // simplex failed
        val center = Vec3()
        Vec3.add(center, vertices[p[0]], vertices[p[1]], vertices[p[2]], vertices[p[3]])
        center.scale(0.25f)
        val t0 = allocateTriangle(p[2], p[3], p[1])
        t0.n.set(2, 3, 1)
        val t1 = allocateTriangle(p[3], p[2], p[0])
        t1.n.set(3, 2, 0)
        val t2 = allocateTriangle(p[0], p[1], p[3])
        t2.n.set(0, 1, 3)
        val t3 = allocateTriangle(p[1], p[0], p[2])
        t3.n.set(1, 0, 2)
        isextreme[p[0]] = 1
        isextreme[p[1]] = 1
        isextreme[p[2]] = 1
        isextreme[p[3]] = 1
        checkIt(t0)
        checkIt(t1)
        checkIt(t2)
        checkIt(t3)
        val n = Vec3()
        for (t in tris) {
            assert(t != null)
            assert(t!!.vmax < 0)
            triNormal(vertices[t[0]], vertices[t[1]], vertices[t[2]], n)
            t.vmax = maxDirSterId(vertices, numVertices, n, allow)
            tmp.sub(vertices[t.vmax], vertices[t[0]])
            t.rise = n.dot(tmp)
        }
        var te: Tri? = null
        vlimit -= 4
        while (vlimit > 0 && extrudable(epsilon).also { te = it } != null) {
            val v = te!!.vmax
            assert(v != -1)
            assert(isextreme[v] == 0)
            isextreme[v] = 1
            var j = tris.size
            while (j-- != 0) {
                if (tris[j] == null) continue
                val t = tris[j]!!
                if (above(vertices, t, vertices[v], 0.01f * epsilon)) extrude(tris[j]!!, v)
            }
            j = tris.size
            while (j-- != 0) {
                if (tris[j] == null) continue
                if (!hasVert(tris[j]!!, v)) break
                val nt: Int3? = tris[j]
                tmp1.sub(vertices[nt!![1]], vertices[nt[0]])
                tmp2.sub(vertices[nt[2]], vertices[nt[1]])
                tmp.cross(tmp1, tmp2)
                if (above(vertices, nt, center, 0.01f * epsilon) || tmp.length() < epsilon * epsilon * 0.1f) {
                    val nb = tris[tris[j]!!.n[0]]!!
                    assert(!hasVert(nb, v))
                    assert(nb.id < j)
                    extrude(nb, v)
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

    private fun findSimplex(vertices: List<Vec3>, numVertices: Int, allow: MutableList<Int>, out: Int4): Int4 {
        val tmp = Vec3()
        val tmp1 = Vec3()
        val tmp2 = Vec3()
        val basis = arrayOf(Vec3(), Vec3(), Vec3())
        basis[0].set(0.01f, 0.02f, 1.0f)
        val p0 = maxDirSterId(vertices, numVertices, basis[0], allow)
        tmp.negate(basis[0])
        val p1 = maxDirSterId(vertices, numVertices, tmp, allow)
        basis[0].sub(vertices[p0], vertices[p1])
        if (p0 == p1 || basis[0].x == 0f && basis[0].y == 0f && basis[0].z == 0f) {
            out.set(-1, -1, -1, -1)
            return out
        }
        basis[1].cross(tmp.set(1f, 0.02f, 0f), basis[0])
        basis[2].cross(tmp.set(-0.02f, 1f, 0f), basis[0])
        if (basis[1].length() > basis[2].length()) basis[1].normalize() else {
            basis[1].set(basis[2]).normalize()
        }
        var p2 = maxDirSterId(vertices, numVertices, basis[1], allow)
        if (p2 == p0 || p2 == p1) {
            p2 = maxDirSterId(vertices, numVertices, tmp.negate(basis[1]), allow)
        }
        if (p2 == p0 || p2 == p1) {
            out.set(-1, -1, -1, -1)
            return out
        }
        basis[1].sub(vertices[p2], vertices[p0])
        basis[2].cross(basis[1], basis[0]).normalize()
        var p3 = maxDirSterId(vertices, numVertices, basis[2], allow)
        if (p3 == p0 || p3 == p1 || p3 == p2) {
            p3 = maxDirSterId(vertices, numVertices, tmp.negate(basis[2]), allow)
        }
        if (p3 == p0 || p3 == p1 || p3 == p2) {
            out.set(-1, -1, -1, -1)
            return out
        }
        tmp1.sub(vertices[p1], vertices[p0])
        tmp2.sub(vertices[p2], vertices[p0]).cross(tmp1, tmp2)
        tmp1.sub(vertices[p3], vertices[p0])
        if (tmp1.dot(tmp2) < 0) {
            val swap = p2
            p2 = p3
            p3 = swap
        }
        out.set(p0, p1, p2, p3)
        return out
    }

    private fun extrude(t0: Tri, v: Int) {
        val t = Int3(t0)
        val n = tris.size

        val ta = allocateTriangle(v, t[1], t[2])
        ta.n.set(t0.n[0], n + 1, n + 2)
        tris[t0.n[0]]?.neib(t[1], t[2])?.set(n)

        val tb = allocateTriangle(v, t[2], t[0])
        tb.n.set(t0.n[1], n + 2, n)
        tris[t0.n[1]]?.neib(t[2], t[0])?.set(n + 1)

        val tc = allocateTriangle(v, t[0], t[1])
        tc.n.set(t0.n[2], n, n + 1)
        tris[t0.n[2]]?.neib(t[0], t[1])?.set(n + 2)

        checkIt(ta)
        checkIt(tb)
        checkIt(tc)
        if (hasVert(tris[ta.n[0]]!!, v)) removeB2b(ta, tris[ta.n[0]]!!)
        if (hasVert(tris[tb.n[0]]!!, v)) removeB2b(tb, tris[tb.n[0]]!!)
        if (hasVert(tris[tc.n[0]]!!, v)) removeB2b(tc, tris[tc.n[0]]!!)

        deAllocateTriangle(t0)
    }

    private fun bringOutYourDead(verts: List<Vec3>, vcount: Int, overts: List<Vec3>, ocount: IntArray, indices: MutableList<Int>, indexCount: Int) {
        val tmpIndices = mutableListOf<Int>()
        for (i in 0 until vertexIndexMapping.size) tmpIndices.add(vertexIndexMapping.size)
        val usedIndices = Array(vcount) { 0 }
        ocount[0] = 0
        for (i in 0 until indexCount) {
            val v = indices[i] // original array index
            assert(v in 0 until vcount)
            if (usedIndices[v] != 0) indices[i] = usedIndices[v] - 1 // index to new array
            else {
                indices[i] = ocount[0] // new index mapping
                overts[ocount[0]].set(verts[v]) // copy old vert to new vert array
                for (k in 0 until vertexIndexMapping.size) if (tmpIndices[k] == v) vertexIndexMapping[k] = ocount[0]
                ocount[0]++ // increment output vert count
                assert(ocount[0] in 0..vcount)
                usedIndices[v] = ocount[0] // assign new index remapping
            }
        }
    }

    private fun cleanupVertices(svcount: Int, svertices: List<Vec3>, vcount: IntArray, vertices: List<Vec3>, normalEpsilon: Float, scale: Vec3): Boolean {
        if (svcount == 0) return false
        vertexIndexMapping.clear()
        vcount[0] = 0
        val recip = FloatArray(3)
        scale.set(1f, 1f, 1f)
        var bmin = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        var bmax = floatArrayOf(-3.4028235E38f, -3.4028235E38f, -3.4028235E38f)
        var vtxPtr = svertices
        var vtxIdx = 0
        for (i in 0 until svcount) {
            val p = vtxPtr[vtxIdx++]
            for (j in 0..2) {
                if (p[j] < bmin[j]) bmin[j] = p[j]
                if (p[j] > bmax[j]) bmax[j] = p[j]
            }
        }
        var dx = bmax[0] - bmin[0]
        var dy = bmax[1] - bmin[1]
        var dz = bmax[2] - bmin[2]
        val center = Vec3(dx * 0.5f + bmin[0], dy * 0.5f + bmin[1], dz * 0.5f + bmin[2])
        if (dx < EPSILON || dy < EPSILON || dz < EPSILON || svcount < 3) {
            var len = Float.MAX_VALUE
            if (dx > EPSILON && dx < len) len = dx
            if (dy > EPSILON && dy < len) len = dy
            if (dz > EPSILON && dz < len) len = dz
            if (len == Float.MAX_VALUE) {
                dz = 0.01f
                dy = dz
                dx = dy
            } else {
                if (dx < EPSILON) dx = len * 0.05f
                if (dy < EPSILON) dy = len * 0.05f
                if (dz < EPSILON) dz = len * 0.05f
            }
            val x1 = center.x - dx
            val x2 = center.x + dx
            val y1 = center.y - dy
            val y2 = center.y + dy
            val z1 = center.z - dz
            val z2 = center.z + dz
            addPoint(vcount, vertices, x1, y1, z1)
            addPoint(vcount, vertices, x2, y1, z1)
            addPoint(vcount, vertices, x2, y2, z1)
            addPoint(vcount, vertices, x1, y2, z1)
            addPoint(vcount, vertices, x1, y1, z2)
            addPoint(vcount, vertices, x2, y1, z2)
            addPoint(vcount, vertices, x2, y2, z2)
            addPoint(vcount, vertices, x1, y2, z2)
            return true
        } else {
            scale.x = dx
            scale.y = dy
            scale.z = dz
            recip[0] = 1f / dx
            recip[1] = 1f / dy
            recip[2] = 1f / dz
            center.x *= recip[0]
            center.y *= recip[1]
            center.z *= recip[2]
        }
        vtxPtr = svertices
        vtxIdx = 0
        for (i in 0 until svcount) {
            val p = vtxPtr[vtxIdx++]
            var px = p.x
            var py = p.y
            var pz = p.z
            px *= recip[0]
            py *= recip[1]
            pz *= recip[2]

            var j = 0
            while (j < vcount[0]) {
                val v = vertices[j]
                dx = abs(v.x - px)
                dy = abs(v.y - py)
                dz = abs(v.z - pz)
                if (dx < normalEpsilon && dy < normalEpsilon && dz < normalEpsilon) {
                    val dist1 = getDist(px, py, pz, center)
                    val dist2 = getDist(v.x, v.y, v.z, center)
                    if (dist1 > dist2) {
                        v.x = px
                        v.y = py
                        v.z = pz
                    }
                    break
                }
                j++
            }
            if (j == vcount[0]) {
                val dest = vertices[vcount[0]]
                dest.x = px
                dest.y = py
                dest.z = pz
                vcount[0]++
            }
            vertexIndexMapping.add(j)
        }

        bmin = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
        bmax = floatArrayOf(-3.4028235E38f, -3.4028235E38f, -3.4028235E38f)
        for (i in 0 until vcount[0]) {
            val p = vertices[i]
            for (j in 0..2) {
                if (p[j] < bmin[j]) bmin[j] = p[j]
                if (p[j] > bmax[j]) bmax[j] = p[j]
            }
        }
        dx = bmax[0] - bmin[0]
        dy = bmax[1] - bmin[1]
        dz = bmax[2] - bmin[2]
        if (dx < EPSILON || dy < EPSILON || dz < EPSILON || vcount[0] < 3) {
            val cx = dx * 0.5f + bmin[0]
            val cy = dy * 0.5f + bmin[1]
            val cz = dz * 0.5f + bmin[2]
            var len = Float.MAX_VALUE
            if (dx >= EPSILON && dx < len) len = dx
            if (dy >= EPSILON && dy < len) len = dy
            if (dz >= EPSILON && dz < len) len = dz
            if (len == Float.MAX_VALUE) {
                dz = 0.01f
                dy = dz
                dx = dy
            } else {
                if (dx < EPSILON) dx = len * 0.05f
                if (dy < EPSILON) dy = len * 0.05f
                if (dz < EPSILON) dz = len * 0.05f
            }
            val x1 = cx - dx
            val x2 = cx + dx
            val y1 = cy - dy
            val y2 = cy + dy
            val z1 = cz - dz
            val z2 = cz + dz
            vcount[0] = 0
            addPoint(vcount, vertices, x1, y1, z1)
            addPoint(vcount, vertices, x2, y1, z1)
            addPoint(vcount, vertices, x2, y2, z1)
            addPoint(vcount, vertices, x1, y2, z1)
            addPoint(vcount, vertices, x1, y1, z2)
            addPoint(vcount, vertices, x2, y1, z2)
            addPoint(vcount, vertices, x2, y2, z2)
            addPoint(vcount, vertices, x1, y2, z2)
            return true
        }
        return true
    }

    companion object {
        private const val SIMD_RADS_PER_DEG = 0.01745329252f
        private const val EPSILON = 0.000001f

        private fun hasVert(t: Int3, v: Int) = t[0] == v || t[1] == v || t[2] == v

        private fun orthonormalize(v: Vec3, out: Vec3): Vec3 {
            val a = Vec3().cross(v, Vec3(0f, 0f, 1f))
            val b = Vec3().cross(v, Vec3(0f, 1f, 0f))
            return if (a.length() > b.length()) {
                out.normalize(a)
                out
            } else {
                out.normalize(b)
                out
            }
        }

        private fun maxDirFiltered(p: List<Vec3>, count: Int, dir: Vec3, allow: List<Int>): Int {
            assert(count != 0)
            var m = -1
            for (i in 0 until count) if (allow[i] != 0) {
                if (m == -1 || p[i].dot(dir) > p[m].dot(dir)) m = i
            }
            assert(m != -1)
            return m
        }

        private fun maxDirSterId(p: List<Vec3>, count: Int, dir: Vec3, allow: MutableList<Int>): Int {
            val tmp = Vec3()
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
                var x = 0f
                while (x <= 360f) {
                    var s = sin(SIMD_RADS_PER_DEG * x)
                    var c = cos(SIMD_RADS_PER_DEG * x)
                    tmp.add(tmp1.scale(s, u), tmp2.scale(c, v)).scale(0.025f).add(dir)
                    val mb = maxDirFiltered(p, count, tmp, allow)
                    if (ma == m && mb == m) {
                        allow[m] = 3
                        return m
                    }
                    if (ma != -1 && ma != mb) {
                        var mc = ma
                        var xx = x - 40f
                        while (xx <= x) {
                            s = sin(SIMD_RADS_PER_DEG * xx)
                            c = cos(SIMD_RADS_PER_DEG * xx)
                            tmp.add(tmp1.scale(s, u), tmp2.scale(c, v)).scale(0.025f).add(dir)
                            val md = maxDirFiltered(p, count, tmp, allow)
                            if (mc == m && md == m) {
                                allow[m] = 3
                                return m
                            }
                            mc = md
                            xx += 5f
                        }
                    }
                    ma = mb
                    x += 45f
                }
                allow[m] = 0
            }
        }

        private fun triNormal(v0: Vec3, v1: Vec3, v2: Vec3, out: Vec3): Vec3 {
            val tmp1 = Vec3().sub(v1, v0)
            val tmp2 = Vec3().sub(v2, v1)
            val cp = Vec3().cross(tmp1, tmp2)
            val m = cp.length()
            return if (m == 0f) out.set(1f, 0f, 0f) else out.scale(1f / m, cp)
        }

        private fun above(vertices: List<Vec3>, t: Int3, p: Vec3, epsilon: Float): Boolean {
            val n = triNormal(vertices[t[0]], vertices[t[1]], vertices[t[2]], Vec3())
            return n.dot(Vec3().sub(p, vertices[t[0]])) > epsilon
        }

        private fun addPoint(vcount: IntArray, p: List<Vec3>, x: Float, y: Float, z: Float) {
            val dest = p[vcount[0]++]
            dest.x = x
            dest.y = y
            dest.z = z
        }

        private fun getDist(px: Float, py: Float, pz: Float, p2: Vec3): Float {
            val dx = px - p2.x
            val dy = py - p2.y
            val dz = pz - p2.z
            return dx * dx + dy * dy + dz * dz
        }
    }
}
