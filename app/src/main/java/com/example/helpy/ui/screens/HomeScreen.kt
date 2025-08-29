
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
import androidx.compose.material.icons.filled.Refresh
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
import com.example.helpy.data.SOSData
import com.example.helpy.repository.SOSRepository
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    private data class NodeKey(val lat: Double, val lon: Double) {
        companion object {
            private const val PRECISION = 1000000.0 // 6 decimal places precision
        }

        fun isNear(other: NodeKey, tolerance: Double = 0.00001): Boolean {
            return abs(lat - other.lat) < tolerance && abs(lon - other.lon) < tolerance
        }

        fun rounded(): NodeKey {
            return NodeKey(
                (lat * PRECISION).roundToInt() / PRECISION,
                (lon * PRECISION).roundToInt() / PRECISION
            )
        }
    }

    fun buildGraph(overpassResponse: OverpassResponse): Graph {
        val nodeMap = mutableMapOf<NodeKey, GraphNode>()
        val edges = mutableMapOf<String, MutableList<GraphEdge>>()
        val nodeConnections = mutableMapOf<NodeKey, MutableSet<String>>() // Track which ways connect to each node

        // First pass: collect all unique nodes and track way connections
        for (element in overpassResponse.elements) {
            if (element.type == "way" && element.geometry != null && element.geometry.isNotEmpty()) {
                val wayId = element.id.toString()

                for ((index, geometry) in element.geometry.withIndex()) {
                    val nodeKey = NodeKey(geometry.lat, geometry.lon).rounded()

                    // Create or get existing node
                    if (!nodeMap.containsKey(nodeKey)) {
                        val nodeId = "${nodeKey.lat}_${nodeKey.lon}"
                        nodeMap[nodeKey] = GraphNode(nodeId, nodeKey.lat, nodeKey.lon)
                    }

                    // Track way connections for intersection detection
                    nodeConnections.getOrPut(nodeKey) { mutableSetOf() }.add(wayId)
                }
            }
        }

        // Second pass: create edges within ways and at intersections
        for (element in overpassResponse.elements) {
            if (element.type == "way" && element.geometry != null && element.geometry.size > 1) {
                val wayNodes = mutableListOf<GraphNode>()

                // Get nodes for this way
                for (geometry in element.geometry) {
                    val nodeKey = NodeKey(geometry.lat, geometry.lon).rounded()
                    nodeMap[nodeKey]?.let { wayNodes.add(it) }
                }

                // Create edges between consecutive nodes in the way
                for (i in 0 until wayNodes.size - 1) {
                    val fromNode = wayNodes[i]
                    val toNode = wayNodes[i + 1]
                    val distance = fromNode.distanceTo(toNode)

                    // Check if this is a one-way street
                    val isOneWay = element.tags?.get("oneway") == "yes"

                    // Add forward edge
                    edges.getOrPut(fromNode.id) { mutableListOf() }.add(
                        GraphEdge(fromNode, toNode, distance)
                    )

                    // Add backward edge only if not one-way
                    if (!isOneWay) {
                        edges.getOrPut(toNode.id) { mutableListOf() }.add(
                            GraphEdge(toNode, fromNode, distance)
                        )
                    }
                }

                // Connect intersection nodes to enable way-to-way navigation
                connectIntersectionNodes(wayNodes, nodeConnections, nodeMap, edges)
            }
        }

        return Graph(nodeMap.values.associateBy { it.id }, edges)
    }

    private fun connectIntersectionNodes(
        wayNodes: List<GraphNode>,
        nodeConnections: Map<NodeKey, Set<String>>,
        nodeMap: Map<NodeKey, GraphNode>,
        edges: MutableMap<String, MutableList<GraphEdge>>
    ) {
        // Check start and end nodes of the way for intersections
        val startNode = wayNodes.first()
        val endNode = wayNodes.last()

        listOf(startNode, endNode).forEach { node ->
            val nodeKey = NodeKey(node.lat, node.lon).rounded()
            val connectedWays = nodeConnections[nodeKey] ?: emptySet()

            // If this node connects multiple ways (intersection), create connections
            if (connectedWays.size > 1) {
                // Find nearby nodes from other ways
                val nearbyNodes = findNearbyIntersectionNodes(nodeKey, nodeMap, 0.00002) // ~2 meter tolerance

                nearbyNodes.forEach { nearbyNode ->
                    if (nearbyNode.id != node.id) {
                        val distance = node.distanceTo(nearbyNode)

                        // Add bidirectional connection at intersection
                        edges.getOrPut(node.id) { mutableListOf() }.add(
                            GraphEdge(node, nearbyNode, distance)
                        )
                        edges.getOrPut(nearbyNode.id) { mutableListOf() }.add(
                            GraphEdge(nearbyNode, node, distance)
                        )
                    }
                }
            }
        }
    }

    private fun findNearbyIntersectionNodes(
        targetKey: NodeKey,
        nodeMap: Map<NodeKey, GraphNode>,
        tolerance: Double
    ): List<GraphNode> {
        return nodeMap.filter { entry ->
            entry.key.isNear(targetKey, tolerance)
        }.map { entry ->
            entry.value
        }
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
    val sosRepository = remember { SOSRepository() } // Tambahkan ini

    var mapView: MapView? by remember { mutableStateOf(null) }
    var currentLocationOverlay: MyLocationNewOverlay? by remember { mutableStateOf(null) }
    var currentTargetMarker: Marker? by remember { mutableStateOf(null) }
    var routePolyline: Polyline? by remember { mutableStateOf(null) }
    var isLoadingRoute by remember { mutableStateOf(false) }
    var selectedSOSLocation: GeoPoint? by remember { mutableStateOf(null) }
    var selectedSOSInfo: SOSData? by remember { mutableStateOf(null) }
    var sosMarkers by remember { mutableStateOf<List<Marker>>(emptyList()) } // Tambahkan ini

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

    LaunchedEffect(Unit) {
        loadActiveSOSAlerts(sosRepository, mapView) { markers ->
            sosMarkers = markers
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
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                // Refresh SOS button
                FloatingActionButton(
                    onClick = {
                        loadActiveSOSAlertsWithNavigation(sosRepository, mapView) { sosLocation, sosData ->
                            selectedSOSLocation = sosLocation
                            selectedSOSInfo = sosData
                            endPoint = sosLocation
                        }
                    },
                    modifier = Modifier.padding(bottom = 8.dp),
                    containerColor = MaterialTheme.colorScheme.tertiary
                ) {
                    Icon(Icons.Filled.Refresh, "Refresh SOS")
                }

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
                    modifier = Modifier.padding(bottom = 8.dp),
                    containerColor = MaterialTheme.colorScheme.primary

                ) {
                    Icon(Icons.Filled.LocationOn, "Lokasi Saya")
                }

                // Route button - hanya satu yang berfungsi untuk SOS navigation
                FloatingActionButton(
                    onClick = {
                        if (startPoint != null && selectedSOSLocation != null && !isLoadingRoute) {
                            isLoadingRoute = true
                            findAndDrawRoute(
                                overpassApi = overpassApi,
                                mapView = mapView,
                                start = startPoint!!,
                                end = selectedSOSLocation!!
                            ) { newPolyline ->
                                routePolyline?.let { mapView?.overlays?.remove(it) }
                                routePolyline = newPolyline
                                isLoadingRoute = false
                            }
                        }
                    },
                    modifier = Modifier.padding(bottom = 100.dp),
                    containerColor = if (isLoadingRoute) MaterialTheme.colorScheme.secondary
                    else if (selectedSOSLocation != null) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.tertiary
                ) {
                    if (isLoadingRoute) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.secondary
                        )
                    } else {
                        Icon(Icons.Filled.PlayArrow, "Navigate to SOS")
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
            selectedSOSInfo?.let { sosInfo ->
                Card(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .fillMaxWidth(0.8f),
                    colors = CardDefaults.cardColors(
                        containerColor = androidx.compose.ui.graphics.Color.Red.copy(alpha = 0.9f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Text(
                            text = "SOS Alert Selected",
                            style = MaterialTheme.typography.titleSmall,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                        Text(
                            text = "User: ${sosInfo.userEmail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                        Text(
                            text = "Tap route button to navigate",
                            style = MaterialTheme.typography.bodySmall,
                            color = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }
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
                                return false // Hapus manual destination setting
                            }
                            override fun longPressHelper(p: GeoPoint?): Boolean = false
                        }

                        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
                        overlays.add(0, mapEventsOverlay)
                        mapView = this

                        // Load SOS alerts setelah map siap
                        CoroutineScope(Dispatchers.Main).launch {
                            delay(500) // Tunggu map siap
                            loadActiveSOSAlertsWithNavigation(sosRepository,
                                mapView!!
                            ) { sosLocation, sosData ->
                                selectedSOSLocation = sosLocation
                                selectedSOSInfo = sosData
                                endPoint = sosLocation // Set endPoint untuk navigasi
                            }
                        }

                        mapView = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

private fun loadActiveSOSAlertsWithNavigation(
    sosRepository: SOSRepository,
    mapView: MapView?,
    onSOSSelected: (GeoPoint, SOSData) -> Unit // Tambah parameter SOSData
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val result = sosRepository.getAllActiveSOSAlerts()
            if (result.isSuccess) {
                val sosAlerts = result.getOrNull() ?: emptyList()

                withContext(Dispatchers.Main) {
                    // Hapus marker SOS lama
                    val oldSOSMarkers = mapView?.overlays?.filterIsInstance<Marker>()
                        ?.filter { it.title?.startsWith("SOS:") == true }
                    oldSOSMarkers?.forEach { mapView.overlays.remove(it) }

                    // Tambahkan marker SOS baru
                    sosAlerts.forEach { sosData ->
                        val marker = Marker(mapView)
                        marker.position = GeoPoint(sosData.latitude, sosData.longitude)
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        marker.title = "SOS: ${sosData.userEmail}"
                        marker.snippet = "Emergency Alert - Selected for navigation"

                        marker.setOnMarkerClickListener { _, _ ->
                            val sosLocation = GeoPoint(sosData.latitude, sosData.longitude)
                            onSOSSelected(sosLocation, sosData) // Pass SOSData juga
                            true
                        }

                        mapView?.overlays?.add(marker)
                    }

                    mapView?.invalidate()
                }
            }
        } catch (e: Exception) {
            println("Error loading SOS alerts: ${e.message}")
        }
    }
}

private fun loadActiveSOSAlerts(
    sosRepository: SOSRepository,
    mapView: MapView?,
    onMarkersLoaded: (List<Marker>) -> Unit
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val result = sosRepository.getAllActiveSOSAlerts()
            if (result.isSuccess) {
                val sosAlerts = result.getOrNull() ?: emptyList()

                withContext(Dispatchers.Main) {
                    mapView?.let { map ->
                        // Hapus marker SOS lama
                        val oldSOSMarkers = map.overlays.filterIsInstance<Marker>()
                            .filter { it.title?.startsWith("SOS:") == true }
                        oldSOSMarkers.forEach { map.overlays.remove(it) }

                        // Tambahkan marker SOS baru
                        val newMarkers = mutableListOf<Marker>()
                        sosAlerts.forEach { sosData ->
                            val marker = Marker(map)
                            marker.position = GeoPoint(sosData.latitude, sosData.longitude)
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = "SOS: ${sosData.userEmail}"
                            marker.snippet = "Emergency Alert - Tap to navigate"

                            // Set custom icon untuk SOS (opsional, bisa pakai default)
                            // marker.icon = ContextCompat.getDrawable(context, R.drawable.sos_icon)

                            // Set OnMarkerClickListener untuk navigasi
                            marker.setOnMarkerClickListener { clickedMarker, _ ->
                                // Set sebagai destination dan mulai navigasi
                                val sosLocation = GeoPoint(sosData.latitude, sosData.longitude)

                                // Hapus target marker lama
                                map.overlays.filterIsInstance<Marker>()
                                    .find { it.title == "Tujuan" }
                                    ?.let { map.overlays.remove(it) }

                                // Tambahkan target marker baru
                                val targetMarker = Marker(map)
                                targetMarker.position = sosLocation
                                targetMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                                targetMarker.title = "Tujuan"
                                targetMarker.snippet = "Navigating to SOS Alert"
                                map.overlays.add(targetMarker)

                                // Set endPoint untuk navigasi
                                // Ini perlu diakses dari scope yang lebih luas
                                // Kita akan handle ini dengan callback
                                navigateToSOS(sosLocation)

                                map.invalidate()
                                true
                            }

                            map.overlays.add(marker)
                            newMarkers.add(marker)
                        }

                        map.invalidate()
                        onMarkersLoaded(newMarkers)
                    }
                }
            }
        } catch (e: Exception) {
            println("Error loading SOS alerts: ${e.message}")
        }
    }
}
private fun navigateToSOS(sosLocation: GeoPoint) {
    // Ini akan dipanggil dari marker click
    // Kita perlu cara untuk mengakses state endPoint dari sini
    // Solusinya adalah dengan menggunakan callback atau state hoisting
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
            println(query)
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