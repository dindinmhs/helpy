package com.example.helpy.domain.model

import kotlin.math.*

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

private fun haversineDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val R = 6371000.0 // Earth's radius in meters
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return R * c
}
