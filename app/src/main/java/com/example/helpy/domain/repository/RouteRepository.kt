package com.example.helpy.domain.repository

import com.example.helpy.domain.model.Graph
import org.osmdroid.util.GeoPoint

interface RouteRepository {
    suspend fun getRouteGraph(startPoint: GeoPoint, endPoint: GeoPoint): Result<Graph>
}
