package com.example.helpy.data

data class SOSData(
    val userId: String = "",
    val userEmail: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "ACTIVE",
    val message: String = "Emergency SOS Alert"
)