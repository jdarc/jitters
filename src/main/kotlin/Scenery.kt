import com.github.quickhull.QuickHull3D
import com.zynaps.graphics.*
import com.zynaps.math.Matrix4
import com.zynaps.math.Vector3
import com.zynaps.physics.dynamics.RigidBody
import com.zynaps.physics.dynamics.Simulation
import com.zynaps.physics.geometry.Hull
import java.awt.Color
import java.util.concurrent.ThreadLocalRandom

class Scenery(private val scene: Scene, private val simulation: Simulation) {

    fun addGround() {
        val plane = Primitives.createBox(5F, 0.001F, 5F).apply { changeMaterial(GREEN) }.compile()
        val body = RigidBody(Hull(plane.extractPoints(), 20F))
        body.isActive = true
        body.isMovable = false
        body.moveTo(Vector3(0F, -1F, 0F), Matrix4.IDENTITY)
        simulation.addBody(body)
        scene.root.addNode(PhysicsNode(body).addNode(Node(Matrix4.createScale(20F), plane)))
    }

    fun addModels(hullToggle: () -> Boolean) {
        val dino = getModelAndHull("dinorider.obj")
        val grunt = getModelAndHull("grunt.obj")
        val triceratops = getModelAndHull("triceratops.obj")
        val bowlingPin = getModelAndHull("bowlingpin.obj")
        val models = arrayOf(dino, grunt, triceratops, bowlingPin)

        for (i in 0..19) {
            val (model, hull, hullPoints) = models[ThreadLocalRandom.current().nextInt(models.size)]
            val body = RigidBody(Hull(hullPoints, 2F))
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

    private companion object {
        val GREEN = CheckerMaterial(0x228B22, 0x32CD32, 64F)

        fun getModelAndHull(filename: String): Triple<Model, Model, Array<Vector3>> {
            val model = Resources.readModel(filename)
            val hull = computeHull(model)
            return Triple(model, hull, hull.extractPoints())
        }

        fun computeHull(model: Model): Model {
            val hull = QuickHull3D(model.extractPoints().fold(FloatArray(0), { acc, cur -> acc + floatArrayOf(cur.x, cur.y, cur.z) }))
            val assembler = Assembler()
            hull.vertices.toList().windowed(3, 3).forEach { (x, y, z) -> assembler.addVertex(x, y, z) }
            hull.faces.forEach { for (i in 1 until it.size - 1) assembler.createTriangle(it[0], it[i], it[i + 1]) }
            return assembler.useNormals(NormalType.SURFACE).changeMaterial(ColorMaterial(0x0088FF)).compile()
        }
    }
}
