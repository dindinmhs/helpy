package com.example.helpy.data.remote

import com.example.helpy.data.model.OverpassResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface OverpassApiService {
    @GET("interpreter")
    suspend fun getWaysInBoundingBox(@Query("data") query: String): OverpassResponse
}
