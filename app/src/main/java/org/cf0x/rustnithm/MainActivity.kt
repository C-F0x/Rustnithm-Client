package org.cf0x.rustnithm

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.cf0x.rustnithm.Bon.Bon
import org.cf0x.rustnithm.Data.DataManager
import org.cf0x.rustnithm.Data.Haptic
import org.cf0x.rustnithm.Jour.ConnState
import org.cf0x.rustnithm.Jour.Jour
import org.cf0x.rustnithm.Jour.JourBackend
import org.cf0x.rustnithm.Theme.RustnithmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.attributes = window.attributes.also { attrs ->
            attrs.preferredDisplayModeId = display?.supportedModes
                ?.maxByOrNull { it.refreshRate }?.modeId ?: 0
        }

        val performanceHintManager = getSystemService(android.os.PerformanceHintManager::class.java)
        val mainThread = intArrayOf(android.os.Process.myTid())
        performanceHintManager?.createHintSession(mainThread, 8_000_000L)

        enableEdgeToEdge()

        WindowCompat.getInsetsController(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        setContent {
            val context = LocalContext.current
            val dataManager: DataManager = viewModel(factory = DataManager.Factory(context))
            val themeMode by dataManager.themeMode.collectAsState()
            val useDynamicColor by dataManager.useDynamicColor.collectAsState()
            val seedColorLong by dataManager.seedColor.collectAsState()
            val useExpressive by dataManager.useExpressive.collectAsState()
            val language by dataManager.language.collectAsState()

            LaunchedEffect(language) {
                // Same pattern as konamiku's LocaleHelper: API 33+ uses
                // LocaleManager (no activity recreation); older devices fall
                // back to AppCompatDelegate (may reconstruct the activity).
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    runCatching {
                        val localeManager =
                            getSystemService(android.app.LocaleManager::class.java)
                                ?: return@runCatching

                        println("DEBUG: Requesting language: $language")
                        println(
                            "DEBUG: Current locales before: " +
                                localeManager.applicationLocales.toLanguageTags()
                        )

                        localeManager.applicationLocales = if (language == "system") {
                            android.os.LocaleList.getEmptyLocaleList()
                        } else {
                            android.os.LocaleList.forLanguageTags(language)
                        }
                    }
                } else if (language != "system") {
                    AppCompatDelegate.setApplicationLocales(
                        LocaleListCompat.forLanguageTags(language)
                    )
                }
            }

            RustnithmTheme(
                themeMode = themeMode,
                useDynamicColor = useDynamicColor,
                useExpressive = useExpressive,
                customSeedColor = Color(seedColorLong)
            ) {
                MainScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val context = LocalContext.current
    val dataManager: DataManager = viewModel(factory = DataManager.Factory(context))
    val haptic = remember { Haptic.getInstance() }

    var selectedPage by remember { mutableIntStateOf(0) }
    var jourResetKey by remember { mutableIntStateOf(0) }
    var controlsExpanded by remember { mutableStateOf(false) }

    // Control state lifted from Jour so the six buttons can live in the top
    // bar, next to the Bon/Jour switcher.
    var connState by remember { mutableStateOf(ConnState.SUSPEND) }
    var coinPressed by remember { mutableStateOf(false) }
    var servicePressed by remember { mutableStateOf(false) }
    var testPressed by remember { mutableStateOf(false) }
    var cardPressed by remember { mutableStateOf(false) }
    var isMickeyEnabled by remember { mutableStateOf(false) }
    val isVibrationEnabled by dataManager.enableVibration.collectAsState()
    val airMode by dataManager.airMode.collectAsState()

    LaunchedEffect(Unit) {
        JourBackend.pollConnectionState().collect { connState = it }
    }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(paddingValues)) {
                Crossfade(targetState = selectedPage, label = "pageTransition") { page ->
                    when (page) {
                        0 -> Bon()
                        1 -> key(jourResetKey) {
                            Jour(
                                connState = connState,
                                coinPressed = coinPressed,
                                servicePressed = servicePressed,
                                testPressed = testPressed,
                                cardPressed = cardPressed,
                                onCoinChanged = { coinPressed = it },
                                onServiceChanged = { servicePressed = it },
                                onTestChanged = { testPressed = it },
                                onCardChanged = { cardPressed = it }
                            )
                        }
                    }
                }
            }

            // Top bar: Bon/Jour switcher + the six control buttons on the same
            // row, dodging status bar and notch/cutout via safeDrawingPadding.
            // Fixed height keeps the chips pixel-stable when the controls expand.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeDrawingPadding()
                    .height(48.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.height(44.dp).zIndex(1f),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxHeight().padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            modifier = Modifier.height(32.dp),
                            selected = selectedPage == 0,
                            onClick = {
                                selectedPage = 0
                                jourResetKey++
                                controlsExpanded = false
                            },
                            label = { Text("Bon") },
                            shape = CircleShape,
                            border = null
                        )
                        FilterChip(
                            modifier = Modifier.height(32.dp),
                            selected = selectedPage == 1,
                            onClick = {
                                if (selectedPage == 1) {
                                    // Second tap on Jour while already on Jour:
                                    // expand/collapse the six control buttons.
                                    controlsExpanded = !controlsExpanded
                                } else {
                                    selectedPage = 1
                                }
                            },
                            label = { Text("Jour") },
                            shape = CircleShape,
                            border = null
                        )
                    }
                }

                // Always composed (never added/removed) so its RenderNode outline
                // and shadow exist from the start: visibility is animated via
                // graphicsLayer, and the pill slides out from under the Bon/Jour
                // chips (zIndex below them) along +X.
                ControlsRow(
                    expanded = controlsExpanded && selectedPage == 1,
                    connState = connState,
                    showMickey = airMode >= 2,
                    isVibrationEnabled = isVibrationEnabled,
                    haptic = haptic,
                    isMickeyEnabled = isMickeyEnabled,
                    onCoinChanged = { coinPressed = it },
                    onServiceChanged = { servicePressed = it },
                    onTestChanged = { testPressed = it },
                    onCardChanged = { cardPressed = it },
                    onMickeyToggle = {
                        isMickeyEnabled = it
                        JourBackend.updateMickeyButton(it)
                    },
                    onConnectionToggle = { JourBackend.toggleConnection() },
                    onConnectionSync = { JourBackend.toggleSync() }
                )
            }
        }
    }
}

/**
 * The six control buttons (connect / coin / test / service / card / M),
 * rendered inline in the top bar next to the Bon/Jour switcher. Expanded by a
 * second tap on the Jour chip while on the Jour page.
 */
@Composable
private fun ControlsRow(
    expanded: Boolean,
    connState: ConnState,
    showMickey: Boolean,
    isVibrationEnabled: Boolean,
    haptic: Haptic,
    isMickeyEnabled: Boolean,
    onCoinChanged: (Boolean) -> Unit,
    onServiceChanged: (Boolean) -> Unit,
    onTestChanged: (Boolean) -> Unit,
    onCardChanged: (Boolean) -> Unit,
    onMickeyToggle: (Boolean) -> Unit,
    onConnectionToggle: () -> Unit,
    onConnectionSync: () -> Unit
) {
    val anim = remember { Animatable(if (expanded) 1f else 0f) }
    var width by remember { mutableIntStateOf(0) }

    LaunchedEffect(expanded) {
        if (expanded) {
            anim.animateTo(1f, tween(250))
        } else {
            anim.animateTo(0f, tween(200))
        }
    }

    Box(
        modifier = Modifier
            .onSizeChanged { width = it.width }
            .zIndex(0f)
            .graphicsLayer {
                alpha = anim.value
                translationX = -width * (1f - anim.value)
            }
            .layout { measurable, constraints ->
                if (anim.value <= 0f) {
                    // Collapsed: don't occupy any space in the top bar row.
                    layout(0, 0) {}
                } else {
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
            }
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ControlButton(
                size = 32.dp,
                content = {
                    Icon(
                        imageVector = when (connState) {
                            ConnState.ACTIVE -> Icons.Default.Link
                            ConnState.WAITING -> Icons.Default.Science
                            ConnState.SUSPEND -> Icons.Default.LinkOff
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = when (connState) {
                            ConnState.ACTIVE -> Color(0xFF4CAF50)
                            ConnState.WAITING -> Color(0xFFFFA000)
                            ConnState.SUSPEND -> MaterialTheme.colorScheme.error
                        }
                    )
                },
                onClick = onConnectionToggle,
                onLongClick = onConnectionSync
            )

            Surface(
                modifier = Modifier.shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(22.dp),
                    clip = true
                ),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val active = connState == ConnState.ACTIVE
            PressControlButton(
                icon = Icons.Default.MonetizationOn,
                enabled = active,
                isVibrationEnabled = isVibrationEnabled,
                haptic = haptic,
                onChanged = onCoinChanged
            )
            PressControlButton(
                icon = Icons.Default.Build,
                enabled = active,
                isVibrationEnabled = isVibrationEnabled,
                haptic = haptic,
                onChanged = onServiceChanged
            )
            PressControlButton(
                icon = Icons.Default.Science,
                enabled = active,
                isVibrationEnabled = isVibrationEnabled,
                haptic = haptic,
                onChanged = onTestChanged
            )
            PressControlButton(
                icon = Icons.Default.CreditCard,
                enabled = active,
                isVibrationEnabled = isVibrationEnabled,
                haptic = haptic,
                onChanged = onCardChanged
            )

                    if (showMickey) {
                        MickeyButton(
                            enabled = active,
                            isMickeyEnabled = isMickeyEnabled,
                            isVibrationEnabled = isVibrationEnabled,
                            haptic = haptic,
                            onMickeyToggle = onMickeyToggle
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlButton(
    size: Dp,
    content: @Composable () -> Unit,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .size(size)
            .pointerInput(onClick, onLongClick) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongClick?.invoke() }
                )
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun PressControlButton(
    icon: ImageVector,
    enabled: Boolean,
    isVibrationEnabled: Boolean,
    haptic: Haptic,
    onChanged: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val glowAlpha = remember { Animatable(0f) }

    Surface(
        modifier = Modifier
            .size(32.dp)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onPress = {
                            try {
                                onChanged(true)
                                if (isVibrationEnabled) haptic.onZoneActivated()
                                glowAlpha.snapTo(1f)
                                awaitRelease()
                            } finally {
                                onChanged(false)
                                scope.launch { glowAlpha.animateTo(0f, tween(400)) }
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
        color = if (enabled) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f + glowAlpha.value * 0.4f)
        } else Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (enabled) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else Color.Gray.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
private fun MickeyButton(
    enabled: Boolean,
    isMickeyEnabled: Boolean,
    isVibrationEnabled: Boolean,
    haptic: Haptic,
    onMickeyToggle: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val mickeyGlow = remember { Animatable(0f) }
    // The pointerInput block below only restarts on `enabled` changes, so a
    // captured `isMickeyEnabled` would go stale (taps would keep flipping the
    // same value: sometimes can't open, sometimes can't close). rememberUpdatedState
    // always reads the latest value instead.
    val currentMickey by rememberUpdatedState(isMickeyEnabled)

    Surface(
        modifier = Modifier
            .size(32.dp)
            .pointerInput(enabled) {
                if (enabled) {
                    detectTapGestures(
                        onTap = {
                            onMickeyToggle(!currentMickey)
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
                color = if (isMickeyEnabled) {
                    Color(0xFFFFD700)
                } else {
                    MaterialTheme.colorScheme.primary.copy(alpha = mickeyGlow.value)
                },
                shape = CircleShape
            ),
        shape = CircleShape,
        color = if (isMickeyEnabled) {
            Color(0xFFFFD700).copy(alpha = 0.25f)
        } else if (enabled) {
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        } else Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "M",
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                color = if (isMickeyEnabled) {
                    Color(0xFFFFD700)
                } else if (enabled) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else Color.Gray.copy(alpha = 0.4f)
            )
        }
    }
}
