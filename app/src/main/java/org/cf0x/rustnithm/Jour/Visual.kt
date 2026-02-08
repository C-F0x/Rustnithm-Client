package org.cf0x.rustnithm.Jour

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import org.cf0x.rustnithm.Data.Haptic
import org.cf0x.rustnithm.Data.TouchLogic
import org.cf0x.rustnithm.Theme.DefaultGameSkin
@Composable
fun JourVisual(
    connState: ConnState,
    activatedAir: Set<Int>,
    activatedSlide: Set<Int>,

    backgroundUri: String?,
    seedColor: Color,
    isDark: Boolean,

    percentPage: Float,
    multiA: Float,
    multiS: Float,

    savedIp: String,
    savedPort: String,
    protocolType: Int,

    isVibrationEnabled: Boolean,
    haptic: Haptic,
    focusManager: FocusManager,

    onActivatedChanged: (air: Set<Int>, slide: Set<Int>) -> Unit,

    onIpSaved: (String) -> Unit,
    onPortSaved: (String) -> Unit,
    onProtocolToggle: () -> Unit,

    onConnectionToggle: () -> Unit,
    onConnectionSync: () -> Unit,

    onCoinChanged: (Boolean) -> Unit,
    onServiceChanged: (Boolean) -> Unit,
    onTestChanged: (Boolean) -> Unit,
    onCardChanged: (Boolean) -> Unit,
    onMickeyToggle: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()

    var tempIp by remember(savedIp) { mutableStateOf(savedIp) }
    var tempPort by remember(savedPort) { mutableStateOf(savedPort) }
    var isIpError by remember { mutableStateOf(false) }
    var isPortError by remember { mutableStateOf(false) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var touchPoints by remember { mutableStateOf(mapOf<PointerId, Offset>()) }

    var lastAir by remember { mutableStateOf(setOf<Int>()) }
    var lastSlide by remember { mutableStateOf(setOf<Int>()) }

    var isMickeyEnabled by remember { mutableStateOf(false) }
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 58.dp)
                .onSizeChanged { containerSize = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
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

                                val newAir = TouchLogic.getActivatedAir(currentPoints.values, airH, multiA)
                                val newSlide = TouchLogic.getActivatedSlide(currentPoints.values, totalW, airH, slideH, multiS)

                                onActivatedChanged(newAir, newSlide)
                            }
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
                seedColor = seedColor,
                isDark = isDark
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
                                                onIpSaved(tempIp)
                                            }
                                        }
                                    },
                                keyboardOptions = KeyboardOptions(
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (!isIpError && tempIp.isNotBlank()) {
                                            onIpSaved(tempIp)
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
                                            onPortSaved(tempPort)
                                        }
                                    },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        if (!isPortError && tempPort.isNotEmpty()) {
                                            onPortSaved(tempPort)
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
                                        onProtocolToggle()
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
                                        onConnectionToggle()
                                    },
                                    onLongPress = {
                                        focusManager.clearFocus()
                                        onConnectionSync()
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
                modifier = Modifier.weight(1.1f),
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
                        Icons.Default.MonetizationOn to onCoinChanged,
                        Icons.Default.Build to onServiceChanged,
                        Icons.Default.Science to onTestChanged,
                        Icons.Default.CreditCard to onCardChanged
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
                    var isMickeyEnabled by remember { mutableStateOf(false) }
                    val mickeyGlow = remember { Animatable(0f) }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(28.dp)
                            .pointerInput(connState) {
                                if (connState == ConnState.ACTIVE) {
                                    detectTapGestures(
                                        onTap = {
                                            isMickeyEnabled = !isMickeyEnabled
                                            onMickeyToggle(isMickeyEnabled)
                                            if (isVibrationEnabled) haptic.onZoneActivated()
                                            scope.launch {
                                                mickeyGlow.snapTo(1f)
                                                mickeyGlow.animateTo(0f, tween(600))
                                            }
                                        }
                                    )
                                }
                            }
                            .border(
                                width = 1.5.dp,
                                color = if (isMickeyEnabled) Color(0xFFFFD700) else MaterialTheme.colorScheme.primary.copy(alpha = mickeyGlow.value),
                                shape = CircleShape
                            ),
                        shape = CircleShape,
                        color = if (isMickeyEnabled) {
                            Color(0xFFFFD700).copy(alpha = 0.25f)
                        } else if (connState == ConnState.ACTIVE) {
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                        } else Color.Transparent
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "M",
                                fontSize = 13.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Black,
                                color = if (isMickeyEnabled) Color(0xFFFFD700) else if (connState == ConnState.ACTIVE) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else Color.Gray.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(130.dp))
        }
    }
}