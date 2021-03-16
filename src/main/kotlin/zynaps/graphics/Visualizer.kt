package zynaps.graphics

import zynaps.math.Matrix4
import zynaps.math.Scalar.min
import zynaps.math.Vector3
import java.awt.Color
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import kotlin.math.PI

class Visualizer(private val canvas: Canvas,  private val shadowSize: Int) {
    private val depthBuffer = FloatArray(canvas.width * canvas.height)
    private val shadowBuffer = FloatArray(shadowSize * shadowSize)
    private val frustum = Frustum()
    private val shadowFrustum = Frustum()
    private val rasterizer = Device(canvas.pixels, depthBuffer, canvas.width, canvas.height)
    private val shadowRasterizer = Device(null, shadowBuffer, shadowSize, shadowSize)
    private val lightCamera = Camera((PI / 4).toFloat(), 1F, 1F, 300F)
    private var ambient = 128

    init {
        val lightDirection = Vector3.normalize(Vector3(-0.95F, 2F, 1F))
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
        val orthographic = Matrix4.createOrthographic(-10F, 10F, -10F, 10F, 0F, 30F)

        ForkJoinPool.commonPool().invokeAll(
            listOf(
                Executors.callable {
                    shadowFrustum.extractPlanes(lightCamera.viewMatrix, orthographic)
                    shadowRasterizer.clear(0)
                    shadowRasterizer.view = lightCamera.viewMatrix
                    shadowRasterizer.proj = orthographic
                    scene.render(shadowFrustum, shadowRasterizer)
                },
                Executors.callable {
                    frustum.extractPlanes(camera.viewMatrix, camera.projectionMatrix)
                    rasterizer.clear(255 shl 24 or background.rgb)
                    rasterizer.view = camera.viewMatrix
                    rasterizer.proj = camera.projectionMatrix
                    scene.render(frustum, rasterizer)
                })
        )

        postProcess(camera.viewMatrix, camera.projectionMatrix, lightCamera.viewMatrix, orthographic)
    }

    private fun postProcess(cameraView: Matrix4, cameraProj: Matrix4, lightView: Matrix4, lightProj: Matrix4) {
        val combined = Matrix4.invert(cameraView * cameraProj) * lightView * lightProj
        val twoOverWidth = 2.0F / canvas.width
        val twoOverHeight = 2.0F / canvas.height
        val halfShadowSize = shadowSize shr 1
        val shadowSize1 = shadowSize - 1
        Parallel.partition(canvas.height) { _, from, to ->
            for (y in from until to) {
                val sy = 1 - y * twoOverHeight
                var mem = y * canvas.width
                for (x in 0 until canvas.width) {
                    val z = depthBuffer[mem]
                    if (z < 1) {
                        val sx = x * twoOverWidth - 1F
                        val sz = 2F * z - 1F

                        val tw = (combined.m03 * sx + combined.m13 * sy + combined.m23 * sz + combined.m33)
                        val tx = (combined.m00 * sx + combined.m10 * sy + combined.m20 * sz + combined.m30) / tw
                        val ty = (combined.m01 * sx + combined.m11 * sy + combined.m21 * sz + combined.m31) / tw
                        val tz = (combined.m02 * sx + combined.m12 * sy + combined.m22 * sz + combined.m32) / tw

                        val nx = (1 + tx) * halfShadowSize
                        val ny = (1 - ty) * halfShadowSize

                        var lit = ambient
                        val diffuse = canvas.pixels[mem]
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

                        canvas.pixels[mem] = 255 shl 24 or red.shl(0x10) or grn.shl(0x08) or blu
                    }
                    ++mem
                }
            }
        }
    }
}
