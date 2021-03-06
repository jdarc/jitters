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

package com.zynaps.physics.collision.narrowphase

import com.zynaps.physics.collision.ImpactDetails
import com.zynaps.physics.collision.NarrowPhase
import com.zynaps.physics.geometry.CollisionSkin

class GjkEpa : NarrowPhase {
    private val gjk = Gjk()
    private val epa = Epa()
    private var results = Results()

    override fun test(shape0: CollisionSkin, shape1: CollisionSkin, margin: Float): ImpactDetails {
        results.status = ResultsStatus.SEPARATED
        results.gjkIterations = gjk.iterations + 1
        if (gjk.init(shape0, shape1, margin).searchOrigin()) {
            results.epaIterations = epa.iterations + 1
            results.initialPenetration = epa.evaluate(gjk)
            if (results.initialPenetration > 0F) {
                results.r0 = epa.nearest[0]
                results.r1 = epa.nearest[1]
                results.status = ResultsStatus.PENETRATING
                results.normal = epa.normal
            } else if (epa.failed) results.status = ResultsStatus.EPA_FAILED
        } else if (gjk.failed) results.status = ResultsStatus.GJK_FAILED
        return results
    }
}
