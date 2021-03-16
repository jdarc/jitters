import zynaps.graphics.*
import zynaps.jitters.geometry.Hull
import zynaps.jitters.geometry.Plane
import zynaps.jitters.physics.RigidBody
import zynaps.jitters.physics.Simulation
import zynaps.math.Matrix4
import zynaps.math.Vector3
import java.awt.Color
import java.util.concurrent.ThreadLocalRandom

class Scenery(private val scene: Scene, private val simulation: Simulation) {

    fun addGround() {
        val body = RigidBody(Plane())
        body.isActive = true
        body.isMovable = false
        body.moveTo(Vector3(0F, -1F, 0F), Matrix4.IDENTITY)
        simulation.addBody(body)
        scene.root.addNode(PhysicsNode(body).addNode(Node(Matrix4.createScale(20F), PLANE_MESH)))
    }

    fun addModels(hullToggle: () -> Boolean) {
        val dino = getModelAndHull("dinorider.obj")
        val grunt = getModelAndHull("grunt.obj")
        val triceratops = getModelAndHull("triceratops.obj")
        val bowlingPin = getModelAndHull("bowlingpin.obj")
        val models = arrayOf(dino, grunt, triceratops, bowlingPin)

        for (i in 0..49) {
            val (model, hull, hullPoints) = models[ThreadLocalRandom.current().nextInt(models.size)]
            val body = RigidBody(Hull(hullPoints, 1.9F))
            body.moveTo(Vector3.random() + Vector3(0F, 1F + i * 3F, 0F), Matrix4.IDENTITY)
            body.isActive = true
            body.mass = 1F
            simulation.addBody(body)
            scene.root.addNode(
                PhysicsNode(body)
                    .addNode(ToggleNode { !hullToggle() }.addNode(Node(Matrix4.createScale(2F), model)))
                    .addNode(ToggleNode { hullToggle() }.addNode(Node(Matrix4.createScale(2F), hull)))
            )
        }
    }

    private fun getModelAndHull(filename: String): Triple<Model, Model, Array<Vector3>> {
        val model = Resources.readModel(filename)
        val hull = model.computeHull()
        return Triple(model, hull, hull.extractPoints())
    }

    private companion object {
        val GREEN = ToolKit.buildCheckeredImage(256, 32, Color(0x228B22), Color(0x32CD32))
        val YELLOW = ToolKit.buildCheckeredImage(128, 8, Color(255, 255, 0), Color(0xD7AA00))
        val BLUE = ToolKit.buildCheckeredImage(128, 8, Color(99, 171, 255), Color(0, 112, 243))
        val PURPLE = ToolKit.buildCheckeredImage(128, 8, Color(255, 111, 239), Color(241, 20, 214))

        val PLANE_MESH = Primitives.createPlane().apply { changeMaterial(TextureMaterial(Texture(GREEN))) }.compile()
        val SPHERE_MESH = Primitives.createSphere(1F, 16, 32).apply { changeMaterial(TextureMaterial(Texture(YELLOW))) }.compile()
    }
}
