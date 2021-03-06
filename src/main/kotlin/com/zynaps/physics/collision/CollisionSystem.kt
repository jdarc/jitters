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

import com.zynaps.physics.Globals
import com.zynaps.physics.RigidBody

@Suppress("MemberVisibilityCanBePrivate")
class CollisionSystem(val broadPhase: BroadPhase, val narrowPhase: NarrowPhase, val tolerance: Float = Globals.COLLISION_TOLERANCE) {
    fun detect(bodies: Set<RigidBody>, handler: CollisionHandler) {
        val candidates = broadPhase.collect(bodies)
        for ((body0, body1) in candidates) {
            val results = narrowPhase.test(body0.skin, body1.skin, tolerance)
            if (results.hasCollided) {
                val r0 = results.r0 - body0.skin.origin
                val r1 = results.r1 - body1.skin.origin
                handler.impact(body0, body1, results.normal, arrayOf(CollisionPoints(r0, r1, results.initialPenetration)))
            }
        }
    }
}
