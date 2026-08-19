package com.adaptivetrust.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.adaptivetrust.mobile.data.repository.AdminRepository
import com.adaptivetrust.mobile.data.repository.AuthRepository
import com.adaptivetrust.mobile.data.repository.EmployeeRepository
import com.adaptivetrust.mobile.ui.navigation.Screen
import com.adaptivetrust.mobile.ui.screens.AdminDashboardScreen
import com.adaptivetrust.mobile.ui.screens.AuthScreen
import com.adaptivetrust.mobile.ui.screens.EmployeeDashboardScreen
import com.adaptivetrust.mobile.ui.theme.AdaptiveTrustTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Instantiate Repositories
        val authRepository = AuthRepository()
        val adminRepository = AdminRepository()
        val employeeRepository = EmployeeRepository()

        setContent {
            AdaptiveTrustTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Auth.route
                    ) {
                        composable(Screen.Auth.route) {
                            AuthScreen(
                                authRepository = authRepository,
                                onNavigateToAdmin = {
                                    navController.navigate(Screen.AdminDashboard.route) {
                                        popUpTo(Screen.Auth.route) { inclusive = false }
                                    }
                                },
                                onNavigateToEmployee = {
                                    navController.navigate(Screen.EmployeeDashboard.route) {
                                        popUpTo(Screen.Auth.route) { inclusive = false }
                                    }
                                }
                            )
                        }

                        composable(Screen.AdminDashboard.route) {
                            AdminDashboardScreen(
                                adminRepository = adminRepository,
                                onBackToAuth = {
                                    authRepository.logout()
                                    navController.navigate(Screen.Auth.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Screen.EmployeeDashboard.route) {
                            EmployeeDashboardScreen(
                                employeeRepository = employeeRepository,
                                onBackToAuth = {
                                    authRepository.logout()
                                    navController.navigate(Screen.Auth.route) {
                                        popUpTo(0) { inclusive = true }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
