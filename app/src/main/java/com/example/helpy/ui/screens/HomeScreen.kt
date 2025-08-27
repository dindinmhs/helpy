
package com.example.helpy.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.helpy.AuthViewModel
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.*
import kotlin.math.*

// Data classes untuk Overpass API response
data class OverpassResponse(
    val elements: List<OverpassElement>
)

data class OverpassElement(
    val type: String,
    val id: Long,
    val lat: Double? = null,
    val lon: Double? = null,
    val geometry: List<OverpassGeometry>? = null,
    val tags: Map<String, String>? = null
)

data class OverpassGeometry(
    val lat: Double,
    val lon: Double
)

// Interface untuk Overpass API
interface OverpassApiService {
    @GET("interpreter")
    suspend fun getWaysInBoundingBox(@Query("data") query: String): OverpassResponse
}

// Data classes untuk graf (A*)
data class GraphNode(
    val id: String,
    val lat: Double,
    val lon: Double
) {
    fun distanceTo(other: GraphNode): Double {
        return haversineDistance(this.lat, this.lon, other.lat, other.lon)
    }
}

data class GraphEdge(
    val from: GraphNode,
    val to: GraphNode,
    val weight: Double
)

data class Graph(
    val nodes: Map<String, GraphNode>,
    val edges: Map<String, List<GraphEdge>>
)

// A* Algorithm implementation
class AStarPathfinder {
    data class AStarNode(
        val node: GraphNode,
        val gScore: Double,
        val fScore: Double,
        val parent: AStarNode?
    ) : Comparable<AStarNode> {
        override fun compareTo(other: AStarNode): Int = fScore.compareTo(other.fScore)
    }

    fun findPath(graph: Graph, start: GraphNode, goal: GraphNode): List<GraphNode> {
        val openSet = PriorityQueue<AStarNode>()
        val closedSet = mutableSetOf<String>()
        val gScores = mutableMapOf<String, Double>()

        openSet.add(AStarNode(start, 0.0, start.distanceTo(goal), null))
        gScores[start.id] = 0.0

        while (openSet.isNotEmpty()) {
            val current = openSet.poll()

            if (current.node.id == goal.id) {
                return reconstructPath(current)
            }

            closedSet.add(current.node.id)

            val neighbors = graph.edges[current.node.id] ?: emptyList()
            for (edge in neighbors) {
                if (edge.to.id in closedSet) continue

                val tentativeGScore = current.gScore + edge.weight
                val currentGScore = gScores[edge.to.id] ?: Double.MAX_VALUE

                if (tentativeGScore < currentGScore) {
                    gScores[edge.to.id] = tentativeGScore
                    val fScore = tentativeGScore + edge.to.distanceTo(goal)
                    openSet.add(AStarNode(edge.to, tentativeGScore, fScore, current))
                }
            }
        }

        return emptyList() // No path found
    }

    private fun reconstructPath(goalNode: AStarNode): List<GraphNode> {
        val path = mutableListOf<GraphNode>()
        var current: AStarNode? = goalNode

        while (current != null) {
            path.add(0, current.node)
            current = current.parent
        }

        return path
    }
}

// Utility functions
fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0 // Earth's radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}

fun findNearestNode(nodes: Map<String, GraphNode>, target: GeoPoint): GraphNode? {
    return nodes.values.minByOrNull { node ->
        haversineDistance(node.lat, node.lon, target.latitude, target.longitude)
    }
}

// Graph builder from Overpass data
class GraphBuilder {
    fun buildGraph(overpassResponse: OverpassResponse): Graph {
        val nodes = mutableMapOf<String, GraphNode>()
        val edges = mutableMapOf<String, MutableList<GraphEdge>>()

        // Process ways to build graph
        for (element in overpassResponse.elements) {
            if (element.type == "way" && element.geometry != null) {
                val wayNodes = mutableListOf<GraphNode>()

                // Create nodes for each point in the way
                for ((index, geometry) in element.geometry.withIndex()) {
                    val nodeId = "${element.id}_$index"
                    val node = GraphNode(nodeId, geometry.lat, geometry.lon)
                    nodes[nodeId] = node
                    wayNodes.add(node)
                }

                // Create bidirectional edges between consecutive nodes
                for (i in 0 until wayNodes.size - 1) {
                    val fromNode = wayNodes[i]
                    val toNode = wayNodes[i + 1]
                    val distance = fromNode.distanceTo(toNode)

                    // Add forward edge
                    edges.getOrPut(fromNode.id) { mutableListOf() }.add(
                        GraphEdge(fromNode, toNode, distance)
                    )

                    // Add backward edge (for bidirectional roads)
                    edges.getOrPut(toNode.id) { mutableListOf() }.add(
                        GraphEdge(toNode, fromNode, distance)
                    )
                }
            }
        }

        return Graph(nodes, edges)
    }
}

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
    var isLoadingRoute by remember { mutableStateOf(false) }

    var startPoint: GeoPoint? by remember { mutableStateOf(null) }
    var endPoint: GeoPoint? by remember { mutableStateOf(null) }

    // Initialize Retrofit for Overpass API
    val overpassApi = remember {
        Retrofit.Builder()
            .baseUrl("https://overpass-api.de/api/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OverpassApiService::class.java)
    }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // State untuk menangani permintaan izin lokasi
    val locationPermissionRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                getCurrentLocation(context) { location ->
                    location?.let {
                        val userLocation = GeoPoint(it.latitude, it.longitude)
                        startPoint = userLocation
                        mapView?.controller?.animateTo(userLocation)
                        currentLocationOverlay?.enableMyLocation()
                    }
                }
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                getCurrentLocation(context) { location ->
                    location?.let {
                        val userLocation = GeoPoint(it.latitude, it.longitude)
                        startPoint = userLocation
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
                    startPoint = userLocation
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
        topBar = {
            TopAppBar(
                title = { Text("Peta Navigasi") },
                actions = {
                    IconButton(onClick = { authViewModel.signOut() }) {
                        Icon(Icons.Filled.ExitToApp, "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = {
                        if (hasLocationPermission(context)) {
                            getCurrentLocation(context) { location ->
                                location?.let {
                                    val userGeoPoint = GeoPoint(it.latitude, it.longitude)
                                    startPoint = userGeoPoint
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
                            isLoadingRoute = true
                            findAndDrawRoute(
                                overpassApi = overpassApi,
                                mapView = mapView,
                                start = startPoint!!,
                                end = endPoint!!
                            ) { newPolyline ->
                                routePolyline?.let { mapView?.overlays?.remove(it) }
                                routePolyline = newPolyline
                                isLoadingRoute = false
                            }
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
                                    endPoint = geoPoint
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

// Function to find and draw route using Overpass API and A*
private fun findAndDrawRoute(
    overpassApi: OverpassApiService,
    mapView: MapView?,
    start: GeoPoint,
    end: GeoPoint,
    onRouteDrawn: (Polyline) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            // Calculate bounding box with some padding
            val padding = 0.01 // ~1km padding
            val minLat = minOf(start.latitude, end.latitude) - padding
            val maxLat = maxOf(start.latitude, end.latitude) + padding
            val minLon = minOf(start.longitude, end.longitude) - padding
            val maxLon = maxOf(start.longitude, end.longitude) + padding

            // Overpass query to get ways (roads) in the bounding box
            val query = """
                [out:json][timeout:25];
                (
                  way["highway"]["highway"!="footway"]["highway"!="cycleway"]["highway"!="path"]
                     ["highway"!="steps"]["highway"!="track"]["access"!="private"]
                     ($minLat,$minLon,$maxLat,$maxLon);
                );
                out geom;
            """.trimIndent()

            // Fetch data from Overpass API
            val response = overpassApi.getWaysInBoundingBox(query)
            println("Overpass API response: $response")

            // Build graph from Overpass data
            val graphBuilder = GraphBuilder()
            val graph = graphBuilder.buildGraph(response)

            if (graph.nodes.isEmpty()) {
                // No roads found, draw straight line
                withContext(Dispatchers.Main) {
                    drawStraightLine(mapView, start, end, onRouteDrawn)
                }
                return@launch
            }

            // Find nearest nodes to start and end points
            val startNode = findNearestNode(graph.nodes, start)
            val endNode = findNearestNode(graph.nodes, end)

            if (startNode == null || endNode == null) {
                // Couldn't find nearest nodes, draw straight line
                withContext(Dispatchers.Main) {
                    drawStraightLine(mapView, start, end, onRouteDrawn)
                }
                return@launch
            }

            // Run A* algorithm
            val pathfinder = AStarPathfinder()
            val path = pathfinder.findPath(graph, startNode, endNode)

            withContext(Dispatchers.Main) {
                if (path.isNotEmpty()) {
                    // Convert path to GeoPoints and draw route
                    val routePoints = mutableListOf<GeoPoint>()
                    routePoints.add(start) // Add actual start point

                    for (node in path) {
                        routePoints.add(GeoPoint(node.lat, node.lon))
                    }

                    routePoints.add(end) // Add actual end point

                    drawRoutePolyline(mapView, routePoints, onRouteDrawn)
                } else {
                    // No path found, draw straight line
                    drawStraightLine(mapView, start, end, onRouteDrawn)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // On error, draw straight line
            withContext(Dispatchers.Main) {
                drawStraightLine(mapView, start, end, onRouteDrawn)
            }
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

// Existing utility functions
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
private fun getCurrentLocation(context: Context, onLocationReceived: (Location?) -> Unit) {
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    fusedLocationClient.lastLocation
        .addOnSuccessListener { location: Location? ->
            onLocationReceived(location)
        }
        .addOnFailureListener {
            onLocationReceived(null)
        }
}