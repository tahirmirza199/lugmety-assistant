package com.lugmety.orderassistant.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lugmety.orderassistant.service.FloatingControlPanelService
import com.lugmety.orderassistant.service.LugmetyAccessibilityService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutoClickerScreen(navController: NavController) {
    val context = LocalContext.current
    var isOverlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var clickIntervalMs by remember { mutableLongStateOf(1000L) }
    var gestureDurationMs by remember { mutableLongStateOf(50L) }
    var clickMode by remember { mutableStateOf("SINGLE_POINT") } // SINGLE_POINT or MULTI_POINT

    val clickerStatus by (LugmetyAccessibilityService.instance?.clickerStatus
        ?: remember { mutableStateOf(LugmetyAccessibilityService.ClickerStatus.STOPPED) }).collectAsState()

    val totalClicks by (LugmetyAccessibilityService.instance?.totalClickCount
        ?: remember { mutableIntStateOf(0) }).collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auto Clicker Module") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B132B),
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFF0B132B))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Overlay Permission Check Banner
            if (!isOverlayGranted) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541)),
                    border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF43F5E)))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF43F5E))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Display Over Other Apps Permission Needed",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "The movable Floating Control Panel requires system alert window permission to appear over the Lugmety driver app.",
                            color = Color(0xFF90E0EF),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:" + context.packageName)
                                    )
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6))
                        ) {
                            Text("Grant Overlay Permission")
                        }
                    }
                }
            }

            // Floating Controller Launch Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF48CAE4)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Floating Control Panel", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text(
                            text = if (clickerStatus == LugmetyAccessibilityService.ClickerStatus.RUNNING) "RUNNING" else "STOPPED",
                            color = if (clickerStatus == LugmetyAccessibilityService.ClickerStatus.RUNNING) Color(0xFF00F5D4) else Color(0xFF90E0EF),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Spawns a draggable floating controller with target pins directly onto your phone screen.",
                        color = Color(0xFF90E0EF),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                isOverlayGranted = Settings.canDrawOverlays(context)
                                if (isOverlayGranted) {
                                    FloatingControlPanelService.start(context)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Open Overlay")
                        }

                        OutlinedButton(
                            onClick = { FloatingControlPanelService.stop(context) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Close Overlay", color = Color.White)
                        }
                    }
                }
            }

            // Controls & Metrics
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Auto Clicker Controls", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { LugmetyAccessibilityService.instance?.startAutoClicker() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00F5D4)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Start", color = Color(0xFF0B132B))
                        }

                        Button(
                            onClick = { LugmetyAccessibilityService.instance?.pauseAutoClicker() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A506B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Pause", color = Color.White)
                        }

                        Button(
                            onClick = { LugmetyAccessibilityService.instance?.stopAutoClicker() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF881337)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Stop", color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Clicks: $totalClicks", color = Color(0xFF48CAE4), style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { LugmetyAccessibilityService.instance?.resetClickCounter() }) {
                            Text("Reset Count", color = Color(0xFF90E0EF))
                        }
                    }
                }
            }

            // Click Mode & Interval Config
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Click Configuration", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Click Mode", color = Color(0xFF90E0EF), style = MaterialTheme.typography.bodySmall)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = clickMode == "SINGLE_POINT",
                            onClick = { clickMode = "SINGLE_POINT" },
                            label = { Text("Single Point") }
                        )
                        FilterChip(
                            selected = clickMode == "MULTI_POINT",
                            onClick = { clickMode = "MULTI_POINT" },
                            label = { Text("Multi-Point Sequence") }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Click Interval: ${clickIntervalMs} ms", color = Color.White)
                    Slider(
                        value = clickIntervalMs.toFloat(),
                        onValueChange = { clickIntervalMs = it.toLong() },
                        valueRange = 100f..5000f,
                        steps = 20
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Gesture Press Duration: ${gestureDurationMs} ms", color = Color.White)
                    Slider(
                        value = gestureDurationMs.toFloat(),
                        onValueChange = { gestureDurationMs = it.toLong() },
                        valueRange = 25f..300f,
                        steps = 11
                    )
                }
            }
        }
    }
}
