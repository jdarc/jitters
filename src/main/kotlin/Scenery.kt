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
import com.zynaps.math.Matrix4
import com.zynaps.math.Vector3
import com.zynaps.physics.RigidBody
import com.zynaps.physics.Simulation
import com.zynaps.physics.geometry.ConvexHull
import com.zynaps.tools.convexhull.HullMaker
import com.zynaps.tools.convexhull.Point3D
import java.util.concurrent.ThreadLocalRandom

class Scenery(private val scene: Scene, private val simulation: Simulation) {

    private var models: Array<Triple<Model, Model, Array<Vector3>>>

    fun addGround() {
        val plane = createBox(5F, 0.001F, 5F).compile()
        val body = RigidBody(ConvexHull(plane.extractPoints(), 20F))
        body.isActive = true
        body.isMovable = false
        body.moveTo(Vector3(0F, -1F, 0F), Matrix4.IDENTITY)
        simulation.addBody(body)
        scene.root.addNode(PhysicsNode(body).addNode(Node(Matrix4.createScale(20F), plane)))
    }

    fun addStackedBoxes() {
        val assembler = createBox(1F, 1F, 1F)
        assembler.withMaterial(CheckerMaterial(0x8B2222, 0xCD3232, 8F))
        val box = assembler.compile()
        for (z in -2..3) {
            for (y in 0..5) {
                for (x in -2..3) {
                    val body = RigidBody(ConvexHull(box.extractPoints()))
                    body.mass = 10F
                    body.moveTo(Vector3(x * 1.1F, y * 1.1F, z * 1.1F))
                    simulation.addBody(body)
                    scene.root.addNode(PhysicsNode(body).addNode(Node(geometry = box)))
                }
            }
        }
    }

    fun addModels(hullToggle: () -> Boolean) {
        for (i in 0..19) {
            val (model, hull, hullPoints) = models[ThreadLocalRandom.current().nextInt(models.size)]
            val body = RigidBody(ConvexHull(hullPoints, 2F))
            body.mass = 10F
            body.moveTo(random() + Vector3(0F, 1F + i * 3F, 0F))
            simulation.addBody(body)
            scene.root.addNode(
                PhysicsNode(body)
                    .addNode(ToggleNode { !hullToggle() }.addNode(Node(Matrix4.createScale(2F), model)))
                    .addNode(ToggleNode { hullToggle() }.addNode(Node(Matrix4.createScale(2F), hull)))
            )
        }
    }

    fun addRandomModel(from: Vector3, at: Vector3, hullToggle: () -> Boolean) {
        val (model, hull, hullPoints) = models[ThreadLocalRandom.current().nextInt(models.size)]
        val body = RigidBody(ConvexHull(hullPoints, 2F))
        body.mass = 10F
        body.moveTo(from)
        body.linearVelocity = (at - from) * 20F
        body.isActive = true
        simulation.addBody(body)
        scene.root.addNode(
            PhysicsNode(body)
                .addNode(ToggleNode { !hullToggle() }.addNode(Node(Matrix4.createScale(2F), model)))
                .addNode(ToggleNode { hullToggle() }.addNode(Node(Matrix4.createScale(2F), hull)))
        )
    }

    init {
        val dino = getModelAndHull("dinorider.obj")
        val grunt = getModelAndHull("grunt.obj")
        val triceratops = getModelAndHull("triceratops.obj")
        val bowlingPin = getModelAndHull("bowlingpin.obj")
        models = arrayOf(dino, grunt, triceratops, bowlingPin)
    }

    private companion object {
        val RND: ThreadLocalRandom = ThreadLocalRandom.current()

        fun random() = Vector3.normalize(Vector3(RND.nextFloat() * 2 - 1, RND.nextFloat() * 2 - 1, RND.nextFloat() * 2 - 1))

        fun getModelAndHull(filename: String): Triple<Model, Model, Array<Vector3>> {
            val model = Resources.readModel(filename)
            val hull = computeHull(model)
            return Triple(model, hull, hull.extractPoints())
        }

        fun computeHull(model: Model): Model {
            val result = HullMaker().build(model.extractPoints().map { (x, y, z) -> Point3D(x, y, z) }.toTypedArray())
            val assembler = Assembler()
            result.vertices.forEach { (x, y, z) -> assembler.addVertex(x, y, z) }
            result.indices.toList().windowed(3, 3).forEach { assembler.createTriangle(it[0], it[1], it[2]) }
            assembler.withNormalType(NormalType.SURFACE)
            assembler.withMaterial(ColorMaterial(0x0088FF))
            return assembler.compile()
        }

        fun createBox(width: Float, height: Float, depth: Float): Assembler {
            val w = 0.5F * width.coerceAtLeast(0.00001F)
            val h = 0.5F * height.coerceAtLeast(0.00001F)
            val d = 0.5F * depth.coerceAtLeast(0.00001F)
            val assembler = Assembler()

            assembler.addVertex(-w, -h, d)
            assembler.addVertex(w, -h, d)
            assembler.addVertex(w, h, d)
            assembler.addVertex(-w, h, d)
            assembler.addVertex(-w, -h, -d)
            assembler.addVertex(w, -h, -d)
            assembler.addVertex(w, h, -d)
            assembler.addVertex(-w, h, -d)

            assembler.addUvCoordinate(0F, 1F)
            assembler.addUvCoordinate(1F, 1F)
            assembler.addUvCoordinate(1F, 0F)
            assembler.addUvCoordinate(0F, 0F)

            assembler.createTriangle(0, 1, 2).withUvIndex(0, 1, 2)
            assembler.createTriangle(2, 3, 0).withUvIndex(2, 3, 0)
            assembler.createTriangle(3, 2, 6).withUvIndex(0, 1, 2)
            assembler.createTriangle(6, 7, 3).withUvIndex(2, 3, 0)
            assembler.createTriangle(5, 4, 7).withUvIndex(0, 1, 2)
            assembler.createTriangle(7, 6, 5).withUvIndex(2, 3, 0)
            assembler.createTriangle(4, 5, 1).withUvIndex(0, 1, 2)
            assembler.createTriangle(1, 0, 4).withUvIndex(2, 3, 0)
            assembler.createTriangle(4, 0, 3).withUvIndex(0, 1, 2)
            assembler.createTriangle(3, 7, 4).withUvIndex(2, 3, 0)
            assembler.createTriangle(1, 5, 6).withUvIndex(0, 1, 2)
            assembler.createTriangle(6, 2, 1).withUvIndex(2, 3, 0)

            assembler.withNormalType(NormalType.SURFACE)
            assembler.withMaterial(CheckerMaterial(0x228B22, 0x32CD32, 64F))
            return assembler
        }
    }
}
