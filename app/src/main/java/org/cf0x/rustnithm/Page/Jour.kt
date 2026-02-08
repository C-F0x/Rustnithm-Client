package org.cf0x.rustnithm.Page

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.cf0x.rustnithm.Data.DataManager
import org.cf0x.rustnithm.Data.Haptic
import org.cf0x.rustnithm.Data.Net
import org.cf0x.rustnithm.Data.TouchLogic
import org.cf0x.rustnithm.Theme.DefaultGameSkin
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.onFocusChanged

enum class ConnState { SUSPEND, WAITING, ACTIVE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Jour() {
    val context = LocalContext.current
    val view = LocalView.current
    val haptic = remember { Haptic.getInstance() }
    val scope = rememberCoroutineScope()

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

    val isSystemDark = isSystemInDarkTheme()
    var connState by remember { mutableStateOf(ConnState.SUSPEND) }

    var tempIp by remember { mutableStateOf("") }
    var tempPort by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    var isIpError by remember { mutableStateOf(false) }
    var isPortError by remember { mutableStateOf(false) }

    val accessCodes by dataManager.accessCodes.collectAsState()
    fun checkIpValid(ip: String) = ip.isNotBlank() && ip.all { it.isDigit() || it == '.' }
    fun checkPortValid(port: String) = port.toIntOrNull() in 0..65535
    LaunchedEffect(Unit) {
        while (true) {
            val rawState = Net.nativeGetState()
            val newState = when (rawState) {
                1 -> ConnState.ACTIVE
                2 -> ConnState.WAITING
                else -> ConnState.SUSPEND
            }
            if (connState != newState) {
                connState = newState
            }
            kotlinx.coroutines.delay(100)
        }
    }
    LaunchedEffect(savedIp, savedPort, protocolType) {
        if (tempIp.isEmpty() && savedIp.isNotEmpty()) {
            tempIp = savedIp
        }
        if (tempPort.isEmpty() && savedPort.isNotEmpty()) {
            tempPort = savedPort
        }
        if (checkIpValid(tempIp) && checkPortValid(tempPort)) {
            Net.updateConfig(tempIp, tempPort.toIntOrNull() ?: 0, protocolType)
        }
    }

    var activatedAir by remember { mutableStateOf(setOf<Int>()) }
    var activatedSlide by remember { mutableStateOf(setOf<Int>()) }
    var lastAir by remember { mutableStateOf(setOf<Int>()) }
    var lastSlide by remember { mutableStateOf(setOf<Int>()) }
    var touchPoints by remember { mutableStateOf(mapOf<PointerId, Offset>()) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var coinPressed by remember { mutableStateOf(false) }
    var servicePressed by remember { mutableStateOf(false) }
    var testPressed by remember { mutableStateOf(false) }
    var cardPressed by remember { mutableStateOf(false) }

    val isReallyDark = when (themeMode) {
        0 -> false
        1 -> true
        else -> isSystemDark
    }

    LaunchedEffect(connState, activatedAir, activatedSlide, coinPressed, servicePressed, testPressed, cardPressed) {
            Net.sendFullState(
                air = activatedAir,
                slide = activatedSlide,
                coin = coinPressed,
                service = servicePressed,
                test = testPressed,
                isCardActive = cardPressed,
                accessCode = accessCodes
            )
    }

    LaunchedEffect(activatedAir, activatedSlide, touchPoints) {
        if (isVibrationEnabled) {
            val newAir = activatedAir - lastAir
            val newSlide = activatedSlide - lastSlide

            if (newAir.isNotEmpty() || newSlide.isNotEmpty()) {
                haptic.onZoneActivated()
            } else if (touchPoints.isNotEmpty()) {
                haptic.onMoveSimulated()
            }
        }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 58.dp)
                .onSizeChanged { containerSize = it }
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val currentPoints = event.changes
                                .filter { it.pressed }
                                .associate { it.id to it.position }

                            touchPoints = currentPoints

                            if (containerSize.width > 0 && containerSize.height > 0) {
                                val totalW = containerSize.width.toFloat()
                                val totalH = containerSize.height.toFloat()
                                val airH = totalH * percentPage
                                val slideH = totalH - airH

                                activatedAir = TouchLogic.getActivatedAir(currentPoints.values, airH, multiA)
                                activatedSlide = TouchLogic.getActivatedSlide(currentPoints.values, totalW, airH, slideH, multiS)
                            }
                        }
                    }
                }
        ) {
            bgUri?.let {
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
                seedColor = Color(seedColorLong),
                isDark = isReallyDark
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .height(40.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(modifier = Modifier.width(110.dp))
            Surface(
                modifier = Modifier.weight(1.5f),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        modifier = Modifier.weight(1.0f).height(32.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = if (isIpError) BorderStroke(1.2.dp, MaterialTheme.colorScheme.error) else null
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.CenterStart) {
                            if (tempIp.isEmpty()) Text("IP", fontSize = 11.sp, color = Color.Gray)
                            BasicTextField(
                                value = tempIp,
                                onValueChange = {
                                    if (connState == ConnState.SUSPEND && it.length <= 15) {
                                        tempIp = it
                                        isIpError = !(it.isNotBlank() && it.all { c -> c.isDigit() || c == '.' })
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused && connState == ConnState.SUSPEND) {
                                            if (!isIpError && tempIp.isNotBlank()) {
                                                dataManager.updateTargetIp(tempIp)
                                            }
                                        }
                                    },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                ),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onDone = {
                                        if (!isIpError && tempIp.isNotBlank()) {
                                            dataManager.updateTargetIp(tempIp)
                                        }
                                        focusManager.clearFocus()
                                    }
                                ),
                                singleLine = true,
                                enabled = connState == ConnState.SUSPEND,
                                textStyle = TextStyle(
                                    fontSize = 12.sp,
                                    color = if (connState == ConnState.SUSPEND) MaterialTheme.colorScheme.onSurface else Color.Gray
                                )
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier.width(60.dp).height(32.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                        border = if (isPortError) BorderStroke(1.2.dp, MaterialTheme.colorScheme.error) else null
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.CenterStart) {
                            if (tempPort.isEmpty()) Text("Port", fontSize = 11.sp, color = Color.Gray)
                            BasicTextField(
                                value = tempPort,
                                onValueChange = {
                                    if (connState == ConnState.SUSPEND && it.length <= 5) {
                                        if (it.all { c -> c.isDigit() }) {
                                            tempPort = it
                                            val p = it.toIntOrNull()
                                            isPortError = p == null || p !in 0..65535
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (!focusState.isFocused && !isPortError && tempPort.isNotEmpty()) {
                                            dataManager.updateTargetPort(tempPort)
                                        }
                                    },
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number,
                                    imeAction = androidx.compose.ui.text.input.ImeAction.Done
                                ),
                                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                                    onDone = {
                                        if (!isPortError && tempPort.isNotEmpty()) {
                                            dataManager.updateTargetPort(tempPort)
                                        }
                                        focusManager.clearFocus()
                                    }
                                ),
                                singleLine = true,
                                enabled = connState == ConnState.SUSPEND,
                                textStyle = TextStyle(
                                    fontSize = 12.sp,
                                    color = if (connState == ConnState.SUSPEND) MaterialTheme.colorScheme.onSurface else Color.Gray
                                )
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(32.dp)
                            .pointerInput(protocolType, connState) {
                                detectTapGestures {
                                    if (connState == ConnState.SUSPEND) {
                                        dataManager.updateProtocolType(if (protocolType == 0) 1 else 0)
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (protocolType == 0) "UDP" else "TCP",
                            fontSize = 10.sp,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            color = if (connState != ConnState.SUSPEND) {
                                Color.Gray
                            } else {
                                if (protocolType == 0) MaterialTheme.colorScheme.primary else Color(0xFF00ACC1)
                            }
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onTap = {
                                        focusManager.clearFocus()
                                        Net.nativeToggleClient()
                                    },
                                    onLongPress = {
                                        focusManager.clearFocus()
                                        Net.nativeToggleSync()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when(connState) {
                                ConnState.ACTIVE -> Icons.Default.Link
                                ConnState.WAITING -> Icons.Default.Science
                                ConnState.SUSPEND -> Icons.Default.LinkOff
                            },
                            contentDescription = null,
                            tint = when(connState) {
                                ConnState.ACTIVE -> Color(0xFF4CAF50)
                                ConnState.WAITING -> Color(0xFFFFA000)
                                ConnState.SUSPEND -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val buttons = listOf(
                        Icons.Default.MonetizationOn to { v: Boolean -> coinPressed = v },
                        Icons.Default.Build to { v: Boolean -> servicePressed = v },
                        Icons.Default.Science to { v: Boolean -> testPressed = v },
                        Icons.Default.CreditCard to { v: Boolean -> cardPressed = v }
                    )

                    buttons.forEach { (icon, update) ->
                        val glowAlpha = remember { Animatable(0f) }
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .pointerInput(connState) {
                                    if (connState == ConnState.ACTIVE) {
                                        detectTapGestures(
                                            onPress = {
                                                try {
                                                    update(true)
                                                    if (isVibrationEnabled) haptic.onZoneActivated()
                                                    glowAlpha.snapTo(1f)
                                                    awaitRelease()
                                                } finally {
                                                    update(false)
                                                    scope.launch {
                                                        glowAlpha.animateTo(0f, tween(400))
                                                    }
                                                }
                                            }
                                        )
                                    }
                                }
                                .border(
                                    width = 1.5.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = glowAlpha.value),
                                    shape = CircleShape
                                ),
                            shape = CircleShape,
                            color = if (connState == ConnState.ACTIVE) {
                                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f + glowAlpha.value * 0.4f)
                            } else Color.Transparent
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = if (connState == ConnState.ACTIVE) {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    } else {
                                        Color.Gray.copy(alpha = 0.4f)
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.width(130.dp))
        }
    }
}