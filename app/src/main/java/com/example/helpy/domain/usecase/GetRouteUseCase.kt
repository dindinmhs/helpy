package com.example.helpy.domain.usecase

import com.example.helpy.domain.model.GraphNode
import com.example.helpy.domain.pathfinding.AStarPathfinder
import com.example.helpy.domain.repository.RouteRepository
import org.osmdroid.util.GeoPoint
import kotlin.math.*

/**
 * GetRouteUseCase - Implementasi algoritma A* untuk pencarian rute tercepat
 * 
 * Algoritma A* (A-Star) adalah algoritma pencarian graf yang digunakan untuk
 * menemukan jalur terpendek dari node awal ke node tujuan. Algoritma ini
 * menggunakan fungsi evaluasi f(n) = g(n) + h(n), dimana:
 * - g(n) = biaya aktual dari start ke node n
 * - h(n) = estimasi biaya heuristik dari node n ke goal
 * - f(n) = estimasi total biaya jalur melalui node n
 * 
 * Keunggulan A*:
 * 1. Optimal: Selalu menemukan jalur terpendek jika heuristik admissible
 * 2. Complete: Akan menemukan solusi jika solusi ada
 * 3. Efficient: Lebih cepat dari Dijkstra karena menggunakan heuristik
 */
class GetRouteUseCase(
    private val routeRepository: RouteRepository
) {
    private val pathfinder = AStarPathfinder()

    /**
     * Executes A* pathfinding algorithm to find the shortest route
     * 
     * Steps:
     * 1. Fetch road network data from OpenStreetMap via Overpass API
     * 2. Build graph representation of road network
     * 3. Find nearest nodes to start and end points
     * 4. Run A* algorithm to find optimal path
     * 5. Return sequence of nodes representing the route
     */

    suspend fun execute(startPoint: GeoPoint, endPoint: GeoPoint): Result<List<GraphNode>> {
        return try {
            println("🔄 Getting route graph...")
            val graphResult = routeRepository.getRouteGraph(startPoint, endPoint)
            
            if (graphResult.isFailure) {
                println("❌ Failed to get graph: ${graphResult.exceptionOrNull()?.message}")
                return Result.failure(graphResult.exceptionOrNull() ?: Exception("Failed to get graph"))
            }
            
            val graph = graphResult.getOrThrow()
            
            if (graph.nodes.isEmpty()) {
                println("⚠️ No roads found in the area")
                return Result.success(emptyList()) // No roads found
            }

            println("🔍 Finding nearest nodes...")
            // Find nearest nodes to start and end points
            val startNode = findNearestNode(graph.nodes, startPoint)
            val endNode = findNearestNode(graph.nodes, endPoint)

            if (startNode == null || endNode == null) {
                println("❌ Couldn't find nearest nodes: start=$startNode, end=$endNode")
                return Result.success(emptyList()) // Couldn't find nearest nodes
            }

            println("📍 Start node: ${startNode.id} at (${startNode.lat}, ${startNode.lon})")
            println("📍 End node: ${endNode.id} at (${endNode.lat}, ${endNode.lon})")

            // Run A* algorithm
            println("🧭 Running A* pathfinding...")
            val path = pathfinder.findPath(graph, startNode, endNode)
            
            if (path.isEmpty()) {
                println("⚠️ No path found between nodes")
            } else {
                println("✅ Path found with ${path.size} nodes")
            }
            
            Result.success(path)
        } catch (e: Exception) {
            println("💥 Exception in GetRouteUseCase: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    private fun findNearestNode(nodes: Map<String, GraphNode>, target: GeoPoint): GraphNode? {
        return nodes.values.minByOrNull { node ->
            haversineDistance(node.lat, node.lon, target.latitude, target.longitude)
        }
    }

    private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val R = 6371000.0 // Earth's radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
