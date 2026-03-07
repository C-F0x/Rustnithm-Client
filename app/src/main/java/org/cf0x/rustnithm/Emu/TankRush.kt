package org.cf0x.rustnithm.Emu

import kotlinx.coroutines.*
import org.cf0x.rustnithm.Data.DataManager

object TankRush {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun start(dataManager: DataManager) {
        if (job?.isActive == true) return

        job = scope.launch {
            while (isActive) {
                val threshold = dataManager.flickThreshold.value
                val plus = dataManager.flickEqualizerPlus.value
                val minus = dataManager.flickEqualizerMinus.value

                TankManager.analysisLoop(
                    threshold = threshold,
                    plus = plus,
                    minus = minus
                )

                delay(10)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        TankManager.resetAll()
    }
}