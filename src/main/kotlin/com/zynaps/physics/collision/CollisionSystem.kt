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

package com.zynaps.physics.collision

import com.zynaps.physics.Settings
import com.zynaps.physics.dynamics.RigidBody

class CollisionSystem {
    val bodies = mutableSetOf<RigidBody>()

    fun detect(listener: CollisionListener) {
        for (body0 in bodies.filter { it.isActive }) {
            for (body1 in bodies.filter { !it.isActive || body0.id < it.id }) {
                if (body0.hitTest(body1)) {
                    val result = GjkEpaSolver.collide(body0.skin, body1.skin, Settings.COLLISION_TOLERANCE)
                    if (result.hasCollided) {
                        val r0 = result.pointA - body0.skin.origin
                        val r1 = result.pointB - body1.skin.origin
                        listener.collisionNotify(body0, body1, result.normal, arrayOf(CollisionPoints(r0, r1, result.depth)))
                    }
                }
            }
        }
    }
}
