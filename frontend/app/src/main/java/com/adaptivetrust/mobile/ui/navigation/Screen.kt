package com.adaptivetrust.mobile.ui.navigation

sealed class Screen(val route: String) {
    object Auth : Screen("auth")
    object AdminDashboard : Screen("admin_dashboard")
    object EmployeeDashboard : Screen("employee_dashboard")
}
