package org.cf0x.rustnithm.Jour

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.cf0x.rustnithm.Data.Net
object JourBackend {
    fun validateIp(ip: String): Boolean {
        return ip.isNotBlank() && ip.all { it.isDigit() || it == '.' }
    }
    fun validatePort(port: String): Boolean {
        return port.toIntOrNull() in 0..65535
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
    }
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