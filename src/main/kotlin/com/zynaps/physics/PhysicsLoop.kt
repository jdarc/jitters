package com.zynaps.physics

import com.zynaps.physics.dynamics.Simulation

class PhysicsLoop(private val simulation: Simulation) {
    private var active = false

    fun start() {
        if (active) return
        Thread {
            active = true
            var tock = System.nanoTime()
            while (active) {
                val tick = tock
                tock = System.nanoTime()
                simulation.integrate((tock - tick) / 1000000000.0F)
                Thread.sleep(1)
            }
        }.start()
    }

    fun stop() {
        active = false
    }
}
