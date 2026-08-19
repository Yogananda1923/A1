package com.adaptivetrust.mobile.ui.screens

import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adaptivetrust.mobile.data.model.EmployeeDashboardResponse
import com.adaptivetrust.mobile.data.model.EmployeeHistoryResponse
import com.adaptivetrust.mobile.data.model.EmployeeLogDetailResponse
import com.adaptivetrust.mobile.data.repository.EmployeeRepository
import com.adaptivetrust.mobile.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDashboardScreen(
    employeeRepository: EmployeeRepository,
    onBackToAuth: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // States
    var dashboard by remember { mutableStateOf<EmployeeDashboardResponse?>(null) }
    var history by remember { mutableStateOf<List<EmployeeHistoryResponse>>(emptyList()) }
    var selectedLogDetail by remember { mutableStateOf<EmployeeLogDetailResponse?>(null) }
    var isLoadingDashboard by remember { mutableStateOf(false) }
    var isLoadingHistory by remember { mutableStateOf(false) }
    var isLoadingDetail by remember { mutableStateOf(false) }

    // Load User Data
    val loadData = {
        isLoadingDashboard = true
        isLoadingHistory = true
        coroutineScope.launch {
            employeeRepository.getPersonalDashboard().onSuccess {
                dashboard = it
            }.onFailure {
                Toast.makeText(context, "Failed to load dashboard: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            isLoadingDashboard = false

            employeeRepository.getPersonalHistory().onSuccess {
                history = it
            }.onFailure {
                Toast.makeText(context, "Failed to load history: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
            isLoadingHistory = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Trust Profile", fontWeight = FontWeight.Bold, color = NeonCyan) },
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Score Dial Component
            if (isLoadingDashboard) {
                Box(modifier = Modifier.height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else {
                dashboard?.let { dash ->
                    ScoreGauge(score = dash.current_score, status = dash.status)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Timeline Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text("Security Event Timeline", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // History Timeline list
            if (isLoadingHistory) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            } else if (history.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Text("No historical score logs recorded.", color = TextSecondary)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(history) { log ->
                        HistoryLogCard(
                            log = log,
                            onClick = {
                                isLoadingDetail = true
                                coroutineScope.launch {
                                    employeeRepository.getPersonalLogDetail(log.log_id).onSuccess {
                                        selectedLogDetail = it
                                    }.onFailure {
                                        Toast.makeText(context, "Verification Failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
                                    }
                                    isLoadingDetail = false
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Cause of Change Transparency AlertDialog
    selectedLogDetail?.let { detail ->
        AlertDialog(
            onDismissRequest = { selectedLogDetail = null },
            confirmButton = {
                TextButton(onClick = { selectedLogDetail = null }) {
                    Text("CLOSE", color = NeonCyan)
                }
            },
            title = {
                Text("Event Transparency Detail", fontWeight = FontWeight.Bold, color = NeonCyan)
            },
            text = {
                Column {
                    Text("Log Reference: ${detail.log_id}", fontSize = 11.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Score Adjustment:", fontWeight = FontWeight.Bold)
                    Text("${detail.score_before} -> ${detail.score_after}", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = getScoreColor(detail.score_after))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Timestamp:", fontWeight = FontWeight.Bold)
                    Text(detail.timestamp, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Cause of Change:", fontWeight = FontWeight.Bold)
                    Text(
                        text = detail.cause_of_change,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                }
            },
            containerColor = SlateGrey
        )
    }

    // Loading overlay for fetch details
    if (isLoadingDetail) {
        AlertDialog(
            onDismissRequest = {},
            confirmButton = {},
            title = { Text("Fetching Details...") },
            text = {
                Box(modifier = Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = NeonCyan)
                }
            },
            containerColor = SlateGrey
        )
    }
}

@Composable
fun ScoreGauge(score: Int, status: String) {
    val scoreColor = getScoreColor(score)

    // Animated score transition
    val animatedScore = animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(durationMillis = 1000),
        label = "score"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Alignment.Center
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Background Track Circle
                drawCircle(
                    color = LightSlateGrey,
                    radius = size.minDimension / 2 - 12.dp.toPx(),
                    style = Stroke(width = 16.dp.toPx())
                )

                // Foreground Trust Score Arc
                val sweepAngle = (animatedScore.value / 100f) * 360f
                drawArc(
                    color = scoreColor,
                    startAngle = -90f,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Inner Content
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = score.toString(),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Text(
                    text = status,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }
        }
    }
}

@Composable
fun HistoryLogCard(log: EmployeeHistoryResponse, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = SlateGrey)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Score: ${log.score_before} -> ${log.score_after}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = getScoreColor(log.score_after)
                )
                Text(
                    text = log.timestamp,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
            Text("DETAILS >", fontSize = 12.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
        }
    }
}
