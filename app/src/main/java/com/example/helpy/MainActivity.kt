package com.example.helpy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.helpy.ui.screens.HomeScreen
import com.example.helpy.ui.screens.LoginScreen
import com.example.helpy.ui.screens.SosScreen
import com.example.helpy.ui.screens.Profile
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

sealed class BottomNavItem(val route: String, val icon: ImageVector, val label: String) {
    object Peta : BottomNavItem("peta", Icons.Default.Home, "Peta")
    object SOS : BottomNavItem("sos", Icons.Default.Warning, "SOS")
    object Profile : BottomNavItem("profil", Icons.Default.Person, "Profile")
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
        MainScreenWithNavigation(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenWithNavigation(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val navController = rememberNavController()
    
    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        NavigationHost(
            navController = navController,
            authViewModel = authViewModel,
            onLogout = onLogout,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        BottomNavItem.Peta,
        BottomNavItem.SOS,
        BottomNavItem.Profile
    )
    
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 8.dp
    ) {
        items.forEach { item ->
            NavigationBarItem(
                icon = { 
                    Icon(
                        item.icon, 
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    ) 
                },
                label = { 
                    Text(
                        item.label,
                        fontSize = 12.sp,
                        fontWeight = if (currentRoute == item.route) FontWeight.Bold else FontWeight.Normal
                    ) 
                },
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        // on the back stack as users select items
                        popUpTo(navController.graph.startDestinationId)
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
fun NavigationHost(
    navController: NavHostController,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = BottomNavItem.Peta.route,
        modifier = modifier
    ) {
        composable(BottomNavItem.Peta.route) {
            HomeScreen(
                authViewModel = authViewModel,
                onLogout = onLogout
            )
        }
        composable(BottomNavItem.SOS.route) {
            SosScreen()
        }
        composable(BottomNavItem.Profile.route) {
            Profile(authViewModel = authViewModel)
        }
    }
}