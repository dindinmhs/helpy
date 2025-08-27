package com.example.helpy.data.remote

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface OpenRouteService {
    @GET("v2/directions/driving-car")
    suspend fun getRoute(
        @Query("api_key") apiKey: String,
        @Query("start") start: String, // "lon,lat"
        @Query("end") end: String, // "lon,lat"
        @Query("format") format: String = "geojson"
    ): Response<OpenRouteResponse>
}

data class OpenRouteResponse(
    val features: List<Feature>
)

data class Feature(
    val geometry: Geometry,
    val properties: Properties
)

data class Geometry(
    val coordinates: List<List<Double>>
)

data class Properties(
    val segments: List<Segment>
)

data class Segment(
    val distance: Double,
    val duration: Double
)
