package com.adaptivetrust.mobile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.adaptivetrust.mobile.data.model.AdminRegisterRequest
import com.adaptivetrust.mobile.data.model.EmployeeRegisterRequest
import com.adaptivetrust.mobile.data.model.LoginRequest
import com.adaptivetrust.mobile.data.model.ResendCodeRequest
import com.adaptivetrust.mobile.data.model.VerifyEmailRequest
import com.adaptivetrust.mobile.data.repository.AuthRepository
import com.adaptivetrust.mobile.ui.theme.ElectricPurple
import com.adaptivetrust.mobile.ui.theme.NeonCyan
import kotlinx.coroutines.launch

@Composable
fun AuthScreen(
    authRepository: AuthRepository,
    onNavigateToAdmin: () -> Unit,
    onNavigateToEmployee: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var pendingVerificationEmail by remember { mutableStateOf("") }
    val tabs = listOf("Login", "Admin Sign Up", "Employee Sign Up")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Gradient
                Text(
                    text = "AdaptiveTrust Mobile",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp
                    ),
                    color = NeonCyan,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                if (pendingVerificationEmail.isNotBlank()) {
                    VerifyEmailTab(
                        email = pendingVerificationEmail,
                        authRepository = authRepository,
                        onVerificationSuccess = {
                            pendingVerificationEmail = ""
                            selectedTab = 0
                        },
                        onBackToLogin = {
                            pendingVerificationEmail = ""
                            selectedTab = 0
                        }
                    )
                } else {
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = NeonCyan,
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        tabs.forEachIndexed { index, title ->
                            Tab(
                                selected = selectedTab == index,
                                onClick = { selectedTab = index },
                                text = { 
                                    Text(
                                        title, 
                                        fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 12.sp
                                    ) 
                                }
                            )
                        }
                    }

                    when (selectedTab) {
                        0 -> LoginTab(
                            authRepository = authRepository,
                            onNavigateToAdmin = onNavigateToAdmin,
                            onNavigateToEmployee = onNavigateToEmployee,
                            onRequireVerification = { unverifiedEmail ->
                                pendingVerificationEmail = unverifiedEmail
                            }
                        )
                        1 -> AdminRegisterTab(
                            authRepository = authRepository,
                            onRequireVerification = { unverifiedEmail ->
                                pendingVerificationEmail = unverifiedEmail
                            }
                        )
                        2 -> EmployeeRegisterTab(
                            authRepository = authRepository,
                            onRequireVerification = { unverifiedEmail ->
                                pendingVerificationEmail = unverifiedEmail
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginTab(
    authRepository: AuthRepository,
    onNavigateToAdmin: () -> Unit,
    onNavigateToEmployee: () -> Unit,
    onRequireVerification: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(color = NeonCyan)
        } else {
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    coroutineScope.launch {
                        val res = authRepository.login(LoginRequest(email, password))
                        isLoading = false
                        res.onSuccess { response ->
                            val role = decodeRoleFromJwt(response.access_token)
                            Toast.makeText(context, "Login Successful! Role: $role", Toast.LENGTH_SHORT).show()
                            if (role == "ADMIN") onNavigateToAdmin() else onNavigateToEmployee()
                        }.onFailure { err ->
                            val msg = err.localizedMessage ?: ""
                            if (msg.contains("email_not_verified", ignoreCase = true)) {
                                Toast.makeText(context, "Email verification required.", Toast.LENGTH_LONG).show()
                                onRequireVerification(email)
                            } else {
                                Toast.makeText(context, "Login Failed: $msg", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(NeonCyan, ElectricPurple)),
                            shape = RoundedCornerShape(25.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("LOGIN", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminRegisterTab(
    authRepository: AuthRepository,
    onRequireVerification: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var companyName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = companyName,
            onValueChange = { companyName = it },
            label = { Text("Company Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(color = NeonCyan)
        } else {
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || fullName.isBlank() || companyName.isBlank()) {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    coroutineScope.launch {
                        val regRes = authRepository.registerAdmin(
                            AdminRegisterRequest(email, password, fullName, companyName)
                        )
                        isLoading = false
                        if (regRes.isSuccess) {
                            Toast.makeText(context, "Workspace created! Verification code sent to email.", Toast.LENGTH_LONG).show()
                            onRequireVerification(email)
                        } else {
                            Toast.makeText(context, "Registration Failed: ${regRes.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(NeonCyan, ElectricPurple)),
                            shape = RoundedCornerShape(25.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("CREATE WORKSPACE", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EmployeeRegisterTab(
    authRepository: AuthRepository,
    onRequireVerification: (String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var companyCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = companyCode,
            onValueChange = { companyCode = it.take(8).uppercase() },
            label = { Text("8-Character Invite Code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(color = NeonCyan)
        } else {
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank() || fullName.isBlank() || companyCode.length != 8) {
                        Toast.makeText(context, "Fill in all fields and provide an 8-char code.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    coroutineScope.launch {
                        val regRes = authRepository.registerEmployee(
                            EmployeeRegisterRequest(email, password, fullName, companyCode)
                        )
                        isLoading = false
                        if (regRes.isSuccess) {
                            Toast.makeText(context, "Registration successful! Verification code sent to email.", Toast.LENGTH_LONG).show()
                            onRequireVerification(email)
                        } else {
                            Toast.makeText(context, "Registration Failed: ${regRes.exceptionOrNull()?.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(NeonCyan, ElectricPurple)),
                            shape = RoundedCornerShape(25.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("REGISTER ACCOUNT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun VerifyEmailTab(
    email: String,
    authRepository: AuthRepository,
    onVerificationSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isResending by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Verify Email Address",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Enter the 6-digit verification code sent to:\n$email",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = otpCode,
            onValueChange = { if (it.length <= 6) otpCode = it.uppercase() },
            label = { Text("6-Digit Code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator(color = NeonCyan)
        } else {
            Button(
                onClick = {
                    if (otpCode.length != 6) {
                        Toast.makeText(context, "Please enter a valid 6-digit code", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    coroutineScope.launch {
                        val res = authRepository.verifyEmail(VerifyEmailRequest(email, otpCode))
                        isLoading = false
                        res.onSuccess {
                            Toast.makeText(context, "Email Verified Successfully! Please log in.", Toast.LENGTH_LONG).show()
                            onVerificationSuccess()
                        }.onFailure { err ->
                            Toast.makeText(context, "Verification Failed: ${err.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(listOf(NeonCyan, ElectricPurple)),
                            shape = RoundedCornerShape(25.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("VERIFY EMAIL", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(
                onClick = {
                    if (isResending) return@TextButton
                    isResending = true
                    coroutineScope.launch {
                        val resendRes = authRepository.resendCode(ResendCodeRequest(email))
                        isResending = false
                        resendRes.onSuccess {
                            Toast.makeText(context, "New verification code sent to email!", Toast.LENGTH_SHORT).show()
                        }.onFailure { err ->
                            Toast.makeText(context, "Failed to resend: ${err.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            ) {
                Text(if (isResending) "Sending..." else "Resend Code", color = NeonCyan)
            }

            TextButton(onClick = onBackToLogin) {
                Text("Back to Login", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// Lightweight manual Base64 decoder to parse JWT token role and route user without dependencies
fun decodeRoleFromJwt(token: String): String {
    return try {
        val parts = token.split(".")
        if (parts.size >= 2) {
            val payloadBytes = android.util.Base64.decode(parts[1], android.util.Base64.URL_SAFE)
            val payload = String(payloadBytes, Charsets.UTF_8)
            if (payload.contains("\"role\":\"ADMIN\"")) {
                "ADMIN"
            } else {
                "EMPLOYEE"
            }
        } else {
            "EMPLOYEE"
        }
    } catch (e: Exception) {
        "EMPLOYEE"
    }
}
