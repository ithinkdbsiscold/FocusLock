package com.focuslock.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val FocusDark = darkColorScheme(
    primary = Color(0xFFD4A574),
    onPrimary = Color(0xFF0A0908),
    primaryContainer = Color(0xFF3D2E1F),
    onPrimaryContainer = Color(0xFFE8D5C4),
    secondary = Color(0xFFC4A882),
    onSecondary = Color(0xFF0A0908),
    secondaryContainer = Color(0xFF3A2F22),
    onSecondaryContainer = Color(0xFFE8D5C4),
    tertiary = Color(0xFFD4A574),
    background = Color(0xFF0A0908),
    onBackground = Color(0xFFE8E0D8),
    surface = Color(0xFF141210),
    onSurface = Color(0xFFE8E0D8),
    surfaceVariant = Color(0xFF1E1B18),
    onSurfaceVariant = Color(0xFF9C9490),
    outline = Color(0xFF4A423C),
    outlineVariant = Color(0xFF332D28),
    error = Color(0xFFFF6B6B),
    onError = Color(0xFF0A0908),
    errorContainer = Color(0xFF3D1F1F),
    inverseSurface = Color(0xFFE8E0D8),
    inverseOnSurface = Color(0xFF0A0908),
    inversePrimary = Color(0xFF7A5530),
    surfaceTint = Color(0xFFD4A574),
    scrim = Color(0xFF000000)
)

private val FocusLight = lightColorScheme(
    primary = Color(0xFF7A5530),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8D5C4),
    background = Color(0xFFFAF6F2),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF0EAE4),
    onBackground = Color(0xFF1A1614),
    onSurface = Color(0xFF1A1614),
    onSurfaceVariant = Color(0xFF6B5F56),
    outline = Color(0xFFB0A49A),
    error = Color(0xFFCC3333)
)

private val FocusTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 44.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1.5).sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.1.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.3.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.8.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.4.sp
    )
)

@Composable
fun FocusLockTheme(darkTheme: Boolean = true, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) FocusDark else FocusLight,
        typography = FocusTypography,
        content = content
    )
}
