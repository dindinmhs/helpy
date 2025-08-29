package com.example.helpy.ui.screens.sos

import androidx.lifecycle.ViewModel
import com.example.helpy.data.repository.SosRepositoryImpl

class SosViewModel(
    private val sosRepository: SosRepositoryImpl
) : ViewModel() {
    fun sendSos(lat: Double, lng: Double) {
        sosRepository.sendSos(lat, lng)
    }
}
