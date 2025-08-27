
package com.example.helpy.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helpy.AuthViewModel
import com.example.helpy.data.remote.OverpassApiService
import com.example.helpy.data.repository.RouteRepositoryImpl
import com.example.helpy.domain.usecase.GetRouteUseCase
import com.example.helpy.ui.screens.map.MapViewModel
import com.example.helpy.ui.screens.map.MapViewModelFactory
import com.example.helpy.util.getCurrentLocation
import com.example.helpy.util.hasLocationPermission
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    authViewModel: AuthViewModel = viewModel(),
    onLogout: () -> Unit
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val context = LocalContext.current
    var mapView: MapView? by remember { mutableStateOf(null) }
    var currentLocationOverlay: MyLocationNewOverlay? by remember { mutableStateOf(null) }
    var currentTargetMarker: Marker? by remember { mutableStateOf(null) }
    var routePolyline: Polyline? by remember { mutableStateOf(null) }

    // Initialize dependencies
    val overpassApi = remember {
        Retrofit.Builder()
            .baseUrl("https://overpass-api.de/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApiService::class.java)
    }

    val routeRepository = remember { RouteRepositoryImpl(overpassApi) }
    val getRouteUseCase = remember { GetRouteUseCase(routeRepository) }
    val viewModelFactory = remember { MapViewModelFactory(getRouteUseCase) }
    val mapViewModel: MapViewModel = viewModel(factory = viewModelFactory)

    // Observe ViewModel states
    val isLoadingRoute by mapViewModel.isLoadingRoute.collectAsState()
    val routePath by mapViewModel.routePath.collectAsState()
    val startPoint by mapViewModel.startPoint.collectAsState()
    val endPoint by mapViewModel.endPoint.collectAsState()

    // Handle route path changes
    LaunchedEffect(routePath) {
        if (routePath.isNotEmpty()) {
            val routePoints = mutableListOf<GeoPoint>()
            startPoint?.let { routePoints.add(it) }
            
            for (node in routePath) {
                routePoints.add(GeoPoint(node.lat, node.lon))
            }
            
            endPoint?.let { routePoints.add(it) }
            
            if (routePoints.isNotEmpty()) {
                drawRoutePolyline(mapView, routePoints) { newPolyline ->
                    routePolyline?.let { mapView?.overlays?.remove(it) }
                    routePolyline = newPolyline
                }
            }
        } else if (startPoint != null && endPoint != null && !isLoadingRoute) {
            // If no path found, draw straight line
            drawStraightLine(mapView, startPoint!!, endPoint!!) { newPolyline ->
                routePolyline?.let { mapView?.overlays?.remove(it) }
                routePolyline = newPolyline
            }
        }
    }

    // State untuk menangani permintaan izin lokasi
    val locationPermissionRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                getCurrentLocation(context) { location ->
                    location?.let {
                        val userLocation = GeoPoint(it.latitude, it.longitude)
                        mapViewModel.setStartPoint(userLocation)
                        mapView?.controller?.animateTo(userLocation)
                        currentLocationOverlay?.enableMyLocation()
                    }
                }
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                getCurrentLocation(context) { location ->
                    location?.let {
                        val userLocation = GeoPoint(it.latitude, it.longitude)
                        mapViewModel.setStartPoint(userLocation)
                        mapView?.controller?.animateTo(userLocation)
                        currentLocationOverlay?.enableMyLocation()
                    }
                }
            }
            else -> {
                // Permission not granted
            }
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            getCurrentLocation(context) { location ->
                location?.let {
                    val userLocation = GeoPoint(it.latitude, it.longitude)
                    mapViewModel.setStartPoint(userLocation)
                    mapView?.controller?.setCenter(userLocation)
                    currentLocationOverlay?.enableMyLocation()
                }
            }
        } else {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(currentUser) {
        if (currentUser == null) {
            onLogout()
        }
    }

    Scaffold(
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = {
                        if (hasLocationPermission(context)) {
                            getCurrentLocation(context) { location ->
                                location?.let {
                                    val userGeoPoint = GeoPoint(it.latitude, it.longitude)
                                    mapViewModel.setStartPoint(userGeoPoint)
                                    mapView?.controller?.animateTo(userGeoPoint)
                                    mapView?.controller?.setZoom(18.0)
                                }
                            }
                        } else {
                            locationPermissionRequest.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Filled.LocationOn, "Lokasi Saya")
                }

                FloatingActionButton(
                    onClick = {
                        if (startPoint != null && endPoint != null && !isLoadingRoute) {
                            mapViewModel.findRoute()
                        }
                    },
                    containerColor = if (isLoadingRoute) MaterialTheme.colorScheme.secondary
                    else MaterialTheme.colorScheme.primary
                ) {
                    if (isLoadingRoute) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    } else {
                        Icon(Icons.Filled.PlayArrow, "Gambar Rute")
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                factory = { ctx ->
                    Configuration.getInstance().load(
                        ctx,
                        ctx.getSharedPreferences("osm_prefs", Context.MODE_PRIVATE)
                    )
                    Configuration.getInstance().userAgentValue = ctx.packageName

                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        controller.setZoom(15.0)

                        val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                        locationOverlay.enableMyLocation()
                        locationOverlay.enableFollowLocation()
                        overlays.add(locationOverlay)
                        currentLocationOverlay = locationOverlay

                        val mapEventsReceiver = object : MapEventsReceiver {
                            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                                p?.let { geoPoint ->
                                    mapViewModel.setEndPoint(geoPoint)
                                    currentTargetMarker?.let { overlays.remove(it) }

                                    val targetMarker = Marker(this@apply)
                                    targetMarker.position = geoPoint
                                    targetMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                    targetMarker.title = "Tujuan"
                                    overlays.add(targetMarker)
                                    currentTargetMarker = targetMarker
                                    this@apply.invalidate()
                                }
                                return true
                            }

                            override fun longPressHelper(p: GeoPoint?): Boolean = false
                        }

                        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
                        overlays.add(0, mapEventsOverlay)
                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun drawRoutePolyline(mapView: MapView?, points: List<GeoPoint>, onRouteDrawn: (Polyline) -> Unit) {
    mapView?.let {
        val polyline = Polyline(it)
        polyline.setPoints(points)
        polyline.outlinePaint.color = Color.BLUE
        polyline.outlinePaint.strokeWidth = 10f
        it.overlays.add(polyline)
        it.invalidate()
        onRouteDrawn(polyline)
    }
}

private fun drawStraightLine(mapView: MapView?, start: GeoPoint, end: GeoPoint, onRouteDrawn: (Polyline) -> Unit) {
    drawRoutePolyline(mapView, listOf(start, end), onRouteDrawn)
}