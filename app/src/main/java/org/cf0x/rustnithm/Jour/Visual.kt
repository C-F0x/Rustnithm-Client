package org.cf0x.rustnithm.Jour

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import org.cf0x.rustnithm.Data.Haptic
import org.cf0x.rustnithm.Data.TouchLogic
import org.cf0x.rustnithm.Theme.DefaultGameSkin

@Composable
fun JourVisual(
    connState: ConnState,
    activatedAir: Set<Int>,
    activatedSlide: Set<Int>,

    backgroundUri: String?,
    percentPage: Float,
    multiA: Float,
    multiS: Float,

    isVibrationEnabled: Boolean,
    haptic: Haptic,
    focusManager: FocusManager,

    onActivatedChanged: (air: Set<Int>, slide: Set<Int>) -> Unit,

    airMode: Int,

    flickZoneNum: Int,
    flickEqualizerPlus: Int,
    flickEqualizerMinus: Int,
    flickUp: Int,
    flickDown: Int
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var touchPoints by remember { mutableStateOf(mapOf<PointerId, Offset>()) }

    var lastAir by remember { mutableStateOf(setOf<Int>()) }
    var lastSlide by remember { mutableStateOf(setOf<Int>()) }

    LaunchedEffect(activatedAir, activatedSlide, touchPoints) {
        if (isVibrationEnabled) {
            val newAir = activatedAir - lastAir
            val newSlide = activatedSlide - lastSlide

            if (newAir.isEmpty() && newSlide.isEmpty() && touchPoints.isNotEmpty()) {
                haptic.onMoveSimulated()
            }
        }
        lastAir = activatedAir
        lastSlide = activatedSlide
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = {
                    focusManager.clearFocus()
                })
            }
    ) {
        val pointerMapping = remember { IntArray(10) { -1 } }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 58.dp)
                .onSizeChanged { containerSize = it }
                .pointerInput(airMode, flickEqualizerPlus, flickEqualizerMinus, flickUp, flickDown, flickZoneNum) {
                    awaitEachGesture {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (airMode == 2) {
                                event.changes.forEach { change ->
                                    val pId = change.id.hashCode()
                                    if (change.pressed) {
                                        var poolIdx = pointerMapping.indexOf(pId)
                                        if (poolIdx == -1) {
                                            poolIdx = pointerMapping.indexOf(-1)
                                            if (poolIdx != -1) pointerMapping[poolIdx] = pId
                                        }
                                        if (poolIdx != -1) {
                                            val yCoord = change.position.y.toInt()
                                            org.cf0x.rustnithm.Data.Net.updateFlickCoords(poolIdx, yCoord)
                                            org.cf0x.rustnithm.Emu.TankManager.updateFlick(
                                                index = poolIdx,
                                                y = yCoord,
                                                zonesNum = flickZoneNum,
                                                up = flickUp,
                                                down = flickDown,
                                                containerHeight = containerSize.height
                                            )
                                        }
                                    } else {
                                        val poolIdx = pointerMapping.indexOf(pId)
                                        if (poolIdx != -1) {
                                            pointerMapping[poolIdx] = -1
                                            org.cf0x.rustnithm.Data.Net.updateFlickCoords(poolIdx, -1)
                                            org.cf0x.rustnithm.Emu.TankManager.updateFlick(
                                                index = poolIdx,
                                                y = -1,
                                                zonesNum = flickZoneNum,
                                                up = flickUp,
                                                down = flickDown,
                                                containerHeight = containerSize.height
                                            )
                                        }
                                    }
                                }
                            }
                            val allCurrentPoints = event.changes
                                .filter { it.pressed }
                                .map { it.position }

                            touchPoints = event.changes
                                .filter { it.pressed }
                                .associate { it.id to it.position }

                            if (containerSize.width > 0 && containerSize.height > 0) {
                                val totalW = containerSize.width.toFloat()
                                val totalH = containerSize.height.toFloat()
                                val airH = totalH * percentPage
                                val slideH = totalH - airH
                                val newAir = TouchLogic.getActivatedAir(allCurrentPoints, airH, multiA, airMode)
                                val newSlide = TouchLogic.getActivatedSlide(allCurrentPoints, totalW, airH, slideH, multiS)
                                onActivatedChanged(newAir, newSlide)
                            }
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
        ) {
            backgroundUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    alpha = 0.4f
                )
            }
            DefaultGameSkin(
                activatedAir = activatedAir,
                activatedSlide = activatedSlide,
                airWeight = percentPage,
                slideWeight = 1f - percentPage,
                multiA = multiA,
                multiS = multiS,
                touchPoints = touchPoints,
                airMode = airMode
            )
            if (airMode == 2) {
                Substratum(
                    flickZoneNum = flickZoneNum,
                    touchPoints = touchPoints
                )
            }
        }

    }
}
