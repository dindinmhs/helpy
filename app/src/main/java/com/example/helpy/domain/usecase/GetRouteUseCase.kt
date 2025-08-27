package com.example.helpy.domain.usecase

import com.example.helpy.domain.model.GraphNode
import com.example.helpy.domain.pathfinding.AStarPathfinder
import com.example.helpy.domain.repository.RouteRepository
import org.osmdroid.util.GeoPoint
import kotlin.math.*

class GetRouteUseCase(
    private val routeRepository: RouteRepository
) {
    private val pathfinder = AStarPathfinder()

    suspend fun execute(startPoint: GeoPoint, endPoint: GeoPoint): Result<List<GraphNode>> {
        return try {
            val graphResult = routeRepository.getRouteGraph(startPoint, endPoint)
            
            if (graphResult.isFailure) {
                return Result.failure(graphResult.exceptionOrNull() ?: Exception("Failed to get graph"))
            }
            
            val graph = graphResult.getOrThrow()
            
            if (graph.nodes.isEmpty()) {
                return Result.success(emptyList()) // No roads found
            }

            // Find nearest nodes to start and end points
            val startNode = findNearestNode(graph.nodes, startPoint)
            val endNode = findNearestNode(graph.nodes, endPoint)

            if (startNode == null || endNode == null) {
                return Result.success(emptyList()) // Couldn't find nearest nodes
            }

            // Run A* algorithm
            val path = pathfinder.findPath(graph, startNode, endNode)
            
            Result.success(path)
        } catch (e: Exception) {
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
