package com.adaptivetrust.mobile.data.api

import com.adaptivetrust.mobile.data.model.*
import java.util.UUID
import retrofit2.http.*

interface AdminApi {
    @GET("admin/dashboard")
    suspend fun getDashboardSummary(): DashboardResponse

    @GET("admin/employees")
    suspend fun listEmployeesByStatus(
        @Query("status") status: String
    ): List<EmployeeSummaryResponse>

    @GET("admin/employees/{user_id}")
    suspend fun getEmployeeDetails(
        @Path("user_id") userId: UUID
    ): EmployeeDetailResponse

    @GET("admin/employees/search")
    suspend fun searchAndSortEmployees(
        @Query("role") role: String?,
        @Query("sort_by") sortBy: String?
    ): List<EmployeeSummaryResponse>

    @POST("admin/override/mfa")
    suspend fun forceMfaOverride(
        @Body request: OverrideRequest
    ): OverrideResponse

    @POST("admin/override/lock")
    suspend fun lockAccountOverride(
        @Body request: OverrideRequest
    ): OverrideResponse

    @POST("admin/override/boost")
    suspend fun boostScoreOverride(
        @Body request: OverrideRequest
    ): OverrideResponse
}
