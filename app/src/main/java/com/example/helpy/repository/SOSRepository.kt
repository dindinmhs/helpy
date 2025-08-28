package com.example.helpy.repository

import com.example.helpy.data.SOSData
import com.google.firebase.Firebase
import com.google.firebase.database.database
import kotlinx.coroutines.tasks.await

class SOSRepository {
    private val database = Firebase.database

    suspend fun sendSOSAlert(sosData: SOSData): Result<String> {
        return try {
            val sosRef = database.getReference("sos_alerts").push()
            val sosId = sosRef.key ?: throw Exception("Failed to generate SOS ID")

            sosRef.setValue(sosData).await()
            Result.success(sosId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateSOSStatus(sosId: String, status: String): Result<Unit> {
        return try {
            database.getReference("sos_alerts")
                .child(sosId)
                .child("status")
                .setValue(status)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllActiveSOSAlerts(): Result<List<SOSData>> {
        return try {
            val snapshot = database.getReference("sos_alerts")
                .orderByChild("status")
                .equalTo("ACTIVE")
                .get()
                .await()

            val sosList = mutableListOf<SOSData>()
            snapshot.children.forEach { child ->
                child.getValue(SOSData::class.java)?.let { sosData ->
                    sosList.add(sosData)
                }
            }
            Result.success(sosList)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserSOSHistory(userId: String): Result<List<SOSData>> {
        return try {
            val snapshot = database.getReference("sos_alerts")
                .orderByChild("userId")
                .equalTo(userId)
                .get()
                .await()

            val sosList = mutableListOf<SOSData>()
            snapshot.children.forEach { child ->
                child.getValue(SOSData::class.java)?.let { sosData ->
                    sosList.add(sosData)
                }
            }
            Result.success(sosList.sortedByDescending { it.timestamp })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}