package com.adaptivetrust.mobile.data.model

import java.util.UUID

data class DashboardResponse(
    val total_users: Int,
    val active_user_count: Int,
    val risk_alerts_count: Int
)

data class EmployeeSummaryResponse(
    val id: UUID,
    val full_name: String,
    val role: String,
    val current_score: Int,
    val status: String,
    val last_seen_at: String?
)

data class TrustLogHistoryResponse(
    val log_id: UUID,
    val score_before: Int,
    val score_after: Int,
    val timestamp: String
)

data class EmployeeDetailResponse(
    val id: UUID,
    val full_name: String,
    val email: String,
    val role: String,
    val current_score: Int,
    val status: String,
    val last_seen_at: String?,
    val trust_logs: List<TrustLogHistoryResponse>
)

data class OverrideRequest(
    val user_id: UUID
)

data class OverrideResponse(
    val status: String,
    val message: String
)
