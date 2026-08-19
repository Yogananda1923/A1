package com.adaptivetrust.mobile.data.repository

import com.adaptivetrust.mobile.data.api.ApiClient
import com.adaptivetrust.mobile.data.model.*
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AdminRepository {
    private val api = ApiClient.adminApi

    suspend fun getDashboardSummary(): Result<DashboardResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getDashboardSummary())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun listEmployeesByStatus(status: String): Result<List<EmployeeSummaryResponse>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.listEmployeesByStatus(status))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun getEmployeeDetails(userId: UUID): Result<EmployeeDetailResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.getEmployeeDetails(userId))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun searchAndSortEmployees(role: String?, sortBy: String?): Result<List<EmployeeSummaryResponse>> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.searchAndSortEmployees(role, sortBy))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun forceMfaOverride(userId: UUID): Result<OverrideResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.forceMfaOverride(OverrideRequest(userId)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun lockAccountOverride(userId: UUID): Result<OverrideResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.lockAccountOverride(OverrideRequest(userId)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun boostScoreOverride(userId: UUID): Result<OverrideResponse> =
        withContext(Dispatchers.IO) {
            try {
                Result.success(api.boostScoreOverride(OverrideRequest(userId)))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
