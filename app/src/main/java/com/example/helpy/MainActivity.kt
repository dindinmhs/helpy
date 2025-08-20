package com.example.helpy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
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

    // Update login state based on current user
    LaunchedEffect(currentUser) {
        isLoggedIn = currentUser != null
    }

    if (isLoggedIn) {
        HomeScreen(
            authViewModel = authViewModel,
            onLogout = { isLoggedIn = false }
        )
    } else {
        LoginScreen(
            authViewModel = authViewModel,
            onLoginSuccess = { isLoggedIn = true }
        )
    }
}