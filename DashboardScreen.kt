package com.lugmety.orderassistant.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lugmety.orderassistant.service.LugmetyAccessibilityService
import com.lugmety.orderassistant.service.MonitoringForegroundService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: NavController) {
    val context = LocalContext.current
    var isMonitoring by remember { mutableStateOf(false) }
    var autoRefresh by remember { mutableStateOf(true) }
    var orderDetection by remember { mutableStateOf(true) }
    var refreshInterval by remember { mutableFloatStateOf(8f) }
    var cooldownInterval by remember { mutableFloatStateOf(4f) }

    val isAccessibilityActive = LugmetyAccessibilityService.isServiceConnected

    val monitoringStatus by (LugmetyAccessibilityService.instance?.monitoringStatus
        ?: remember { mutableStateOf(LugmetyAccessibilityService.MonitoringStatus.STOPPED) }).collectAsState()

    val refreshCount by (LugmetyAccessibilityService.instance?.refreshCount
        ?: remember { mutableIntStateOf(0) }).collectAsState()

    val detectedCount by (LugmetyAccessibilityService.instance?.detectedOrderCount
        ?: remember { mutableIntStateOf(0) }).collectAsState()

    val failedCount by (LugmetyAccessibilityService.instance?.failedAttemptsCount
        ?: remember { mutableIntStateOf(0) }).collectAsState()

    val clickerStatus by (LugmetyAccessibilityService.instance?.clickerStatus
        ?: remember { mutableStateOf(LugmetyAccessibilityService.ClickerStatus.STOPPED) }).collectAsState()

    val totalClicks by (LugmetyAccessibilityService.instance?.totalClickCount
        ?: remember { mutableIntStateOf(0) }).collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Lugmety Order Assistant", color = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAccessibilityActive) "● Active" else "○ Inactive",
                            color = if (isAccessibilityActive) Color(0xFF00F5D4) else Color(0xFFF43F5E),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0B132B),
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { navController.navigate("autoclicker") }) {
                        Icon(Icons.Default.AdsClick, contentDescription = "Auto Clicker", tint = Color(0xFF48CAE4))
                    }
                    IconButton(onClick = { navController.navigate("setup") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Accessibility Setup", tint = Color.White)
                    }
                    IconButton(onClick = { navController.navigate("diagnostics") }) {
                        Icon(Icons.Default.BugReport, contentDescription = "Diagnostics", tint = Color(0xFF90E0EF))
                    }
                }
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
            // 1. Accessibility Status Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541)),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(
                        if (isAccessibilityActive) Color(0xFF00F5D4) else Color(0xFFF43F5E)
                    )
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isAccessibilityActive) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (isAccessibilityActive) Color(0xFF00F5D4) else Color(0xFFF43F5E)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (isAccessibilityActive) "Accessibility Service Connected" else "Accessibility Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Text(
                            if (isAccessibilityActive)
                                "Ready to detect verified Lugmety screens and execute swipe gestures."
                            else
                                "Enable Lugmety Assistant in Android Accessibility Settings to automate gestures.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF90E0EF)
                        )
                    }
                    if (!isAccessibilityActive) {
                        Button(
                            onClick = { navController.navigate("setup") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6))
                        ) {
                            Text("Setup")
                        }
                    }
                }
            }

            // 2. Lugmety Monitoring Controls
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF0077B6)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Lugmety Order Monitoring", style = MaterialTheme.typography.titleLarge, color = Color.White)
                        Text(
                            text = monitoringStatus.name,
                            color = if (monitoringStatus == LugmetyAccessibilityService.MonitoringStatus.MONITORING) Color(0xFF00F5D4) else Color(0xFF90E0EF),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                isMonitoring = true
                                MonitoringForegroundService.start(
                                    context,
                                    refreshInterval.toInt(),
                                    cooldownInterval.toInt(),
                                    autoRefresh,
                                    orderDetection
                                )
                            },
                            enabled = isAccessibilityActive && monitoringStatus != LugmetyAccessibilityService.MonitoringStatus.MONITORING,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start")
                        }

                        Button(
                            onClick = {
                                isMonitoring = false
                                LugmetyAccessibilityService.instance?.pauseMonitoring()
                            },
                            enabled = monitoringStatus == LugmetyAccessibilityService.MonitoringStatus.MONITORING,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3A506B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Pause, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Pause")
                        }

                        Button(
                            onClick = {
                                isMonitoring = false
                                MonitoringForegroundService.stop(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF881337)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop")
                        }
                    }
                }
            }

            // 3. Metrics Summary Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricCard("Refreshes", refreshCount.toString(), Modifier.weight(1f))
                MetricCard("Detected", detectedCount.toString(), Modifier.weight(1f))
                MetricCard("Failed", failedCount.toString(), Modifier.weight(1f))
            }

            // 4. Auto Clicker Quick Section
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
                        Text("Auto Clicker Module", style = MaterialTheme.typography.titleMedium, color = Color.White)
                        Text("Clicks: $totalClicks", color = Color(0xFF00F5D4), style = MaterialTheme.typography.labelMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Independent gesture clicker with movable floating control panel and target markers.",
                        color = Color(0xFF90E0EF),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { navController.navigate("autoclicker") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077B6)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Open Clicker Settings")
                        }
                    }
                }
            }

            // 5. Auto Refresh & Detection Configuration
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541))) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Auto Refresh & Detection Settings", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Auto Refresh Swipes", color = Color.White)
                            Text("Bounds-based pull-down on order list", color = Color(0xFF90E0EF), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = autoRefresh,
                            onCheckedChange = {
                                autoRefresh = it
                                LugmetyAccessibilityService.instance?.setAutoRefreshEnabled(it)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Order Detection", color = Color.White)
                            Text("Inspect UI nodes for new incoming cards", color = Color(0xFF90E0EF), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(
                            checked = orderDetection,
                            onCheckedChange = {
                                orderDetection = it
                                LugmetyAccessibilityService.instance?.setOrderDetectionEnabled(it)
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Refresh Interval: ${refreshInterval.toInt()} seconds", color = Color.White)
                    Slider(
                        value = refreshInterval,
                        onValueChange = { refreshInterval = it },
                        valueRange = 4f..30f,
                        steps = 26
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541)),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF3A506B)))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF90E0EF))
            Text(value, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        }
    }
}
