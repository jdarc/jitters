package com.zynaps.graphics

import com.zynaps.math.Matrix4
import com.zynaps.math.Vector3
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2

class Camera {
    private var view: Matrix4? = Matrix4.IDENTITY
    private var proj: Matrix4? = Matrix4.IDENTITY

    var eye = Vector3.UNIT_Z
        set(value) {
            field = value
            view = null
        }

    var center = Vector3.ZERO
        set(value) {
            field = value
            view = null
        }

    var up = Vector3.UNIT_Y
        set(value) {
            field = value
            view = null
        }

    var fieldOfView = (PI / 4.0).toFloat()
        set(value) {
            field = value.coerceIn(0.0001F, 3.1415F)
            proj = null
        }

    var aspectRatio = 1F
        set(value) {
            field = value.coerceIn(0.0001F, 65536F)
            proj = null
        }

    var nearPlane = 1F
        set(value) {
            field = value.coerceIn(0.0001F, Float.MAX_VALUE)
            proj = null
        }

    var farPlane = 100F
        set(value) {
            field = value.coerceIn(0.0001F, Float.MAX_VALUE)
            proj = null
        }

    val lookAt get() = Vector3.normalize(center - eye)

    val yaw get() = -atan2(-lookAt.x, -lookAt.z)

    val pitch get() = -asin(lookAt.y)

    val viewMatrix: Matrix4
        get() {
            view = view ?: Matrix4.createLookAt(eye, center, up)
            return view!!
        }

    val projectionMatrix: Matrix4
        get() {
            proj = proj ?: Matrix4.createPerspectiveFov(fieldOfView, aspectRatio, nearPlane, farPlane)
            return proj!!
        }

    fun moveTo(x: Float, y: Float, z: Float) {
        eye = Vector3(x, y, z)
    }

    fun lookAt(x: Float, y: Float, z: Float) {
        center = Vector3(x, y, z)
    }

    fun worldUp(x: Float, y: Float, z: Float) {
        up = Vector3.normalize(Vector3(x, y, z))
    }
}
