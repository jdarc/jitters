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

import java.io.IOException

@Suppress("SpellCheckingInspection")
class WavefrontReader {

    @Throws(IOException::class)
    fun load(path: String): Assembler {
        var smooth = false
        val assembler = Assembler()
        var material: Material = Material.DEFAULT
        val materials = mutableMapOf<String, Material>()
        Resources.stream(path) { stream ->
            stream.filter { it.isNotEmpty() }.map { it.trim() }.forEach {
                when {
                    it.startsWith("mtllib ", true) -> materials.putAll(loadMaterials(it))
                    it.startsWith("v ", true) -> assembler.addVertex(floats(it, "v ".length).toFloatArray())
                    it.startsWith("vt ", true) -> assembler.addUvCoordinate(floats(it, "vt ".length).toFloatArray())
                    it.startsWith("vn ", true) -> assembler.addNormal(floats(it, "vn ".length).toFloatArray())
                    it.startsWith("f ", true) -> assembleFace(it, smooth, material, assembler)
                    it.startsWith("s ", true) -> smooth = isSmoothing(it.substring("s ".length).trim().toLowerCase())
                    it.startsWith("usemtl ", true) -> material = materials[it.substring("usemtl ".length)] ?: Material.DEFAULT
                }
            }
        }
        assembler.centerAndScale()
        return assembler
    }

    private fun isSmoothing(s: String) = s == "on" || s == "1"

    private fun loadMaterials(matlib: String) = Resources.readLines(matlib.substring("mtllib ".length))
        .filter { it.isNotBlank() }
        .joinToString(System.lineSeparator()) { it.trim() }
        .replace("newmtl", "~newmtl").split("~").map(String::lines)
        .filter { it.isNotEmpty() }
        .associate { findIn(it, "newmtl") to buildMaterial(it) }
        .toMap()

    private fun buildMaterial(directives: List<String>): Material {
        return ColorMaterial(toRGB(findIn(directives, "kd", "1 1 1").split(' ').map(String::toFloat)))
    }

    private fun findIn(items: List<String>, item: String, default: String = "") = (items.find {
        it.startsWith(item, true)
    } ?: "$item $default").substring(item.length).trim()

    private fun assembleFace(line: String, smooth: Boolean, material: Material, assembler: Assembler) {
        val vx = IntArray(3)
        val vt = IntArray(3)
        val vn = IntArray(3)

        val match = line.substring("f ".length).trim().split(' ')

        val a = match.first()
        vx[0] = extractVx(a)
        vt[0] = extractVt(a)
        vn[0] = extractVn(a)
        match.subList(1, match.size).windowed(2).forEach { (b, c) ->
            vx[1] = extractVx(b)
            vt[1] = extractVt(b)
            vn[1] = extractVn(b)
            vx[2] = extractVx(c)
            vt[2] = extractVt(c)
            vn[2] = extractVn(c)
            val triangle = assembler.createTriangle(vx[0], vx[1], vx[2])
            when {
                !vn.contains(-1) -> triangle.withNormalIndex(vn[0], vn[1], vn[2]).normalType = NormalType.VERTEX
                else -> triangle.normalType = if (smooth) NormalType.AVERAGED else NormalType.SURFACE
            }
            triangle.material = material
        }
    }


    private companion object {
        fun toRGB(xyz: List<Float>) = to256(xyz[0], 16) or to256(xyz[1], 8) or to256(xyz[2])

        fun to256(x: Float, shift: Int = 0) = (x.coerceIn(0.0F, 1.0F) * 0xFF).toInt() shl shift

        fun floats(line: String, offset: Int) = line.substring(offset).trim().split(' ').map(String::toFloat).toTypedArray()

        private fun extractVx(it: String) = when {
            it.contains('/') -> it.substring(0, it.indexOf('/')).trim().toInt() - 1
            else -> it.toInt() - 1
        }

        private fun extractVt(it: String): Int {
            val slash1 = it.indexOf('/')
            val slash2 = it.lastIndexOf('/')
            return when {
                slash1 != -1 && slash1 == slash2 -> tryParse(it.substring(slash1 + 1)) - 1
                slash1 != -1 && slash2 != -1 && slash1 != slash2 -> tryParse(it.substring(slash1 + 1, slash2)) - 1
                else -> -1
            }
        }

        private fun extractVn(it: String): Int {
            val slash = it.lastIndexOf('/')
            if (it.indexOf('/') == slash) return -1
            return if (slash != -1) tryParse(it.substring(slash + 1)) - 1 else -1
        }

        fun tryParse(s: String): Int {
            return try {
                val trimmed = s.trim { it <= ' ' }
                if (trimmed.isBlank()) 0 else trimmed.toInt()
            } catch (ignore: NumberFormatException) {
                0
            }
        }
    }
}
