package org.cf0x.rustnithm.Jour

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import org.cf0x.rustnithm.Data.Net

object JourBackend {

    fun validateIp(ip: String): Boolean {
        val parts = ip.split('.')
        if (parts.size != 4) return false
        return parts.all { part ->
            part.isNotEmpty() && part.length <= 3 && part.all { it.isDigit() } && part.toInt() in 0..255
        }
    }

    fun validatePort(port: String): Boolean {
        return port.toIntOrNull()?.let { it in 0..65535 } ?: false
    }

    fun updateNetworkConfig(ip: String, port: Int, protocol: Int) {
        Net.updateConfig(ip, port, protocol)
    }

    fun sendGameState(
        air: Set<Int>,
        airMode: Int,
        slide: Set<Int>,
        coin: Boolean,
        service: Boolean,
        test: Boolean,
        card: Boolean,
        accessCode: String
    ) {
        Net.sendFullState(
            air = air,
            airMode = airMode,
            slide = slide,
            coin = coin,
            service = service,
            test = test,
            isCardActive = card,
            accessCode = accessCode
        )
    }

    fun pollConnectionState(): Flow<ConnState> = flow {
        while (true) {
            val rawState = Net.nativeGetState()
            val connState = when (rawState) {
                1 -> ConnState.ACTIVE
                2 -> ConnState.WAITING
                else -> ConnState.SUSPEND
            }
            emit(connState)
            delay(100)
        }
    }.distinctUntilChanged()

    fun toggleConnection() {
        Net.nativeToggleClient()
    }

    fun toggleSync() {
        Net.nativeToggleSync()
    }

    fun updateMickeyButton(enabled: Boolean) {
        Net.setMickeyState(enabled)
    }
}