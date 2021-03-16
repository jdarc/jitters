package zynaps.graphics

import zynaps.math.Matrix4
import zynaps.math.Vector3
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import kotlin.math.sqrt

class FirstPersonControl(private val camera: Camera) {
    private var lookAt = camera.lookAt
    private var yaw = camera.yaw
    private var pitch = camera.pitch
    private var lastX = 0
    private var lastY = 0
    private var dragging = false
    private var movementMask = 0

    fun keyUp(code: Int) {
        when (code) {
            KeyEvent.VK_W -> movementMask = movementMask xor MOVEMENT_FORWARD
            KeyEvent.VK_S -> movementMask = movementMask xor MOVEMENT_BACK
            KeyEvent.VK_A -> movementMask = movementMask xor MOVEMENT_LEFT
            KeyEvent.VK_D -> movementMask = movementMask xor MOVEMENT_RIGHT
        }
    }

    fun keyDown(code: Int) {
        when (code) {
            KeyEvent.VK_W -> movementMask = movementMask or MOVEMENT_FORWARD
            KeyEvent.VK_S -> movementMask = movementMask or MOVEMENT_BACK
            KeyEvent.VK_A -> movementMask = movementMask or MOVEMENT_LEFT
            KeyEvent.VK_D -> movementMask = movementMask or MOVEMENT_RIGHT
        }
    }

    fun mouseDown(evt: MouseEvent) {
        dragging = true
        lastX = evt.x
        lastY = evt.y
    }

    fun mouseUp(evt: MouseEvent) {
        dragging = false
        lastX = evt.x
        lastY = evt.y
    }

    fun mouseMove(evt: MouseEvent) {
        if (dragging) {
            yaw += (evt.x - lastX) * 0.005F
            pitch += (evt.y - lastY) * 0.005F
            pitch = pitch.coerceIn(-1.57F, 1.57F)
            lookAt = Matrix4.createRotationY(yaw) * Matrix4.createRotationX(pitch) * Vector3(0F, 0F, -1F)
        }
        lastX = evt.x
        lastY = evt.y
    }

    fun update(seconds: Float, speed: Float) {
        var eye = camera.eye
        val scaledSpeed = seconds * speed

        if (movementMask and MOVEMENT_FORWARD == MOVEMENT_FORWARD) {
            eye += lookAt * scaledSpeed
        } else if (movementMask and MOVEMENT_BACK == MOVEMENT_BACK) {
            eye -= lookAt * scaledSpeed
        }
        if (movementMask and MOVEMENT_LEFT == MOVEMENT_LEFT) {
            val scalar = scaledSpeed / sqrt(lookAt.z * lookAt.z + lookAt.x * lookAt.x)
            eye -= Vector3(-lookAt.z * scalar, 0F, lookAt.x * scalar)
        } else if (movementMask and MOVEMENT_RIGHT == MOVEMENT_RIGHT) {
            val scalar = scaledSpeed / sqrt(lookAt.z * lookAt.z + lookAt.x * lookAt.x)
            eye += Vector3(-lookAt.z * scalar, 0F, lookAt.x * scalar)
        }

        camera.eye = eye
        camera.center = eye + lookAt
    }

    companion object {
        private const val MOVEMENT_FORWARD = 1
        private const val MOVEMENT_BACK = 2
        private const val MOVEMENT_LEFT = 4
        private const val MOVEMENT_RIGHT = 8
    }
}
