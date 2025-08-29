package com.example.helpy.domain.model

data class SosSignal(
    val userId: String = "",
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val timestamp: Long = 0
)
