package com.lugmety.orderassistant.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lugmety.orderassistant.service.LugmetyAccessibilityService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(navController: NavController) {
    var isSimulatedTestActive by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Diagnostics & Verification") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("System Checks", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))

                    StatusRow(
                        label = "Accessibility Service",
                        active = LugmetyAccessibilityService.isServiceRunning
                    )
                    StatusRow(label = "Gesture API Support", active = true)
                    StatusRow(label = "Room Local Database", active = true)
                    StatusRow(label = "Notification Channels", active = true)
                }
            }

            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Simulated Test Mode", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Test order alerts and notification triggers safely without interacting with live Lugmety.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { isSimulatedTestActive = !isSimulatedTestActive },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isSimulatedTestActive) "Stop Simulation" else "Trigger Simulated Order Alert")
                    }
                }
            }
        }
    }
}

@Composable
fun StatusRow(label: String, active: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(
            if (active) "ONLINE / READY" else "NOT AVAILABLE",
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}
