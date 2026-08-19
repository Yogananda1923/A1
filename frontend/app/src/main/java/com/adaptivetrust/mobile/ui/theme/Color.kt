package com.adaptivetrust.mobile.ui.theme

import androidx.compose.ui.graphics.Color

// Premium Dark Mode Palette
val DeepCharcoal = Color(0xFF121212)
val SlateGrey = Color(0xFF1E1E1E)
val LightSlateGrey = Color(0xFF2D2D2D)
val NeonCyan = Color(0xFF00E5FF)
val ElectricPurple = Color(0xFF7C4DFF)

// Neutral Text
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0B0B0)

// Safety Trust Score Indicators
val TrustActive = Color(0xFF00E676)     // Safe Green (score >= 70)
val TrustWarning = Color(0xFFFFD600)    // Warning Amber (40 <= score < 70)
val TrustSuspended = Color(0xFFFF1744)  // Suspended Red (score < 40)
