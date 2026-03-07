package org.cf0x.rustnithm.Emu

import org.cf0x.rustnithm.Data.Net

class Tank {
    private val flickDefault = 1024

    private var flickHeight = flickDefault
    private var flickZoneLast = 0
    var bind = 0

    fun updateManual(flickZone: Int, flickUp: Int, flickDown: Int) {
        if (flickZoneLast != 0) {
            if (flickZone > flickZoneLast) {
                flickHeight += flickUp
            } else if (flickZone < flickZoneLast) {
                flickHeight -= flickDown
            }
        }
        flickZoneLast = flickZone
    }

    fun tickAnalysis(threshold: Int, plus: Int, minus: Int): Boolean {
        if (flickHeight == flickDefault) return false

        val flickDelta = flickHeight - flickDefault

        return if (flickDelta > 0) {
            if (flickDelta >= threshold) {
                flickHeight = flickDefault
                true
            } else {
                flickHeight -= minus
                if (flickHeight < flickDefault) flickHeight = flickDefault
                false
            }
        } else {
            val absDelta = -flickDelta
            if (absDelta >= threshold) {
                flickHeight = flickDefault
                true
            } else {
                flickHeight += plus
                if (flickHeight > flickDefault) flickHeight = flickDefault
                false
            }
        }
    }

    fun reset() {
        bind = 0
        flickHeight = flickDefault
        flickZoneLast = 0
    }
}

object TankManager {
    private val tanks = Array(10) { Tank() }
    private val pointerToTankMap = mutableMapOf<Int, Int>()

    fun updateFlick(
        index: Int,
        y: Int,
        zonesNum: Int,
        up: Int,
        down: Int,
        containerHeight: Int
    ) {
        if (y == -1) {
            val tankIdx = pointerToTankMap[index]
            if (tankIdx != null) {
                tanks[tankIdx].reset()
                pointerToTankMap.remove(index)
            }
            return
        }

        val tankIdx = pointerToTankMap[index] ?: run {
            val available = tanks.indexOfFirst { it.bind == 0 }
            if (available != -1) {
                tanks[available].bind = 1
                pointerToTankMap[index] = available
                available
            } else {
                null
            }
        }

        tankIdx?.let { i ->
            val zoneSize = containerHeight.toFloat() / zonesNum
            val currentZone = zonesNum - (y / zoneSize).toInt()
            val clampedZone = currentZone.coerceIn(1, zonesNum)
            tanks[i].updateManual(clampedZone, up, down)
        }
    }

    fun analysisLoop(threshold: Int, plus: Int, minus: Int) {
        tanks.forEach { tank ->
            if (tank.bind == 1) {
                if (tank.tickAnalysis(threshold, plus, minus)) {
                    Net.nativeTriggerFlick()
                }
            }
        }
    }

    fun resetAll() {
        tanks.forEach { it.reset() }
        pointerToTankMap.clear()
    }
}