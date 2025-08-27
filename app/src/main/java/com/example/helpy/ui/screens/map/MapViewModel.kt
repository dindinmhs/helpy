package com.example.helpy.ui.screens.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.helpy.domain.model.GraphNode
import com.example.helpy.domain.usecase.GetRouteUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.osmdroid.util.GeoPoint

class MapViewModel(
    private val getRouteUseCase: GetRouteUseCase
) : ViewModel() {

    private val _isLoadingRoute = MutableStateFlow(false)
    val isLoadingRoute: StateFlow<Boolean> = _isLoadingRoute.asStateFlow()

    private val _routePath = MutableStateFlow<List<GraphNode>>(emptyList())
    val routePath: StateFlow<List<GraphNode>> = _routePath.asStateFlow()

    private val _startPoint = MutableStateFlow<GeoPoint?>(null)
    val startPoint: StateFlow<GeoPoint?> = _startPoint.asStateFlow()

    private val _endPoint = MutableStateFlow<GeoPoint?>(null)
    val endPoint: StateFlow<GeoPoint?> = _endPoint.asStateFlow()

    fun setStartPoint(point: GeoPoint) {
        _startPoint.value = point
    }

    fun setEndPoint(point: GeoPoint) {
        _endPoint.value = point
    }

    fun findRoute() {
        val start = _startPoint.value
        val end = _endPoint.value
        
        if (start == null || end == null || _isLoadingRoute.value) {
            return
        }

        viewModelScope.launch {
            _isLoadingRoute.value = true
            try {
                val result = getRouteUseCase.execute(start, end)
                if (result.isSuccess) {
                    _routePath.value = result.getOrThrow()
                } else {
                    // Handle error - could emit error state
                    _routePath.value = emptyList()
                }
            } catch (e: Exception) {
                // Handle error
                _routePath.value = emptyList()
            } finally {
                _isLoadingRoute.value = false
            }
        }
    }

    fun clearRoute() {
        _routePath.value = emptyList()
    }
}
