package org.cf0x.rustnithm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import org.cf0x.rustnithm.Bon.Bon
import org.cf0x.rustnithm.Data.DataManager
import org.cf0x.rustnithm.Jour.Jour
import org.cf0x.rustnithm.Theme.RustnithmTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

            RustnithmTheme(
                themeMode = themeMode,
                useDynamicColor = useDynamicColor,
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
    var selectedPage by remember { mutableIntStateOf(0) }
    var jourResetKey by remember { mutableIntStateOf(0) }

    Scaffold { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.padding(paddingValues)) {
                Crossfade(targetState = selectedPage, label = "pageTransition") { page ->
                    when (page) {
                        0 -> Bon()
                        1 -> key(jourResetKey) { Jour() }
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Text(
                        text = "Rustnithm",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        FilterChip(
                            modifier = Modifier.height(32.dp),
                            selected = selectedPage == 0,
                            onClick = {
                                selectedPage = 0
                                jourResetKey++
                            },
                            label = { Text("Bon") },
                            shape = CircleShape,
                            border = null
                        )
                        FilterChip(
                            modifier = Modifier.height(32.dp),
                            selected = selectedPage == 1,
                            onClick = { selectedPage = 1 },
                            label = { Text("Jour") },
                            shape = CircleShape,
                            border = null
                        )
                    }
                }
            }
        }
    }
}