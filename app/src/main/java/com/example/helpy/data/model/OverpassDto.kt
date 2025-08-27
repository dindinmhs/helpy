package com.example.helpy.data.model

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
