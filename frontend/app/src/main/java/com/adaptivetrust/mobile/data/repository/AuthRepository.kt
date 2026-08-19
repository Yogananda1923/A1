package com.adaptivetrust.mobile.data.repository

import com.adaptivetrust.mobile.data.api.ApiClient
import com.adaptivetrust.mobile.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AuthRepository {
    private val api = ApiClient.authApi

    suspend fun registerAdmin(request: AdminRegisterRequest): Result<AdminRegisterResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.registerAdmin(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun registerEmployee(request: EmployeeRegisterRequest): Result<UserResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.registerEmployee(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun login(request: LoginRequest): Result<LoginResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.login(request)
                ApiClient.token = response.access_token
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun verifyEmail(request: VerifyEmailRequest): Result<VerifyEmailResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.verifyEmail(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    suspend fun resendCode(request: ResendCodeRequest): Result<Map<String, Any>> =
        withContext(Dispatchers.IO) {
            try {
                val response = api.resendCode(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    fun logout() {
        ApiClient.token = null
    }

    fun isLoggedIn(): Boolean {
        return ApiClient.token != null
    }
}
