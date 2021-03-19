package com.zynaps.graphics

import com.zynaps.math.Matrix4
import com.zynaps.math.Vector3
import java.awt.Color
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import kotlin.math.ceil
import kotlin.math.min

@Suppress("unused", "HasPlatformType")
class Visualizer(private val bitmap: Bitmap, private val shadowSize: Int) {
    private val depthBuffer = FloatArray(bitmap.width * bitmap.height)
    private val shadowBuffer = FloatArray(shadowSize * shadowSize)
    private val frustum = Frustum()
    private val shadowFrustum = Frustum()
    private val rasterizer = Device(bitmap.pixels, depthBuffer, bitmap.width, bitmap.height)
    private val shadowRasterizer = Device(IntArray(0), shadowBuffer, shadowSize, shadowSize)
    private val lightCamera = Camera()
    private var ambient = 128

    init {
        val lightDirection = Vector3.normalize(Vector3(-0.95F, 2F, 1F))
        lightCamera.farPlane = 300F
        lightCamera.moveTo(lightDirection.x * 20, lightDirection.y * 20, lightDirection.z * 20)
        lightCamera.lookAt(0F, 0F, 0F)

        rasterizer.moveLight(lightDirection.x, lightDirection.y, lightDirection.z)
        shadowRasterizer.cull = CullMode.FRONT
    }

    var ambientIntensity
        get() = ambient / 255F
        set(value) {
            ambient = (value.coerceIn(0F, 1F) * 255F).toInt()
        }

    var background = Color.BLACK

    fun render(scene: Scene, camera: Camera) {
        val orthographic = Matrix4.createOrthographic(-10F, 10F, -10F, 10F, 1F, 30F)

        ForkJoinPool.commonPool().invokeAll(
            listOf(
                Executors.callable {
                    shadowFrustum.configure(lightCamera.viewMatrix, orthographic)
                    shadowRasterizer.clear(0)
                    shadowRasterizer.view = lightCamera.viewMatrix
                    shadowRasterizer.proj = orthographic
                    scene.render(shadowFrustum, shadowRasterizer)
                },
                Executors.callable {
                    frustum.configure(camera.viewMatrix, camera.projectionMatrix)
                    rasterizer.clear(background.rgb)
                    rasterizer.view = camera.viewMatrix
                    rasterizer.proj = camera.projectionMatrix
                    scene.render(frustum, rasterizer)
                })
        )

        postProcess(camera.viewMatrix, camera.projectionMatrix, lightCamera.viewMatrix, orthographic)
    }

    private fun postProcess(cameraView: Matrix4, cameraProj: Matrix4, lightView: Matrix4, lightProj: Matrix4) {
        val combined = lightProj * lightView * Matrix4.invert(cameraProj * cameraView)
        val twoOverWidth = 2F / bitmap.width
        val twoOverHeight = 2F / bitmap.height
        val halfShadowSize = shadowSize shr 1
        val shadowSize1 = shadowSize - 1
        val size = ceil(bitmap.height.toFloat() / CPUS).toInt()
        ForkJoinPool.commonPool().invokeAll((0 until bitmap.height step size).mapIndexed { _, it ->
            Executors.callable {
                for (y in it until it + min(size, bitmap.height - it)) {
                    val sy = 1 - y * twoOverHeight
                    var mem = y * bitmap.width
                    for (x in 0 until bitmap.width) {
                        val z = depthBuffer[mem]
                        if (z < 1) {
                            val sx = x * twoOverWidth - 1F
                            val sz = 2F * z - 1F

                            val tw = (combined.m30 * sx + combined.m31 * sy + combined.m32 * sz + combined.m33)
                            val tx = (combined.m00 * sx + combined.m01 * sy + combined.m02 * sz + combined.m03) / tw
                            val ty = (combined.m10 * sx + combined.m11 * sy + combined.m12 * sz + combined.m13) / tw
                            val tz = (combined.m20 * sx + combined.m21 * sy + combined.m22 * sz + combined.m23) / tw

                            val nx = (1 + tx) * halfShadowSize
                            val ny = (1 - ty) * halfShadowSize

                            var lit = ambient
                            val diffuse = bitmap.pixels[mem]
                            if (nx > 1 && nx < shadowSize1 && ny > 1 && ny < shadowSize1) {
                                val nz = (1F + tz) * 0.5F
                                val smem1 = ny.toInt() * shadowSize + nx.toInt() - 1
                                val smem0 = smem1 - shadowSize
                                val smem2 = smem1 + shadowSize
                                val a = if (nz < shadowBuffer[smem0 + 0]) 1 else 0
                                val b = if (nz < shadowBuffer[smem0 + 1]) 1 else 0
                                val c = if (nz < shadowBuffer[smem0 + 2]) 1 else 0
                                val d = if (nz < shadowBuffer[smem1 + 0]) 1 else 0
                                val e = if (nz < shadowBuffer[smem1 + 1]) 1 else 0
                                val f = if (nz < shadowBuffer[smem2 + 0]) 1 else 0
                                val g = if (nz < shadowBuffer[smem2 + 1]) 1 else 0
                                val h = if (nz < shadowBuffer[smem2 + 2]) 1 else 0
                                val acc = a + b + c + d + e + f + g + h

                                lit = min(0xFF, (diffuse shr 24 and 0xFF) * acc shr 3) + ambient
                            }

                            var red = (diffuse shr 0x10 and 255) * lit shr 8
                            var grn = (diffuse shr 0x08 and 255) * lit shr 8
                            var blu = (diffuse shr 0x00 and 255) * lit shr 8
                            if (red > 255) red = 255
                            if (grn > 255) grn = 255
                            if (blu > 255) blu = 255

                            bitmap.pixels[mem] = 255 shl 24 or red.shl(0x10) or grn.shl(0x08) or blu
                        }
                        ++mem
                    }
                }
            }
        })
    }

    private companion object {
        val CPUS = Runtime.getRuntime().availableProcessors()
    }
}
