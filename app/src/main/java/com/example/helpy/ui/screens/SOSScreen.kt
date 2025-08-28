package com.example.helpy.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.helpy.AuthViewModel
import com.example.helpy.data.SOSData
import com.example.helpy.repository.SOSRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SOSScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val currentUser by authViewModel.currentUser.collectAsState()
    val sosRepository = remember { SOSRepository() }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var isSOSActive by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }
    var currentSOSId by remember { mutableStateOf<String?>(null) }


    // Permission launcher untuk lokasi
    val locationPermissionRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                    permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Permission granted, send SOS
                sendSOSAlert(
                    context = context,
                    fusedLocationClient = fusedLocationClient,
                    sosRepository = sosRepository,
                    currentUser = currentUser,
                    setLoading = { isLoading = it },
                    setMessage = { statusMessage = it },
                    setSOSActive = { isSOSActive = it },
                    setSOSId = { currentSOSId = it }
                )
            }
            else -> {
                statusMessage = "Location permission required for SOS"
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency SOS") },
                actions = {
                    IconButton(onClick = { authViewModel.signOut() }) {
                        Icon(Icons.Filled.ExitToApp, "Logout")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Status message
            if (statusMessage.isNotEmpty()) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }

            // Loading indicator
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.padding(16.dp)
                )
            }

            // SOS Button
            Button(
                onClick = {
                    if (!isSOSActive) {
                        // Check location permission first
                        if (hasLocationPermission(context)) {
                            sendSOSAlert(
                                context = context,
                                fusedLocationClient = fusedLocationClient,
                                sosRepository = sosRepository,
                                currentUser = currentUser,
                                setLoading = { isLoading = it },
                                setMessage = { statusMessage = it },
                                setSOSActive = { isSOSActive = it },
                                setSOSId = { currentSOSId = it }
                            )
                        } else {
                            locationPermissionRequest.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    } else {
                        // Deactivate SOS
                        deactivateSOS(
                            sosRepository = sosRepository,
                            sosId = currentSOSId,
                            setLoading = { isLoading = it },
                            setMessage = { statusMessage = it },
                            setSOSActive = { isSOSActive = it },
                            setSOSId = { currentSOSId = it }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSOSActive) Color.Gray else Color.Red
                ),
                modifier = Modifier.size(200.dp),
                enabled = !isLoading
            ) {
                Text(
                    text = if (isSOSActive) "DEACTIVATE" else "SOS",
                    style = MaterialTheme.typography.headlineLarge
                )
            }

            if (isSOSActive) {
                Text(
                    text = "SOS Alert Active",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Red,
                    modifier = Modifier.padding(top = 16.dp)
                )
            }
        }
    }
}

// Utility function untuk check permission
private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

@SuppressLint("MissingPermission")
private fun sendSOSAlert(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    sosRepository: SOSRepository,
    currentUser: FirebaseUser?, // Ubah dari User? ke FirebaseUser?
    setLoading: (Boolean) -> Unit,
    setMessage: (String) -> Unit,
    setSOSActive: (Boolean) -> Unit,
    setSOSId: (String?) -> Unit
) {
    if (currentUser == null) {
        setMessage("User not authenticated")
        return
    }

    setLoading(true)
    setMessage("Getting location...")

    fusedLocationClient.lastLocation
        .addOnSuccessListener { location: Location? ->
            if (location != null) {
                val sosData = SOSData(
                    userId = currentUser.uid,
                    userEmail = currentUser.email ?: "",
                    latitude = location.latitude,
                    longitude = location.longitude,
                    timestamp = System.currentTimeMillis(),
                    status = "ACTIVE"
                )

                // Send to Firebase
                CoroutineScope(Dispatchers.IO).launch {
                    val result = sosRepository.sendSOSAlert(sosData)
                    withContext(Dispatchers.Main) {
                        setLoading(false)
                        if (result.isSuccess) {
                            val sosId = result.getOrNull()
                            setSOSId(sosId)
                            setSOSActive(true)
                            setMessage("SOS Alert sent successfully!")
                        } else {
                            setMessage("Failed to send SOS: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
            } else {
                setLoading(false)
                setMessage("Unable to get current location")
            }
        }
        .addOnFailureListener { exception ->
            setLoading(false)
            setMessage("Location error: ${exception.message}")
        }
}

private fun deactivateSOS(
    sosRepository: SOSRepository,
    sosId: String?,
    setLoading: (Boolean) -> Unit,
    setMessage: (String) -> Unit,
    setSOSActive: (Boolean) -> Unit,
    setSOSId: (String?) -> Unit
) {
    if (sosId == null) return

    setLoading(true)
    CoroutineScope(Dispatchers.IO).launch {
        val result = sosRepository.updateSOSStatus(sosId, "RESOLVED")
        withContext(Dispatchers.Main) {
            setLoading(false)
            if (result.isSuccess) {
                setSOSActive(false)
                setSOSId(null)
                setMessage("SOS Alert deactivated")
            } else {
                setMessage("Failed to deactivate SOS")
            }
        }
    }
}