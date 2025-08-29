package com.example.helpy.data.repository

import com.example.helpy.domain.model.SosSignal
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SosRepositoryImpl(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    fun sendSos(lat: Double, lng: Double) {
        val userId = auth.currentUser?.uid ?: return
        val sosId = database.reference.child("sos_signals").push().key ?: return

        val signal = SosSignal(
            userId = userId,
            lat = lat,
            lng = lng,
            timestamp = System.currentTimeMillis()
        )

        database.reference.child("sos_signals").child(sosId).setValue(signal)
    }
}
