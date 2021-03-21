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

import com.zynaps.graphics.*
import com.zynaps.physics.PhysicsLoop
import com.zynaps.physics.dynamics.Simulation
import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import javax.swing.JFrame
import javax.swing.Timer
import javax.swing.WindowConstants
import kotlin.system.exitProcess

class MainFrame : JFrame("Jitters - Physics Engine") {
    private var bitmap: Bitmap

    init {
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        background = Color(0x54445b)

        setSize(1440, 900)
        setLocationRelativeTo(null)
        isResizable = false
        ignoreRepaint = true

        val camera = Camera()
        camera.aspectRatio = size.width / size.height.toFloat()
        camera.moveTo(-6F, 3F, 7F)
        camera.lookAt(0F, 0F, 0F)
        val control = FirstPersonControl(camera)

        bitmap = Bitmap(width shr 1, height shr 1)
        val visualizer = Visualizer(bitmap, 1024)
        visualizer.background = background

        var toggle = false

        val scene = Scene()
        val simulation = Simulation()

        val scenery = Scenery(scene, simulation)
        scenery.addGround()
        scenery.addModels { toggle }

        val throwSomething = { scenery.addRandomModel(camera.eye, camera.center) { toggle } }

        val mask = AWTEvent.MOUSE_MOTION_EVENT_MASK + AWTEvent.MOUSE_EVENT_MASK + AWTEvent.KEY_EVENT_MASK
        Toolkit.getDefaultToolkit().addAWTEventListener(fun(it: AWTEvent) {
            when (it) {
                is MouseEvent -> when (it.getID()) {
                    MouseEvent.MOUSE_PRESSED -> control.mouseDown(it)
                    MouseEvent.MOUSE_RELEASED -> control.mouseUp(it)
                    MouseEvent.MOUSE_MOVED, MouseEvent.MOUSE_DRAGGED -> control.mouseMove(it)
                }
                is KeyEvent -> {
                    when (it.getID()) {
                        KeyEvent.KEY_PRESSED -> {
                            if (it.keyCode == KeyEvent.VK_ESCAPE) exitProcess(0)
                            if (it.keyCode == KeyEvent.VK_SPACE) toggle = !toggle
                            if (it.keyCode == KeyEvent.VK_PERIOD) throwSomething()
                            control.keyDown(it.keyCode)
                        }
                        KeyEvent.KEY_RELEASED -> control.keyUp(it.keyCode)
                    }
                }
            }
        }, mask)

        PhysicsLoop(simulation).start()

        Timer(8) {
            control.update(0.008F, 30F)
            scene.update(0.008F)
            visualizer.render(scene, camera)
            repaint()
        }.start()
    }

    override fun paint(g: Graphics) {
        g as Graphics2D
        val scaleOp = AffineTransformOp(AffineTransform.getScaleInstance(2.0, 2.0), AffineTransformOp.TYPE_BILINEAR)
        g.drawImage(bitmap.image, scaleOp, 0, 0)
    }
}
