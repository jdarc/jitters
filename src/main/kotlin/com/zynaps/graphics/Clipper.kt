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

package com.zynaps.graphics

internal class Clipper {
    private val dst = Array(6) { Vertex() }
    private val src = Array(6) { Vertex() }

    operator fun get(index: Int) = src[index]

    fun clip(a: Vertex, b: Vertex, c: Vertex): Int {
        val mask = computeClipMask(a, b, c)

        if (outside(mask)) return 0

        src[0].copyFrom(a)
        src[1].copyFrom(b)
        src[2].copyFrom(c)
        if (inside(mask)) return 3

        src[3].copyFrom(a)
        return clip(dst, src, clip(src, dst, 3, -1F), 1F)
    }

    private companion object {
        private const val NER = 0b000000000000000111
        private const val FAR = 0b000000000000111000
        private const val BOT = 0b000000000111000000
        private const val TOP = 0b000000111000000000
        private const val LFT = 0b000111000000000000
        private const val RGH = 0b111000000000000000

        private const val SAFETY = 0.999995F

        private fun inside(mask: Int) = mask == 0

        private fun outside(packed: Int): Boolean {
            val ner = packed and NER == NER
            val far = packed and FAR == FAR
            val bot = packed and BOT == BOT
            val top = packed and TOP == TOP
            val lft = packed and LFT == LFT
            val rgh = packed and RGH == RGH
            return ner or far or bot or top or lft or rgh
        }

        fun computeClipMask(v0: Vertex, v1: Vertex, v2: Vertex): Int {
            var acc = if (v2.x < v2.w) 0 else 1
            acc = acc shl 1 or if (v1.x < v1.w) 0 else 1
            acc = acc shl 1 or if (v0.x < v0.w) 0 else 1
            acc = acc shl 1 or if (v2.x > -v2.w) 0 else 1
            acc = acc shl 1 or if (v1.x > -v1.w) 0 else 1
            acc = acc shl 1 or if (v0.x > -v0.w) 0 else 1
            acc = acc shl 1 or if (v2.y < v2.w) 0 else 1
            acc = acc shl 1 or if (v1.y < v1.w) 0 else 1
            acc = acc shl 1 or if (v0.y < v0.w) 0 else 1
            acc = acc shl 1 or if (v2.y > -v2.w) 0 else 1
            acc = acc shl 1 or if (v1.y > -v1.w) 0 else 1
            acc = acc shl 1 or if (v0.y > -v0.w) 0 else 1
            acc = acc shl 1 or if (v2.z < v2.w) 0 else 1
            acc = acc shl 1 or if (v1.z < v1.w) 0 else 1
            acc = acc shl 1 or if (v0.z < v0.w) 0 else 1
            acc = acc shl 1 or if (v2.z > -v2.w) 0 else 1
            acc = acc shl 1 or if (v1.z > -v1.w) 0 else 1
            return acc shl 1 or if (v0.z > -v0.w) 0 else 1
        }

        fun clip(src: Array<Vertex>, dst: Array<Vertex>, remaining: Int, side: Float): Int {
            var ia = 0
            var ib = 0
            var a1 = src[ia++]
            var a2 = src[ia++]
            var na = a1.z * side - a1.w * SAFETY
            for (it in 0 until remaining) {
                val nb = a2.z * side - a2.w * SAFETY
                if (na < 0F) {
                    if (nb < 0F) {
                        dst[ib++].copyFrom(a2)
                    } else {
                        dst[ib++].lerp(a1, a2, na / (na - nb))
                    }
                } else if (nb < 0F) {
                    dst[ib++].lerp(a1, a2, na / (na - nb))
                    dst[ib++].copyFrom(a2)
                }
                na = nb
                a1 = a2
                a2 = src[ia++]
            }
            dst[ib].copyFrom(dst[0])
            return ib
        }
    }
}
