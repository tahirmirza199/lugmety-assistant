package com.lugmety.orderassistant.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.lugmety.orderassistant.service.MonitoringForegroundService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderActionScreen(navController: NavController) {
    val context = LocalContext.current
    var mode by remember { mutableStateOf("MANUAL_CONFIRMATION") }
    var emergencyTriggered by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Action Controller", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
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
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF0077B6)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Order Acceptance Mode", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Configure how detected orders are handled. Safe manual confirmation is always recommended to ensure you review the delivery distance and items before committing.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF90E0EF)
                    )
                }
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF1E2E56)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = mode == "MANUAL_CONFIRMATION",
                            onClick = { mode = "MANUAL_CONFIRMATION" },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF00F5D4))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Manual Confirmation Mode (Recommended)", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                "Immediately sends high-priority heads-up notification and brings you directly to Lugmety. You tap Accept manually.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF90E0EF)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = mode == "AUTO_ACCEPT_IF_PERMITTED",
                            onClick = { mode = "AUTO_ACCEPT_IF_PERMITTED" },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF48CAE4))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Automated Acceptance Mode", style = MaterialTheme.typography.titleMedium, color = Color.White)
                            Text(
                                "Requires explicit label verification on the Accept button. Strictly guarded against random clicks. Disabled if screen labels cannot be verified.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF90E0EF)
                            )
                        }
                    }
                }
            }

            // Emergency Stop Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C2541)),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFF43F5E)))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFF43F5E))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Emergency Stop Controller", style = MaterialTheme.typography.titleMedium, color = Color(0xFFF43F5E))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Instantly disconnects all accessibility interactions, stops ongoing swipes, cancels auto-clicks, and resets the service state immediately.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF90E0EF)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = {
                            // Real emergency stop execution
                            LugmetyAccessibilityService.instance?.emergencyStopAll()
                            MonitoringForegroundService.stop(context)
                            FloatingControlPanelService.stop(context)
                            emergencyTriggered = true
                            Toast.makeText(context, "EMERGENCY STOP ACTIVATED: All gestures cancelled.", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF43F5E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("EMERGENCY STOP ALL ACTIONS", color = Color.White)
                    }

                    if (emergencyTriggered) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "✓ Emergency stop signal dispatched. All background timers and accessibility gestures have been terminated.",
                            color = Color(0xFF00F5D4),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        }
    }
}
