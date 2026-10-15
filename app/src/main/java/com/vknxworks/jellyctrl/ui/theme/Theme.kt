package com.vknxworks.jellyctrl.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.vknxworks.jellyctrl.data.settings.ThemeChoice

val JellyBlue = Color(0xFF00A4DC)
val JellyPurple = Color(0xFFAA5CC3)

private val Dark = darkColorScheme(
    primary = JellyBlue,
    onPrimary = Color(0xFF00141C),
    primaryContainer = Color(0xFF003A50),
    onPrimaryContainer = Color(0xFFBFE9FF),
    secondary = JellyPurple,
    onSecondary = Color(0xFF1E0628),
    secondaryContainer = Color(0xFF4A2357),
    onSecondaryContainer = Color(0xFFF6D8FF),
    tertiary = Color(0xFF7FD6C2),
    background = Color(0xFF101015),
    onBackground = Color(0xFFE5E1E9),
    surface = Color(0xFF101015),
    onSurface = Color(0xFFE5E1E9),
    surfaceVariant = Color(0xFF2A2833),
    onSurfaceVariant = Color(0xFFC9C5D0),
    surfaceContainerLowest = Color(0xFF0B0B0F),
    surfaceContainerLow = Color(0xFF16161C),
    surfaceContainer = Color(0xFF1B1A21),
    surfaceContainerHigh = Color(0xFF25242C),
    surfaceContainerHighest = Color(0xFF302F37),
    outline = Color(0xFF928F9A),
    outlineVariant = Color(0xFF47454F),
)

private val Light = lightColorScheme(
    primary = Color(0xFF00658A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC4E7FF),
    onPrimaryContainer = Color(0xFF001E2C),
    secondary = Color(0xFF7D3D92),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF6D8FF),
    onSecondaryContainer = Color(0xFF320044),
)

@Composable
fun JellyCtrlTheme(choice: ThemeChoice = ThemeChoice.SYSTEM, dynamic: Boolean = false, content: @Composable () -> Unit) {
    val dark = when (choice) {
        ThemeChoice.SYSTEM -> isSystemInDarkTheme()
        ThemeChoice.LIGHT -> false
        ThemeChoice.DARK, ThemeChoice.BLACK -> true
    }
    val ctx = LocalContext.current
    var scheme = when {
        dynamic && dark -> dynamicDarkColorScheme(ctx)
        dynamic -> dynamicLightColorScheme(ctx)
        dark -> Dark
        else -> Light
    }
    if (choice == ThemeChoice.BLACK) {
        scheme = scheme.copy(
            background = Color.Black,
            surface = Color.Black,
            surfaceContainerLowest = Color.Black,
            surfaceContainerLow = Color(0xFF0A0A0C),
            surfaceContainer = Color(0xFF111114),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
