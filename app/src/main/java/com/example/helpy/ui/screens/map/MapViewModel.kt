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
            println("⚠️ Cannot find route: start=$start, end=$end, loading=${_isLoadingRoute.value}")
            return
        }

        println("🚀 Starting route finding from (${start.latitude}, ${start.longitude}) to (${end.latitude}, ${end.longitude})")

        viewModelScope.launch {
            _isLoadingRoute.value = true
            try {
                val result = getRouteUseCase.execute(start, end)
                if (result.isSuccess) {
                    val path = result.getOrThrow()
                    println("✅ Route found with ${path.size} nodes")
                    _routePath.value = path
                } else {
                    println("❌ Route finding failed: ${result.exceptionOrNull()?.message}")
                    _routePath.value = emptyList()
                }
            } catch (e: Exception) {
                println("💥 Exception during route finding: ${e.message}")
                e.printStackTrace()
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
