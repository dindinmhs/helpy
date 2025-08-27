package com.example.helpy.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.helpy.domain.usecase.GetRouteUseCase

class MapViewModelFactory(
    private val getRouteUseCase: GetRouteUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MapViewModel(getRouteUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
