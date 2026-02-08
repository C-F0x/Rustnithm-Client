package org.cf0x.rustnithm.Jour

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import org.cf0x.rustnithm.Data.DataManager
import org.cf0x.rustnithm.Data.Haptic

enum class ConnState { SUSPEND, WAITING, ACTIVE }
@Composable
fun Jour() {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = remember { Haptic.getInstance() }

    LaunchedEffect(view) {
        haptic.attachView(view)
    }

    val dataManager: DataManager = viewModel(factory = DataManager.Factory(context))
    val savedIp by dataManager.targetIp.collectAsState()
    val savedPort by dataManager.targetPort.collectAsState()
    val bgUri by dataManager.backgroundImage.collectAsState()
    val percentPage by dataManager.percentPage.collectAsState()
    val multiA by dataManager.multiA.collectAsState()
    val multiS by dataManager.multiS.collectAsState()
    val seedColorLong by dataManager.seedColor.collectAsState()
    val themeMode by dataManager.themeMode.collectAsState()
    val isVibrationEnabled by dataManager.enableVibration.collectAsState()
    val protocolType by dataManager.protocolType.collectAsState()
    val airMode by dataManager.airMode.collectAsState()
    val accessCodes by dataManager.accessCodes.collectAsState()

    val focusManager = LocalFocusManager.current
    val isSystemDark = isSystemInDarkTheme()

    var connState by remember { mutableStateOf(ConnState.SUSPEND) }
    var activatedAir by remember { mutableStateOf(setOf<Int>()) }
    var activatedSlide by remember { mutableStateOf(setOf<Int>()) }
    var coinPressed by remember { mutableStateOf(false) }
    var servicePressed by remember { mutableStateOf(false) }
    var testPressed by remember { mutableStateOf(false) }
    var cardPressed by remember { mutableStateOf(false) }

    var lastAir by remember { mutableStateOf(setOf<Int>()) }
    var lastSlide by remember { mutableStateOf(setOf<Int>()) }

    val isReallyDark = when (themeMode) {
        0 -> false
        1 -> true
        else -> isSystemDark
    }

    LaunchedEffect(Unit) {
        JourBackend.pollConnectionState().collect { newState ->
            connState = newState
        }
    }

    LaunchedEffect(savedIp, savedPort, protocolType) {
        if (savedIp.isNotEmpty() && savedPort.isNotEmpty()) {
            val port = savedPort.toIntOrNull() ?: 0
            if (JourBackend.validateIp(savedIp) && JourBackend.validatePort(savedPort)) {
                JourBackend.updateNetworkConfig(savedIp, port, protocolType)
            }
        }
    }


    LaunchedEffect(connState, activatedAir, activatedSlide, coinPressed, servicePressed, testPressed, cardPressed) {
        JourBackend.sendGameState(
            air = activatedAir,
            airMode = airMode,
            slide = activatedSlide,
            coin = coinPressed,
            service = servicePressed,
            test = testPressed,
            card = cardPressed,
            accessCode = accessCodes
        )
    }

    LaunchedEffect(activatedAir, activatedSlide) {
        if (isVibrationEnabled) {
            val newAir = activatedAir - lastAir
            val newSlide = activatedSlide - lastSlide

            if (newAir.isNotEmpty() || newSlide.isNotEmpty()) {
                haptic.onZoneActivated()
            }
        }
        lastAir = activatedAir
        lastSlide = activatedSlide
    }

    JourVisual(
        connState = connState,
        activatedAir = activatedAir,
        activatedSlide = activatedSlide,

        backgroundUri = bgUri,
        seedColor = Color(seedColorLong),
        isDark = isReallyDark,

        percentPage = percentPage,
        multiA = multiA,
        multiS = multiS,

        savedIp = savedIp,
        savedPort = savedPort,
        protocolType = protocolType,

        isVibrationEnabled = isVibrationEnabled,
        haptic = haptic,
        focusManager = focusManager,

        onActivatedChanged = { air, slide ->
            activatedAir = air
            activatedSlide = slide
        },

        onIpSaved = { ip ->
            dataManager.updateTargetIp(ip)
        },
        onPortSaved = { port ->
            dataManager.updateTargetPort(port)
        },
        onProtocolToggle = {
            dataManager.updateProtocolType(if (protocolType == 0) 1 else 0)
        },

        onConnectionToggle = {
            JourBackend.toggleConnection()
        },
        onConnectionSync = {
            JourBackend.toggleSync()
        },

        onCoinChanged = { coinPressed = it },
        onServiceChanged = { servicePressed = it },
        onTestChanged = { testPressed = it },
        onCardChanged = { cardPressed = it },
        onMickeyToggle = { enabled ->
            JourBackend.updateMickeyButton(enabled)
        }
    )
}