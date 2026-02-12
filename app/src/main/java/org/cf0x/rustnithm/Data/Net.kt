package org.cf0x.rustnithm.Data

import android.util.Log

object Net {
    const val STATE_SUSPEND = 0
    const val STATE_ACTIVE = 1
    const val STATE_WAITING = 2

    init {
        try {
            System.loadLibrary("rustnithm")
            nativeInit()
            Log.d("Net", "Rustnithm engine initialized.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("Net", "Failed to load rustnithm library", e)
        }
    }

    private external fun nativeInit()
    private external fun nativeUpdateConfig(ip: String, port: Int, protocolType: Int)
    external fun nativeGetState(): Int
    external fun nativeToggleClient()
    external fun nativeToggleSync()

    external fun nativeUpdateFlickCoords(index: Int, y: Int)
    external fun nativeTouchDown(pid: Int, y: Int)
    external fun nativeTouchUp(pid: Int)

    private external fun nativeUpdateState(
        packetType: Int,
        buttonMask: Int,
        airByte: Int,
        sliderMask: Int,
        handshakePayload: Int,
        cardBcd: ByteArray?,
        airMode: Int
    )

    private external fun nativeMickeyButton(enabled: Int)

    fun updateConfig(ip: String, port: Int, protocolType: Int) {
        nativeUpdateConfig(ip, port, protocolType)
    }

    fun setMickeyState(enabled: Boolean) {
        nativeMickeyButton(if (enabled) 1 else 0)
    }

    fun onTouchDown(pid: Int, y: Float) {
        nativeTouchDown(pid, y.toInt())
    }

    fun onTouchMove(pid: Int, y: Float) {
        nativeUpdateFlickCoords(pid, y.toInt())
    }

    fun onTouchUp(pid: Int) {
        nativeTouchUp(pid)
    }

    fun sendFullState(
        air: Set<Int>,
        airMode: Int,
        slide: Set<Int>,
        coin: Boolean,
        service: Boolean,
        test: Boolean,
        isCardActive: Boolean,
        accessCode: String
    ) {
        if (isCardActive && accessCode.length == 20) {
            val bcd = ByteArray(10)
            try {
                for (i in 0 until 10) {
                    val high = accessCode[i * 2].digitToInt(16)
                    val low = accessCode[i * 2 + 1].digitToInt(16)
                    bcd[i] = ((high shl 4) or low).toByte()
                }
                nativeUpdateState(48, 0, 0, 0, 0, bcd, airMode)
                return
            } catch (e: Exception) {
                Log.e("Net", "Access code format error")
            }
        }

        if (coin || service || test) {
            var mask = 0
            if (coin) mask = mask or 0x01
            if (service) mask = mask or 0x02
            if (test) mask = mask or 0x04
            nativeUpdateState(16, mask, 0, 0, 0, null, airMode)
        } else {
            var airByte = 0
            if (airMode == 1) {
                for (id in air) {
                    val bitIndex = id - 1
                    if (bitIndex in 0..5) {
                        airByte = airByte or (1 shl bitIndex)
                    }
                }
            }
            var sliderMask = 0
            for (id in slide) {
                val adjustedId = id - 1
                if (adjustedId in 0..31) {
                    sliderMask = sliderMask or (1 shl adjustedId)
                }
            }
            nativeUpdateState(32, 0, airByte, sliderMask, 0, null, airMode)
        }
    }
}