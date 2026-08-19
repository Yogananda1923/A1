package com.adaptivetrust.mobile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adaptivetrust.mobile.data.model.DashboardResponse
import com.adaptivetrust.mobile.data.model.EmployeeSummaryResponse
import com.adaptivetrust.mobile.data.repository.AdminRepository
import com.adaptivetrust.mobile.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    adminRepository: AdminRepository,
    onBackToAuth: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // State Variables
    var stats by remember { mutableStateOf<DashboardResponse?>(null) }
    var employees by remember { mutableStateOf<List<EmployeeSummaryResponse>>(emptyList()) }
    var selectedRole by remember { mutableStateOf<String?>(null) }
    var selectedSortBy by remember { mutableStateOf("score_desc") }
    var isLoadingStats by remember { mutableStateOf(false) }
    var isLoadingList by remember { mutableStateOf(false) }

    // Selected Employee for Override Bottom Sheet
    var selectedEmployee by remember { mutableStateOf<EmployeeSummaryResponse?>(null) }

    // Load Data
    val loadData = {
        isLoadingStats = true
        isLoadingList = true
        coroutineScope.launch {
            adminRepository.getDashboardSummary().onSuccess {
                stats = it
            }.onFailure {
                Toast.makeText(context, "Failed to load dashboard: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            isLoadingStats = false

            adminRepository.searchAndSortEmployees(selectedRole, selectedSortBy).onSuccess {
                employees = it
            }.onFailure {
                Toast.makeText(context, "Failed to load directory: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            isLoadingList = false
        }
    }

    LaunchedEffect(selectedRole, selectedSortBy) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Command Center", fontWeight = FontWeight.Bold, color = NeonCyan) },
                navigationIcon = {
                    IconButton(onClick = onBackToAuth) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Log Out", tint = NeonCyan)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Stats Row
            if (isLoadingStats) {
                Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard("Total Users", stats?.total_users?.toString() ?: "0", NeonCyan, Modifier.weight(1f))
                    StatCard("Active", stats?.active_user_count?.toString() ?: "0", TrustActive, Modifier.weight(1f))
                    StatCard("Risk Alerts", stats?.risk_alerts_count?.toString() ?: "0", TrustSuspended, Modifier.weight(1f))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filters & Sorting Layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Directory", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Role Filter Dropdown selector
                    FilterDropdown(
                        selectedOption = when (selectedRole) {
                            "ADMIN" -> "Admin"
                            "EMPLOYEE" -> "Worker"
                            else -> "All Roles"
                        },
                        options = listOf("All Roles", "Admin", "Worker"),
                        onOptionSelected = {
                            selectedRole = when (it) {
                                "Admin" -> "ADMIN"
                                "Worker" -> "EMPLOYEE"
                                else -> null
                            }
                        }
                    )

                    // Sort order dropdown selector
                    FilterDropdown(
                        selectedOption = if (selectedSortBy == "score_asc") "Score Low-High" else "Score High-Low",
                        options = listOf("Score High-Low", "Score Low-High"),
                        onOptionSelected = {
                            selectedSortBy = if (it == "Score Low-High") "score_asc" else "score_desc"
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Employee Directory
            if (isLoadingList) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else if (employees.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No workers match the requested criteria.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(employees) { emp ->
                        EmployeeItemCard(
                            employee = emp,
                            onClick = { selectedEmployee = emp }
                        )
                    }
                }
            }
        }
    }

    // Modal Bottom Sheet for Actions and Overrides
    selectedEmployee?.let { emp ->
        ModalBottomSheet(
            onDismissRequest = { selectedEmployee = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = SlateGrey
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .padding(bottom = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Override Actions for ${emp.full_name}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = "Current Trust Score: ${emp.current_score} (${emp.status})",
                    color = getScoreColor(emp.current_score),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Button(
                    onClick = {
                        coroutineScope.launch {
                            adminRepository.boostScoreOverride(emp.id).onSuccess {
                                Toast.makeText(context, "Trust score boosted to 100", Toast.LENGTH_SHORT).show()
                                selectedEmployee = null
                                loadData()
                            }.onFailure {
                                Toast.makeText(context, "Boost failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrustActive)
                ) {
                    Text("BOOST TRUST TO 100", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            adminRepository.forceMfaOverride(emp.id).onSuccess {
                                Toast.makeText(context, "Forced MFA in user Redis session", Toast.LENGTH_SHORT).show()
                                selectedEmployee = null
                                loadData()
                            }.onFailure {
                                Toast.makeText(context, "MFA Force failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrustWarning)
                ) {
                    Text("FORCE MFA CHALLENGE", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            adminRepository.lockAccountOverride(emp.id).onSuccess {
                                Toast.makeText(context, "Account Suspended & sessions revoked", Toast.LENGTH_SHORT).show()
                                selectedEmployee = null
                                loadData()
                            }.onFailure {
                                Toast.makeText(context, "Lock failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TrustSuspended)
                ) {
                    Text("SUSPEND ACCOUNT (REVOKE SESSIONS)", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, accentColor: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SlateGrey)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(title, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 24.sp, color = accentColor, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun EmployeeItemCard(employee: EmployeeSummaryResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SlateGrey)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(employee.full_name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    text = "${employee.role} | Status: ${employee.status}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Box(
                modifier = Modifier
                    .background(getScoreColor(employee.current_score), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = employee.current_score.toString(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (employee.current_score >= 40) Color.Black else Color.White
                )
            }
        }
    }
}

@Composable
fun FilterDropdown(
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(
            onClick = { expanded = true },
            colors = ButtonDefaults.buttonColors(containerColor = LightSlateGrey),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(selectedOption, fontSize = 11.sp, color = TextPrimary)
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(14.dp).padding(start = 2.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SlateGrey)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, fontSize = 12.sp, color = TextPrimary) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

fun getScoreColor(score: Int): Color {
    return when {
        score >= 70 -> TrustActive
        score >= 40 -> TrustWarning
        else -> TrustSuspended
    }
}
