package com.zynaps.graphics

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.streams.toList

object Resources {
    private val loader = Resources.javaClass.classLoader

    fun <T> stream(name: String, cb: (Stream<String>) -> T): T = Files.lines(Path.of(fetch(name)?.toURI())).use { return cb(it) }

    fun readLines(name: String) = stream(name) { it.toList() }

    fun readModel(name: String) = WavefrontReader().load(name).compile()

    private fun fetch(name: String) = loader.getResource(name)
}


