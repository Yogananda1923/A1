package com.adaptivetrust.mobile.data.model

import java.util.UUID

data class EmployeeDashboardResponse(
    val current_score: Int,
    val status: String
)

data class EmployeeHistoryResponse(
    val log_id: UUID,
    val score_before: Int,
    val score_after: Int,
    val timestamp: String
)

data class EmployeeLogDetailResponse(
    val log_id: UUID,
    val score_before: Int,
    val score_after: Int,
    val timestamp: String,
    val cause_of_change: String
)
