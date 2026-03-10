package org.cf0x.rustnithm.Jour

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.viewmodel.compose.viewModel
import org.cf0x.rustnithm.Data.DataManager
import org.cf0x.rustnithm.Data.Haptic
import org.cf0x.rustnithm.Emu.TankRush

enum class ConnState { SUSPEND, WAITING, ACTIVE }

@Composable
fun Jour() {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = remember { Haptic.getInstance() }
    val dataManager: DataManager = viewModel(factory = DataManager.Factory(context))
    val focusManager = LocalFocusManager.current

    var connState by remember { mutableStateOf<ConnState>(ConnState.SUSPEND) }
    var activatedAir by remember { mutableStateOf<Set<Int>>(setOf()) }
    var activatedSlide by remember { mutableStateOf<Set<Int>>(setOf()) }
    var coinPressed by remember { mutableStateOf(false) }
    var servicePressed by remember { mutableStateOf(false) }
    var testPressed by remember { mutableStateOf(false) }
    var cardPressed by remember { mutableStateOf(false) }
    var lastAir by remember { mutableStateOf<Set<Int>>(setOf()) }
    var lastSlide by remember { mutableStateOf<Set<Int>>(setOf()) }

    val savedIp by dataManager.targetIp.collectAsState()
    val savedPort by dataManager.targetPort.collectAsState()
    val bgUri by dataManager.backgroundImage.collectAsState()
    val percentPage by dataManager.percentPage.collectAsState()
    val multiA by dataManager.multiA.collectAsState()
    val multiS by dataManager.multiS.collectAsState()
    val isVibrationEnabled by dataManager.enableVibration.collectAsState()
    val protocolType by dataManager.protocolType.collectAsState()
    val airMode by dataManager.airMode.collectAsState()
    val accessCodes by dataManager.accessCodes.collectAsState()
    val flickThreshold by dataManager.flickThreshold.collectAsState()
    val flickEqualizerPlus by dataManager.flickEqualizerPlus.collectAsState()
    val flickEqualizerMinus by dataManager.flickEqualizerMinus.collectAsState()
    val flickUp by dataManager.flickUp.collectAsState()
    val flickDown by dataManager.flickDown.collectAsState()
    val flickZoneNum by dataManager.flickZoneNum.collectAsState()

    DisposableEffect(Unit) {
        haptic.attachView(view)
        onDispose {
            TankRush.stop()
            if (connState != ConnState.SUSPEND) {
                JourBackend.toggleConnection()
            }
        }
    }

    LaunchedEffect(Unit) {
        JourBackend.pollConnectionState().collect { newState ->
            connState = newState
        }
    }

    LaunchedEffect(connState) {
        if (connState != ConnState.SUSPEND) {
            TankRush.start(dataManager)
        } else {
            TankRush.stop()
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
        if (connState == ConnState.ACTIVE) {
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
        percentPage = percentPage,
        multiA = multiA,
        multiS = multiS,
        savedIp = savedIp,
        savedPort = savedPort,
        protocolType = protocolType,
        isVibrationEnabled = isVibrationEnabled,
        haptic = haptic,
        focusManager = focusManager,
        airMode = airMode,
        flickZoneNum = flickZoneNum,
        flickThreshold = flickThreshold,
        flickEqualizerPlus = flickEqualizerPlus,
        flickEqualizerMinus = flickEqualizerMinus,
        flickUp = flickUp,
        flickDown = flickDown,
        onActivatedChanged = { air, slide ->
            activatedAir = air
            activatedSlide = slide
        },
        onIpSaved = { ip -> dataManager.updateTargetIp(ip) },
        onPortSaved = { port -> dataManager.updateTargetPort(port) },
        onProtocolToggle = {
            dataManager.updateProtocolType(if (protocolType == 0) 1 else 0)
        },
        onConnectionToggle = { JourBackend.toggleConnection() },
        onConnectionSync = { JourBackend.toggleSync() },
        onCoinChanged = { coinPressed = it },
        onServiceChanged = { servicePressed = it },
        onTestChanged = { testPressed = it },
        onCardChanged = { cardPressed = it },
        onMickeyToggle = { enabled -> JourBackend.updateMickeyButton(enabled) }
    )
}