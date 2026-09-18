package com.example.amrapaligm.api

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class VisitorRequest(
    val name: String,
    val flatNumbers: String,
    val phoneNumber: String,
    val image: String // Base64 encoded image
)

data class VisitorResponse(
    val message: String,
    val visitor: Visitor
)

data class Visitor(
    val name: String,
    val flatNumbers: String,
    val phoneNumber: String,
    val image: String,
    val entryDateTime: String
)

interface VisitorApi {
    @GET("api/health")
    suspend fun checkConnection(): Response<Unit>

    @POST("api/visitor")
    suspend fun addVisitor(@Body visitorRequest: VisitorRequest): Response<VisitorResponse>
} 