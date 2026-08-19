package com.adaptivetrust.mobile.data.model

import java.util.UUID

data class AdminRegisterRequest(
    val email: String,
    val password: String,
    val full_name: String,
    val company_name: String
)

data class CompanyResponse(
    val id: UUID,
    val name: String,
    val company_code: String,
    val is_active: Boolean
)

data class UserResponse(
    val id: UUID,
    val company_id: UUID,
    val email: String,
    val full_name: String,
    val role: String,
    val is_active: Boolean,
    val current_score: Int,
    val is_email_verified: Boolean = false
)

data class AdminRegisterResponse(
    val admin: UserResponse,
    val company: CompanyResponse
)

data class EmployeeRegisterRequest(
    val email: String,
    val password: String,
    val full_name: String,
    val company_code: String
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val access_token: String,
    val token_type: String
)

data class VerifyEmailRequest(
    val email: String,
    val code: String
)

data class ResendCodeRequest(
    val email: String
)

data class VerifyEmailResponse(
    val message: String,
    val email: String,
    val is_email_verified: Boolean
)
