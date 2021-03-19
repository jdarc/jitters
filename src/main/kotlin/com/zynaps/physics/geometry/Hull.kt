package com.zynaps.physics.geometry

import com.zynaps.math.Matrix4
import com.zynaps.math.Vector3
import com.zynaps.physics.Settings
import kotlin.math.pow

class Hull(private val points: Array<Vector3>, scale: Float = 1F) : Shape() {
    private val scale = scale + Settings.COLLISION_TOLERANCE
    private var transpose = Matrix4.IDENTITY

    override var origin = Vector3.ZERO
    override var basis = Matrix4.IDENTITY
        set(value) {
            field = value * scale
            transpose = Matrix4.transpose(value)
        }

    override fun getSupport(direction: Vector3) = basis * localGetSupporting(Vector3.normalize(transpose * direction)) + origin

    override fun calculateBodyInertia(mass: Float): Matrix4 {
        var minX = Float.POSITIVE_INFINITY
        var minY = Float.POSITIVE_INFINITY
        var minZ = Float.POSITIVE_INFINITY
        var maxX = Float.NEGATIVE_INFINITY
        var maxY = Float.NEGATIVE_INFINITY
        var maxZ = Float.NEGATIVE_INFINITY
        for (i in points.indices) {
            val vx = points[i].x
            val vy = points[i].y
            val vz = points[i].z
            if (vx < minX) minX = vx else if (vx > maxX) maxX = vx
            if (vy < minY) minY = vy else if (vy > maxY) maxY = vy
            if (vz < minZ) minZ = vz else if (vz > maxZ) maxZ = vz
        }
        var sizeX = maxX - minX
        var sizeY = maxY - minY
        var sizeZ = maxZ - minZ
        volume = sizeX * sizeY * sizeZ
        val cog = ((minX + maxX).pow(2) + (minY + maxY).pow(2) + (minZ + maxZ).pow(2)) * 0.25F
        sizeX = sizeX.pow(2) * 0.25F
        sizeY = sizeY.pow(2) * 0.25F
        sizeZ = sizeZ.pow(2) * 0.25F
        val x = cog + (sizeY + sizeZ) * (1F / 3F)
        val y = cog + (sizeZ + sizeX) * (1F / 3F)
        val z = cog + (sizeX + sizeY) * (1F / 3F)
        return Matrix4.createScale(x, y, z)
    }

    private fun localGetSupporting(v: Vector3): Vector3 {
        var out = Vector3.ZERO
        var dist = Float.NEGATIVE_INFINITY
        for (p in points) {
            val dot = Vector3.dot(v, p)
            if (dot > dist) {
                dist = dot
                out = p
            }
        }
        return out
    }
}
