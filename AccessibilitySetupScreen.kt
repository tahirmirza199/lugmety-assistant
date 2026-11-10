package com.lugmety.orderassistant.ui.screens

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.lugmety.orderassistant.service.LugmetyAccessibilityService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibilitySetupScreen(navController: NavController) {
    val context = LocalContext.current
    val isAccessibilityActive = LugmetyAccessibilityService.isServiceRunning

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accessibility Setup") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isAccessibilityActive)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isAccessibilityActive) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            if (isAccessibilityActive) "Accessibility Service Enabled" else "Accessibility Permission Required",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            if (isAccessibilityActive)
                                "Ready to detect Lugmety order list screens and perform swipe gestures."
                            else
                                "You must enable Lugmety Order Assistant in Android Settings.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Step 1: Open Accessibility Settings
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Step 1: Enable Accessibility Service", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "1. Tap 'Open Accessibility Settings' below.\n" +
                        "2. Look for 'Lugmety Order Assistant' under Installed Apps or Downloaded Services.\n" +
                        "3. Turn the switch ON and accept the prompt.\n" +
                        "Note: This app NEVER reads passwords, credit cards, or personal messages. It only scans verified Lugmety driver screens.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Accessibility, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Accessibility Settings")
                    }
                }
            }

            // Step 2: Battery Optimization
            Card {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Step 2: Disable Battery Restrictions", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Android aggressively kills background services unless battery optimization is disabled. To ensure continuous monitoring while Lugmety is open, please whitelist this assistant.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:" + context.packageName)
                                }
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.BatteryAlert, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Disable Battery Optimization")
                    }
                }
            }
        }
    }
}
