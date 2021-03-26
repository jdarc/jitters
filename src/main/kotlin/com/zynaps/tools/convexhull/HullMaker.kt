package com.zynaps.tools.convexhull

import kotlin.math.*

@Suppress("DuplicatedCode")
class HullMaker {

    fun build(points: Array<Point3D>, maxPoints: Int = 64, normalEpsilon: Double = 0.001): HullResult {
        if (points.isNotEmpty()) {
            val scale = Vec3()
            val pointCloud = points.map { Vec3(it.x, it.y, it.z) }.toTypedArray()
            val vertexSource = Array(points.size.coerceAtLeast(8)) { Vec3() }
            val numVertices = cleanupVertices(pointCloud, vertexSource, normalEpsilon, scale)
            if (numVertices > 0) {
                for (i in 0 until numVertices) vertexSource[i].scale(scale)
                val indices = calcHull(vertexSource, numVertices, maxPoints)
                if (indices.isNotEmpty()) {
                    return HullResult(bringOutYourDead(vertexSource, indices).map { Point3D(it) }, indices, true)
                }
            }
        }
        return HullResult(emptyList(), IntArray(0), false)
    }

    private fun cleanupVertices(srcPoints: Array<Vec3>, dstPoints: Array<Vec3>, normalEpsilon: Double, outScale: Vec3): Int {
        val bmin = Vec3(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
        val bmax = Vec3(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)
        for (p in srcPoints) {
            if (p.x < bmin.x) bmin.x = p.x
            if (p.y < bmin.y) bmin.y = p.y
            if (p.z < bmin.z) bmin.z = p.z
            if (p.x > bmax.x) bmax.x = p.x
            if (p.y > bmax.y) bmax.y = p.y
            if (p.z > bmax.z) bmax.z = p.z
        }
        val dx = bmax.x - bmin.x
        val dy = bmax.y - bmin.y
        val dz = bmax.z - bmin.z
        val center = Vec3(bmin.x + dx * 0.5, bmin.y + dy * 0.5, bmin.z + dz * 0.5)

        outScale.set(dx, dy, dz)
        if (dx < EPSILON || dy < EPSILON || dz < EPSILON || srcPoints.size < 3) {
            var len = Double.MAX_VALUE
            if (dx > EPSILON && dx < len) len = dx
            if (dy > EPSILON && dy < len) len = dy
            if (dz > EPSILON && dz < len) len = dz
            if (len == Double.MAX_VALUE) {
                outScale.set(dy, dz, 0.01)
            } else {
                if (dx < EPSILON) outScale.x = len * 0.05
                if (dy < EPSILON) outScale.y = len * 0.05
                if (dz < EPSILON) outScale.z = len * 0.05
            }
            dstPoints[0].set(center.x - outScale.x, center.y - outScale.y, center.z - outScale.z)
            dstPoints[1].set(center.x + outScale.x, center.y - outScale.y, center.z - outScale.z)
            dstPoints[2].set(center.x + outScale.x, center.y + outScale.y, center.z - outScale.z)
            dstPoints[3].set(center.x - outScale.x, center.y + outScale.y, center.z - outScale.z)
            dstPoints[4].set(center.x - outScale.x, center.y - outScale.y, center.z + outScale.z)
            dstPoints[5].set(center.x + outScale.x, center.y - outScale.y, center.z + outScale.z)
            dstPoints[6].set(center.x + outScale.x, center.y + outScale.y, center.z + outScale.z)
            dstPoints[7].set(center.x - outScale.x, center.y + outScale.y, center.z + outScale.z)
            return 8
        }

        center.x /= dx
        center.y /= dy
        center.z /= dz

        var total = 0
        for (p in srcPoints) {
            val px = p.x / dx
            val py = p.y / dy
            val pz = p.z / dz
            val ax = px - center.x
            val ay = py - center.y
            val az = pz - center.z
            val aLen = ax * ax + ay * ay + az * az
            var j = 0
            while (j < total) {
                val v = dstPoints[j++]
                if (abs(v.x - px) < normalEpsilon && abs(v.y - py) < normalEpsilon && abs(v.z - pz) < normalEpsilon) {
                    val bx = v.x - center.x
                    val by = v.y - center.y
                    val bz = v.z - center.z
                    if (aLen > bx * bx + by * by + bz * bz) {
                        v.x = px
                        v.y = py
                        v.z = pz
                    }
                    break
                }
            }
            if (j == total) {
                dstPoints[total++].set(px, py, pz)
            }
        }

        if (dx < EPSILON || dy < EPSILON || dz < EPSILON || total < 3) {
            val cx = bmin.x + dx * 0.5
            val cy = bmin.y + dy * 0.5
            val cz = bmin.z + dz * 0.5
            var len = Double.MAX_VALUE
            if (dx >= EPSILON && dx < len) len = dx
            if (dy >= EPSILON && dy < len) len = dy
            if (dz >= EPSILON && dz < len) len = dz
            var lx = dy
            var ly = dz
            var lz = 0.01
            if (len != Double.MAX_VALUE) {
                if (dx < EPSILON) lx = len * 0.05
                if (dy < EPSILON) ly = len * 0.05
                if (dz < EPSILON) lz = len * 0.05
            }
            dstPoints[0].set(cx - lx, cy - ly, cz - lz)
            dstPoints[1].set(cx + lx, cy - ly, cz - lz)
            dstPoints[2].set(cx + lx, cy + ly, cz - lz)
            dstPoints[3].set(cx - lx, cy + ly, cz - lz)
            dstPoints[4].set(cx - lx, cy - ly, cz + lz)
            dstPoints[5].set(cx + lx, cy - ly, cz + lz)
            dstPoints[6].set(cx + lx, cy + ly, cz + lz)
            dstPoints[7].set(cx - lx, cy + ly, cz + lz)
            return 8
        }
        return total
    }

    private fun calcHull(vertices: Array<Vec3>, numVertices: Int, vLimit: Int): IntArray {
        val tris = calcHullGen(vertices, numVertices, vLimit)
        if (tris.isEmpty()) return IntArray(0)
        val actualTris = tris.filterNotNull()
        val ts = IntArray(actualTris.size * 3)
        actualTris.forEachIndexed { index, tri ->
            ts[index * 3 + 0] = tri[0]
            ts[index * 3 + 1] = tri[1]
            ts[index * 3 + 2] = tri[2]
            tris[tri.id] = null
        }
        return ts
    }

    private fun calcHullGen(vertices: Array<Vec3>, numVertices: Int, vertexLimit: Int): Array<Tri?> {
        if (numVertices < 4) return emptyArray()
        val allow = IntArray(numVertices) { 1 }

        val p = findSimplex(vertices, numVertices, allow, Int4())
        if (p.x == -1) return emptyArray() // simplex failed

        val isExtreme = IntArray(numVertices) { 0 }
        isExtreme[p[0]] = 1
        isExtreme[p[1]] = 1
        isExtreme[p[2]] = 1
        isExtreme[p[3]] = 1

        val bmin = Vec3(vertices[0])
        val bmax = Vec3(vertices[0])
        for (j in 1 until numVertices) {
            val b = vertices[j]
            if (b.x < bmin.x) bmin.x = b.x
            if (b.y < bmin.y) bmin.y = b.y
            if (b.z < bmin.z) bmin.z = b.z
            if (b.x > bmax.x) bmax.x = b.x
            if (b.y > bmax.y) bmax.y = b.y
            if (b.z > bmax.z) bmax.z = b.z
        }

        val v1 = vertices[p[0]]
        val v2 = vertices[p[1]]
        val v3 = vertices[p[2]]
        val v4 = vertices[p[3]]
        val vx = (v1.x + v2.x + v3.x + v4.x) * 0.25
        val vy = (v1.y + v2.y + v3.y + v4.y) * 0.25
        val vz = (v1.z + v2.z + v3.z + v4.z) * 0.25
        val center = Vec3(vx, vy, vz)

        val tmp2 = Vec3()
        val tmp1 = Vec3()
        val tmp0 = Vec3(bmax).sub(bmin)
        val epsilon = tmp0.length() * 0.001

        val t0 = Tri(p[2], p[3], p[1], 0).with(2, 3, 1)
        val t1 = Tri(p[3], p[2], p[0], 1).with(3, 2, 0)
        val t2 = Tri(p[0], p[1], p[3], 2).with(0, 1, 3)
        val t3 = Tri(p[1], p[0], p[2], 3).with(1, 0, 2)

        triNormal(vertices[t0[0]], vertices[t0[1]], vertices[t0[2]], tmp1)
        t0.vmax = maxDirSterId(vertices, numVertices, tmp1, allow)
        t0.rise = tmp1.dot(tmp0.set(vertices[t0.vmax]).sub(vertices[t0[0]]))

        triNormal(vertices[t1[0]], vertices[t1[1]], vertices[t1[2]], tmp1)
        t1.vmax = maxDirSterId(vertices, numVertices, tmp1, allow)
        t1.rise = tmp1.dot(tmp0.set(vertices[t1.vmax]).sub(vertices[t1[0]]))

        triNormal(vertices[t2[0]], vertices[t2[1]], vertices[t2[2]], tmp1)
        t2.vmax = maxDirSterId(vertices, numVertices, tmp1, allow)
        t2.rise = tmp1.dot(tmp0.set(vertices[t2.vmax]).sub(vertices[t2[0]]))

        triNormal(vertices[t3[0]], vertices[t3[1]], vertices[t3[2]], tmp1)
        t3.vmax = maxDirSterId(vertices, numVertices, tmp1, allow)
        t3.rise = tmp1.dot(tmp0.set(vertices[t3.vmax]).sub(vertices[t3[0]]))

        val tris = mutableListOf<Tri?>(t0, t1, t2, t3)
        var limit = (if (vertexLimit == 0) Int.MAX_VALUE else vertexLimit) - 4
        while (limit-- > 0) {
            val te = extrudable(tris, epsilon) ?: continue
            isExtreme[te.vmax] = 1

            for (j in tris.size - 1 downTo 0) {
                val tri = tris[j] ?: continue
                if (above(vertices, tri, vertices[te.vmax], epsilon * 0.01)) {
                    extrude(tris, tri, te.vmax)
                }
            }

            var j = tris.size
            while (j-- != 0) {
                val nt = tris[j] ?: continue
                if (!hasVert(nt, te.vmax)) break
                if (above(vertices, nt, center, epsilon * 0.01) || tmp0.cross(
                        tmp1.set(vertices[nt[1]]).sub(vertices[nt[0]]),
                        tmp2.set(vertices[nt[2]]).sub(vertices[nt[1]])
                    ).length() < epsilon * epsilon * 0.1
                ) {
                    extrude(tris, tris[nt.n[0]]!!, te.vmax)
                    j = tris.size
                }
            }

            for (f in tris.size - 1 downTo 0) {
                val t = tris[f] ?: continue
                if (t.vmax >= 0) break
                triNormal(vertices[t[0]], vertices[t[1]], vertices[t[2]], tmp1)
                t.vmax = maxDirSterId(vertices, numVertices, tmp1, allow)
                if (isExtreme[t.vmax] != 0) {
                    t.vmax = -1
                } else {
                    t.rise = tmp1.dot(tmp0.set(vertices[t.vmax]).sub(vertices[t[0]]))
                }
            }
        }

        return tris.toTypedArray()
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

        fun bringOutYourDead(vertices: Array<Vec3>, indices: IntArray): Array<Vec3> {
            var count = 0
            val outVertices = MutableList(vertices.size) { Vec3() }
            val used = IntArray(vertices.size)
            for (i in indices.indices) {
                val v = indices[i]
                if (used[v] != 0) {
                    indices[i] = used[v] - 1
                } else {
                    indices[i] = count
                    outVertices[count].set(vertices[v])
                    used[v] = ++count
                }
            }
            return outVertices.toTypedArray()
        }

        fun removeB2b(tris: List<Tri?>, s: Tri, t: Tri) {
            tris[s.neib(s[1], s[2]).get()]!!.neib(s[2], s[1]).set(t.neib(s[2], s[1]).get())
            tris[t.neib(s[2], s[1]).get()]!!.neib(s[1], s[2]).set(s.neib(s[1], s[2]).get())
            tris[s.neib(s[2], s[0]).get()]!!.neib(s[0], s[2]).set(t.neib(s[0], s[2]).get())
            tris[t.neib(s[0], s[2]).get()]!!.neib(s[2], s[0]).set(s.neib(s[2], s[0]).get())
            tris[s.neib(s[0], s[1]).get()]!!.neib(s[1], s[0]).set(t.neib(s[1], s[0]).get())
            tris[t.neib(s[1], s[0]).get()]!!.neib(s[0], s[1]).set(s.neib(s[0], s[1]).get())
        }

        fun extrudable(tris: List<Tri?>, epsilon: Double): Tri? {
            var t: Tri? = null
            for (tri in tris) {
                if (t == null || tri != null && t.rise < tri.rise) {
                    t = tri
                }
            }
            return if (t == null || t.rise < epsilon) null else t
        }

        fun extrude(tris: MutableList<Tri?>, tri: Tri, v: Int) {
            val t = Int3(tri)
            val n = tris.size

            val ta = Tri(v, t[1], t[2], n + 0).with(tri.n[0], n + 1, n + 2)
            val tb = Tri(v, t[2], t[0], n + 1).with(tri.n[1], n + 2, n + 0)
            val tc = Tri(v, t[0], t[1], n + 2).with(tri.n[2], n + 0, n + 1)

            tris[tri.n[0]]?.neib(t[1], t[2])?.set(n + 0)
            tris[tri.n[1]]?.neib(t[2], t[0])?.set(n + 1)
            tris[tri.n[2]]?.neib(t[0], t[1])?.set(n + 2)

            tris.add(ta)
            tris.add(tb)
            tris.add(tc)

            if (hasVert(tris[ta.n[0]]!!, v)) {
                removeB2b(tris, ta, tris[ta.n[0]]!!)
                tris[ta.id] = null
                tris[tris[ta.n[0]]!!.id] = null
            }

            if (hasVert(tris[tb.n[0]]!!, v)) {
                removeB2b(tris, tb, tris[tb.n[0]]!!)
                tris[tb.id] = null
                tris[tris[tb.n[0]]!!.id] = null
            }

            if (hasVert(tris[tc.n[0]]!!, v)) {
                removeB2b(tris, tc, tris[tc.n[0]]!!)
                tris[tc.id] = null
                tris[tris[tc.n[0]]!!.id] = null
            }

            tris[tri.id] = null
        }

        private fun findSimplex(vertices: Array<Vec3>, numVertices: Int, allow: IntArray, out: Int4): Int4 {
            val tmp0 = Vec3()

            val basis = arrayOf(Vec3(0.01, 0.02, 1.0), Vec3(), Vec3())
            val p0 = maxDirSterId(vertices, numVertices, basis[0], allow)
            val p1 = maxDirSterId(vertices, numVertices, tmp0.negate(basis[0]), allow)
            basis[0].set(vertices[p0]).sub(vertices[p1])
            if (p0 == p1 || basis[0].x == 0.0 && basis[0].y == 0.0 && basis[0].z == 0.0) return out.set(-1, -1, -1, -1)

            basis[1].cross(tmp0.set(1.0, 0.02, 0.0), basis[0])
            basis[2].cross(tmp0.set(-0.02, 1.0, 0.0), basis[0])
            if (basis[1].length() > basis[2].length()) {
                basis[1].normalize()
            } else {
                basis[1].set(basis[2]).normalize()
            }
            var p2 = maxDirSterId(vertices, numVertices, basis[1], allow)
            if (p2 == p0 || p2 == p1) p2 = maxDirSterId(vertices, numVertices, tmp0.negate(basis[1]), allow)
            if (p2 == p0 || p2 == p1) return out.set(-1, -1, -1, -1)

            basis[1].set(vertices[p2]).sub(vertices[p0])
            basis[2].cross(basis[1], basis[0]).normalize()
            var p3 = maxDirSterId(vertices, numVertices, basis[2], allow)
            if (p3 == p0 || p3 == p1 || p3 == p2) p3 = maxDirSterId(vertices, numVertices, tmp0.negate(basis[2]), allow)
            if (p3 == p0 || p3 == p1 || p3 == p2) return out.set(-1, -1, -1, -1)

            val tmp1 = Vec3(vertices[p1]).sub(vertices[p0])
            if (tmp1.set(vertices[p3]).sub(vertices[p0]).dot(tmp0.cross(tmp1, tmp0.set(vertices[p2]).sub(vertices[p0]))) < 0) {
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
                    tmp0.set(u).scale(s).add(tmp2.set(v).scale(c)).scale(0.025).add(dir)
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
                            tmp0.set(tmp1.set(u).scale(s)).add(tmp2.set(v).scale(c)).scale(0.025).add(dir)
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
