package com.safespend.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object FinanceColors {
    val Positive = Color(0xFF16845B)
    val Warning = Color(0xFFB66A00)
    val Cash = Color(0xFF16845B)
    val Cobalt = Color(0xFF315FC4)
    val Canvas = Color(0xFFF7F8FA)
    val Panel = Color.White
    val Outline = Color(0xFFE5E8ED)
}

private val LightScheme = lightColorScheme(
    primary = FinanceColors.Cobalt,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9EEFC),
    onPrimaryContainer = Color(0xFF172B5C),
    secondary = FinanceColors.Positive,
    onSecondary = Color.White,
    background = FinanceColors.Canvas,
    onBackground = Color(0xFF171A21),
    surface = Color.White,
    onSurface = Color(0xFF171A21),
    surfaceVariant = Color(0xFFF0F2F5),
    onSurfaceVariant = Color(0xFF687385),
    outline = FinanceColors.Outline,
    error = Color(0xFFB42318),
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFF7A271A)
)

@Composable
fun SafeSpendTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightScheme,
        content = content
    )
}
