package com.example.helpy.data.repository

import com.example.helpy.data.remote.OverpassApiService
import com.example.helpy.domain.model.Graph
import com.example.helpy.domain.repository.RouteRepository
import org.osmdroid.util.GeoPoint

class RouteRepositoryImpl(
    private val overpassApiService: OverpassApiService
) : RouteRepository {

    private val graphBuilder = GraphBuilder()

    override suspend fun getRouteGraph(startPoint: GeoPoint, endPoint: GeoPoint): Result<Graph> {
        return try {
            // Calculate bounding box with increased padding for better coverage
            val padding = 0.005 // ~500m padding (reduced from 1km for better performance)
            val minLat = minOf(startPoint.latitude, endPoint.latitude) - padding
            val maxLat = maxOf(startPoint.latitude, endPoint.latitude) + padding
            val minLon = minOf(startPoint.longitude, endPoint.longitude) - padding
            val maxLon = maxOf(startPoint.longitude, endPoint.longitude) + padding

            // Enhanced Overpass query to get more road types
            val query = """
                [out:json][timeout:30];
                (
                  way["highway"~"^(motorway|trunk|primary|secondary|tertiary|residential|living_street|service|unclassified)$"]
                     ["access"!="private"]["access"!="no"]
                     ($minLat,$minLon,$maxLat,$maxLon);
                );
                out geom;
            """.trimIndent()

            println("🔍 Overpass Query: $query")
            println("📍 Bounding Box: ($minLat,$minLon,$maxLat,$maxLon)")

            // Fetch data from Overpass API
            val response = overpassApiService.getWaysInBoundingBox(query)
            
            println("📊 API Response: ${response.elements.size} elements found")
            
            // Build graph from Overpass data
            val graph = graphBuilder.buildGraph(response)
            
            println("🗺️ Graph built: ${graph.nodes.size} nodes, ${graph.edges.size} edge groups")
            
            Result.success(graph)
        } catch (e: Exception) {
            println("❌ Error getting route graph: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
