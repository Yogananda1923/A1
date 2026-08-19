package com.adaptivetrust.mobile.data.api

import com.adaptivetrust.mobile.data.model.*
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/register/admin")
    suspend fun registerAdmin(
        @Body request: AdminRegisterRequest
    ): AdminRegisterResponse

    @POST("auth/register/employee")
    suspend fun registerEmployee(
        @Body request: EmployeeRegisterRequest
    ): UserResponse

    @POST("auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): LoginResponse

    @POST("auth/verify-email")
    suspend fun verifyEmail(
        @Body request: VerifyEmailRequest
    ): VerifyEmailResponse

    @POST("auth/resend-code")
    suspend fun resendCode(
        @Body request: ResendCodeRequest
    ): Map<String, Any>
}
