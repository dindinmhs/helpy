package com.example.helpy

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.helpy.ui.screens.ProfileScreen
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
        Box(modifier = Modifier.fillMaxSize()) {
            // 👉 Konten utama tetap fullscreen
            when (selectedTab) {
                0 -> SOSScreen(
                    authViewModel = authViewModel,
                    onLogout = { isLoggedIn = false }
                )
                1 -> HomeScreen(
                    authViewModel = authViewModel,
                    onLogout = { isLoggedIn = false }
                )
                2 -> ProfileScreen(
                    authViewModel = authViewModel,
                    onLogout = { isLoggedIn = false },
                    onUpdateProfile = { name, phone, address, photoUri ->
                        Log.d("ProfileUpdate", "Name=$name, Phone=$phone, Address=$address, Photo=$photoUri")
                    }
                )
            }

            // 👉 Floating Bottom Navigation
            Surface(
                shape = RoundedCornerShape(50),
                tonalElevation = 6.dp,
                shadowElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 60.dp)
                    .fillMaxWidth(0.85f)
                    .height(65.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f).clickable { selectedTab = 0 }
                    ) {
                        Icon(
                            Icons.Filled.Warning,
                            contentDescription = "SOS",
                            tint = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Text(
                            "SOS",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedTab == 0) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f).clickable { selectedTab = 1 }
                    ) {
                        Icon(
                            Icons.Filled.LocationOn,
                            contentDescription = "Map",
                            tint = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Text(
                            "Map",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedTab == 1) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.weight(1f).clickable { selectedTab = 2 }
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = "Profile",
                            tint = if (selectedTab == 2) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                        Text(
                            "Profile",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedTab == 2) MaterialTheme.colorScheme.primary else Color.Gray
                        )
                    }
                }
            }



        }
    } else {
        LoginScreen(
            authViewModel = authViewModel,
            onLoginSuccess = { isLoggedIn = true }
        )
    }
}


