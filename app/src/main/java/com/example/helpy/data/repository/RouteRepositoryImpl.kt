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
            // Calculate bounding box with some padding
            val padding = 0.01 // ~1km padding
            val minLat = minOf(startPoint.latitude, endPoint.latitude) - padding
            val maxLat = maxOf(startPoint.latitude, endPoint.latitude) + padding
            val minLon = minOf(startPoint.longitude, endPoint.longitude) - padding
            val maxLon = maxOf(startPoint.longitude, endPoint.longitude) + padding

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
            val response = overpassApiService.getWaysInBoundingBox(query)
            
            // Build graph from Overpass data
            val graph = graphBuilder.buildGraph(response)
            
            Result.success(graph)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
