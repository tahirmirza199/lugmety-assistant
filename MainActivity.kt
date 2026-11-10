package com.lugmety.orderassistant.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lugmety.orderassistant.ui.screens.*
import com.lugmety.orderassistant.ui.theme.LugmetyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LugmetyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "dashboard") {
                        composable("dashboard") {
                            DashboardScreen(navController = navController)
                        }
                        composable("setup") {
                            AccessibilitySetupScreen(navController = navController)
                        }
                        composable("actions") {
                            OrderActionScreen(navController = navController)
                        }
                        composable("autoclicker") {
                            AutoClickerScreen(navController = navController)
                        }
                        composable("diagnostics") {
                            DiagnosticsScreen(navController = navController)
                        }
                    }
                }
            }
        }
    }
}
