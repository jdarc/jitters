package zynaps.graphics

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

    private fun loadMaterials(matlib: String): Map<String, Material> {
        val path = matlib.substring("mtllib ".length)
        return if (Resources.exists(path)) {
            Resources.readLines(path)
                .filter { it.isNotBlank() }
                .joinToString(System.lineSeparator()) { it.trim() }
                .replace("newmtl", "~newmtl").split("~").map(String::lines)
                .filter { it.isNotEmpty() }
                .associate { findIn(it, "newmtl") to buildMaterial(it) }
                .toMap()
        } else {
            emptyMap()
        }
    }

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
                !vn.contains(-1) -> triangle.normals(vn[0], vn[1], vn[2]).useNormals(NormalType.VERTEX)
                else -> triangle.useNormals(if (smooth) NormalType.AVERAGED else NormalType.SURFACE)
            }
            triangle.changeMaterial(material)
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
