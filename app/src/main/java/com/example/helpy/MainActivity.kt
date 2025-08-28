package com.example.helpy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helpy.ui.screens.HomeScreen
import com.example.helpy.ui.screens.LoginScreen
import com.example.helpy.ui.theme.HelpyTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import com.example.helpy.ui.screens.SOSScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelpyTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    MyApp()
                }
            }
        }
    }
}

@Composable
fun MyApp() {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }
    val authViewModel: AuthViewModel = viewModel(
        factory = AuthViewModelFactory(authManager)
    )

    var isLoggedIn by remember { mutableStateOf(false) }
    val currentUser by authViewModel.currentUser.collectAsState()

    // State untuk tab yang aktif (0 = SOS, 1 = Map)
    var selectedTab by remember { mutableStateOf(0) }

    // Update login state based on current user
    LaunchedEffect(currentUser) {
        isLoggedIn = currentUser != null
        // Reset ke tab SOS saat login
        if (currentUser != null) {
            selectedTab = 0
        }
    }

    if (isLoggedIn) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    text = { Text("SOS") },
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Filled.Warning, contentDescription = "SOS") }
                )
                Tab(
                    text = { Text("Map") },
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Filled.LocationOn, contentDescription = "Map") }
                )
            }

            // Content berdasarkan tab yang dipilih
            when (selectedTab) {
                0 -> SOSScreen(
                    authViewModel = authViewModel,
                    onLogout = { isLoggedIn = false }
                )
                1 -> HomeScreen(
                    authViewModel = authViewModel,
                    onLogout = { isLoggedIn = false }
                )
            }
        }
    } else {
        LoginScreen(
            authViewModel = authViewModel,
            onLoginSuccess = { isLoggedIn = true }
        )
    }
}