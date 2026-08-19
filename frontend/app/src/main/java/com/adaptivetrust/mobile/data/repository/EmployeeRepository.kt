package com.adaptivetrust.mobile.data.repository

import com.adaptivetrust.mobile.data.api.ApiClient
import com.adaptivetrust.mobile.data.model.*
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EmployeeRepository {
    private val api = ApiClient.employeeApi

    suspend fun getPersonalDashboard(): Result<EmployeeDashboardResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getPersonalDashboard())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getPersonalHistory(): Result<List<EmployeeHistoryResponse>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getPersonalHistory())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getPersonalLogDetail(logId: UUID): Result<EmployeeLogDetailResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getPersonalLogDetail(logId))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
