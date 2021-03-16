import zynaps.PhysicsLoop
import zynaps.graphics.*
import zynaps.graphics.Canvas
import zynaps.jitters.physics.Simulation
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
    private var canvas: Canvas

    init {
        defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
        background = Color(0x54445b)

        setSize(1440, 900)
        setLocationRelativeTo(null)
        isResizable = false
        ignoreRepaint = true

        val camera = Camera((Math.PI / 4.0).toFloat(), size.width / size.height.toFloat(), 1F, 100F)
        camera.moveTo(-6F, 3F, 7F)
        camera.lookAt(0F, 0F, 0F)
        val control = FirstPersonControl(camera)

        canvas = Canvas(width shr 1, height shr 1)
        val visualizer = Visualizer(canvas, 1024)
        visualizer.background = background

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
                            control.keyDown(it.keyCode)
                        }
                        KeyEvent.KEY_RELEASED -> control.keyUp(it.keyCode)
                    }
                }
            }
        }, mask)

        val scene = Scene()
        val simulation = Simulation()

        val scenery = Scenery(scene, simulation)
        scenery.addGround()
        scenery.addDinosaurs()

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
        g.drawImage(canvas.image, scaleOp, 0, 0)
    }
}
