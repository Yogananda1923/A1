package com.adaptivetrust.mobile.data.api

import com.adaptivetrust.mobile.data.model.*
import java.util.UUID
import retrofit2.http.GET
import retrofit2.http.Path

interface EmployeeApi {
    @GET("employee/dashboard")
    suspend fun getPersonalDashboard(): EmployeeDashboardResponse

    @GET("employee/history")
    suspend fun getPersonalHistory(): List<EmployeeHistoryResponse>

    @GET("employee/history/{log_id}")
    suspend fun getPersonalLogDetail(
        @Path("log_id") logId: UUID
    ): EmployeeLogDetailResponse
}
