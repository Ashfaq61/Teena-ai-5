package com.teena.assistant.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object TeenaColors {
    val Void = Color(0xFF060913)
    val Surface = Color(0xFF0D1322)
    val SurfaceElevated = Color(0xFF151F35)
    val Border = Color(0xFF23304D)
    val Cyan = Color(0xFF22D3EE)
    val Violet = Color(0xFF8B5CF6)
    val Emerald = Color(0xFF10B981)
    val Amber = Color(0xFFF59E0B)
    val Rose = Color(0xFFF43F5E)
    val Slate = Color(0xFF64748B)
    val TextPrimary = Color(0xFFF1F5F9)
    val TextSecondary = Color(0xFF94A3B8)
}

private val TeenaDarkColorScheme = darkColorScheme(
    primary = TeenaColors.Cyan,
    onPrimary = TeenaColors.Void,
    secondary = TeenaColors.Violet,
    onSecondary = TeenaColors.TextPrimary,
    tertiary = TeenaColors.Emerald,
    background = TeenaColors.Void,
    onBackground = TeenaColors.TextPrimary,
    surface = TeenaColors.Surface,
    onSurface = TeenaColors.TextPrimary,
    surfaceVariant = TeenaColors.SurfaceElevated,
    onSurfaceVariant = TeenaColors.TextSecondary,
    outline = TeenaColors.Border,
    error = TeenaColors.Rose,
    onError = TeenaColors.TextPrimary
)

private val TeenaTypography = Typography(
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

@Composable
fun TeenaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TeenaDarkColorScheme,
        typography = TeenaTypography,
        content = content
    )
}
