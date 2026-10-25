package com.vknxworks.jellyctrl.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.vknxworks.jellyctrl.R

/** One set of Ctrl / Signal colours. [CtrlColors.Dark] and [CtrlColors.Light] are the two the app uses. */
@Immutable
data class CtrlPalette(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val raised: Color,
    /** Card borders. */
    val outline: Color,
    /** Borders on buttons, fields and the floating bar. */
    val controlOutline: Color,
    val divider: Color,
    val text: Color,
    val muted: Color,
    /** Disabled and stopped states. */
    val inactive: Color,
    /** Accent for text, icons, thin lines and focus borders (the fill colour is too pale for text on white). */
    val accentText: Color,
    val progressTrack: Color,
    val progressFill: Color,
    val switchTrackOff: Color,
    val switchKnobOff: Color,
    val pinEmpty: Color,
    val error: Color,
    val onError: Color,
    val warning: Color,
)

/** The palette for the current theme. Set by [JellyCtrlTheme]; read it through [CtrlColors]. */
val LocalCtrlColors = staticCompositionLocalOf { CtrlColors.Dark }

/**
 * Ctrl / Signal colours, shared by every Ctrl series app, in a dark and a light version that follow the
 * phone's dark theme setting. [Accent] is the one place the accent is set. The other tokens read the
 * current palette, so they can only be used while composing.
 */
object CtrlColors {
    /** Accent fill: hero cards, primary buttons, the selected tab, switch tracks, PIN dots. Same in both themes. */
    val Accent = Color(0xFFB48CFF)
    /** Content on an accent fill. */
    val OnAccent = Color(0xFF08080A)

    val Dark = CtrlPalette(
        isDark = true,
        background = Color(0xFF08080A),
        surface = Color(0xFF141417),
        raised = Color(0xFF1C1C21),
        outline = Color(0xFF232328),
        controlOutline = Color(0xFF2A2A2F),
        divider = Color(0xFF1E1E23),
        text = Color(0xFFF4F4F2),
        muted = Color(0xFFA3A3AB),
        inactive = Color(0xFF8C8C94),
        accentText = Accent,
        progressTrack = Color(0xFF26262C),
        progressFill = Color(0xFFF4F4F2),
        switchTrackOff = Color(0xFF2A2A30),
        switchKnobOff = Color(0xFF8C8C94),
        pinEmpty = Color(0xFF3A3A41),
        error = Color(0xFFFF6B6B),
        onError = Color(0xFF08080A),
        warning = Color(0xFFF2C38B),
    )

    val Light = CtrlPalette(
        isDark = false,
        background = Color(0xFFF5F5F1),
        surface = Color(0xFFFFFFFF),
        raised = Color(0xFFECECE7),
        outline = Color(0xFFDEDED7),
        controlOutline = Color(0xFFD2D2CA),
        divider = Color(0xFFE6E6E0),
        text = Color(0xFF0B0B0D),
        muted = Color(0xFF5C5C64),
        inactive = Color(0xFF74747C),
        accentText = Color(0xFF6A3BDB),
        progressTrack = Color(0xFFE2E2DC),
        progressFill = Color(0xFF0B0B0D),
        switchTrackOff = Color(0xFFDCDCD5),
        switchKnobOff = Color(0xFF74747C),
        pinEmpty = Color(0xFFC9C9C1),
        error = Color(0xFFD93636),
        onError = Color(0xFFFFFFFF),
        warning = Color(0xFF9A5B00),
    )

    val current: CtrlPalette @Composable @ReadOnlyComposable get() = LocalCtrlColors.current
    val isDark: Boolean @Composable @ReadOnlyComposable get() = current.isDark

    val Background: Color @Composable @ReadOnlyComposable get() = current.background
    val Surface: Color @Composable @ReadOnlyComposable get() = current.surface
    val Raised: Color @Composable @ReadOnlyComposable get() = current.raised
    val Outline: Color @Composable @ReadOnlyComposable get() = current.outline
    val ControlOutline: Color @Composable @ReadOnlyComposable get() = current.controlOutline
    val Divider: Color @Composable @ReadOnlyComposable get() = current.divider
    val Text: Color @Composable @ReadOnlyComposable get() = current.text
    val Muted: Color @Composable @ReadOnlyComposable get() = current.muted
    val Inactive: Color @Composable @ReadOnlyComposable get() = current.inactive
    val AccentText: Color @Composable @ReadOnlyComposable get() = current.accentText
    val ProgressTrack: Color @Composable @ReadOnlyComposable get() = current.progressTrack
    val ProgressFill: Color @Composable @ReadOnlyComposable get() = current.progressFill
    val SwitchTrackOff: Color @Composable @ReadOnlyComposable get() = current.switchTrackOff
    val SwitchKnobOff: Color @Composable @ReadOnlyComposable get() = current.switchKnobOff
    val PinEmpty: Color @Composable @ReadOnlyComposable get() = current.pinEmpty
    val Error: Color @Composable @ReadOnlyComposable get() = current.error
    val Warning: Color @Composable @ReadOnlyComposable get() = current.warning
}

/** Corner sizes of the Signal look. */
object CtrlShapes {
    val Card = RoundedCornerShape(22.dp)
    val Hero = RoundedCornerShape(26.dp)
    val Control = RoundedCornerShape(14.dp)
    val Button = RoundedCornerShape(16.dp)
    val MainButton = RoundedCornerShape(18.dp)
    val Field = RoundedCornerShape(16.dp)
    val Badge = RoundedCornerShape(10.dp)
    val Chip = RoundedCornerShape(10.dp)
    val Dot = RoundedCornerShape(2.dp)
    val Poster = RoundedCornerShape(14.dp)
    val Dialog = RoundedCornerShape(26.dp)
    val SheetTop = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
}

private fun signalScheme(p: CtrlPalette) = (if (p.isDark) darkColorScheme() else lightColorScheme()).copy(
    // Material's "primary" is used for accent text and icons (text buttons, selected rows, links); fills use
    // the container colours or [CtrlColors.Accent] directly.
    primary = p.accentText,
    onPrimary = if (p.isDark) CtrlColors.OnAccent else Color.White,
    primaryContainer = CtrlColors.Accent,
    onPrimaryContainer = CtrlColors.OnAccent,
    inversePrimary = p.accentText,
    secondary = p.text,
    onSecondary = p.background,
    secondaryContainer = CtrlColors.Accent,
    onSecondaryContainer = CtrlColors.OnAccent,
    tertiary = p.warning,
    onTertiary = p.background,
    tertiaryContainer = p.raised,
    onTertiaryContainer = p.warning,
    background = p.background,
    onBackground = p.text,
    surface = p.surface,
    onSurface = p.text,
    surfaceVariant = p.raised,
    onSurfaceVariant = p.muted,
    surfaceTint = Color.Transparent,
    surfaceBright = p.raised,
    surfaceDim = p.background,
    surfaceContainerLowest = p.background,
    surfaceContainerLow = p.surface,
    surfaceContainer = p.surface,
    surfaceContainerHigh = p.surface,
    surfaceContainerHighest = p.raised,
    inverseSurface = p.text,
    inverseOnSurface = p.background,
    outline = p.controlOutline,
    outlineVariant = p.outline,
    error = p.error,
    onError = p.onError,
    errorContainer = p.raised,
    onErrorContainer = p.error,
    scrim = Color.Black,
)

private val SignalDark = signalScheme(CtrlColors.Dark)
private val SignalLight = signalScheme(CtrlColors.Light)

private fun grotesk(weight: Int) = Font(
    R.font.space_grotesk,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

/** Space Grotesk (SIL OFL 1.1): all UI text and headings. */
val SpaceGrotesk = FontFamily(grotesk(400), grotesk(500), grotesk(600), grotesk(700))

/** JetBrains Mono (SIL OFL 1.1): numbers, data readouts, section labels and the wordmark. */
val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
)

private fun style(size: Int, line: Int, weight: Int, tracking: Double = 0.0) = TextStyle(
    fontFamily = SpaceGrotesk,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = FontWeight(weight),
    letterSpacing = tracking.em,
)

/** Text styles of the Signal look that Material's type scale has no slot for. */
object CtrlType {
    /** Section labels: JetBrains Mono 11sp, spaced out. Pass the text in UPPERCASE. */
    val Label = TextStyle(fontFamily = JetBrainsMono, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Normal, letterSpacing = 0.16.em)
    /** Bold labels on accent cards. */
    val LabelStrong = Label.copy(fontWeight = FontWeight.SemiBold)
    /** The "JELLY/CTRL" wordmark. */
    val Wordmark = TextStyle(fontFamily = JetBrainsMono, fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.14.em)
    /** Small data readouts: times, codes, counts. */
    val Mono = TextStyle(fontFamily = JetBrainsMono, fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.Normal)
    val MonoMedium = Mono.copy(fontWeight = FontWeight.Medium)
    /** Giant numeric readout on hero cards. */
    val BigNumber = TextStyle(fontFamily = SpaceGrotesk, fontSize = 64.sp, lineHeight = 60.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.05).em)
    /** PIN pad digits. */
    val Key = TextStyle(fontFamily = JetBrainsMono, fontSize = 26.sp, lineHeight = 30.sp, fontWeight = FontWeight.Medium)
}

private val SignalTypography = Typography(
    displayLarge = style(56, 56, 700, -0.04),
    displayMedium = style(48, 50, 700, -0.04),
    displaySmall = style(40, 42, 700, -0.035),
    headlineLarge = style(40, 42, 700, -0.035),
    headlineMedium = style(30, 34, 700, -0.03),
    headlineSmall = style(24, 30, 700, -0.02),
    titleLarge = style(20, 26, 600, -0.01),
    titleMedium = style(18, 24, 600),
    titleSmall = style(15, 20, 600),
    bodyLarge = style(16, 24, 400),
    bodyMedium = style(14, 21, 400),
    bodySmall = style(12, 17, 400),
    labelLarge = style(14, 20, 600),
    labelMedium = style(12, 16, 500),
    labelSmall = style(11, 14, 500, 0.02),
)

private val SignalShapes = Shapes(
    extraSmall = RoundedCornerShape(9.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(26.dp),
)

/** Horizontal page margin used everywhere. */
val Gutter = 20.dp

/**
 * The Ctrl / Signal theme. Follows the phone's dark theme setting unless [dark] is given (the video player
 * always passes true). No wallpaper (Material You) colours.
 */
@Composable
fun JellyCtrlTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val palette = if (dark) CtrlColors.Dark else CtrlColors.Light
    CompositionLocalProvider(LocalCtrlColors provides palette) {
        MaterialTheme(colorScheme = if (dark) SignalDark else SignalLight, typography = SignalTypography, shapes = SignalShapes, content = content)
    }
}

/** Always the dark Signal palette, whatever the phone uses: for content-first screens like the photo viewer. */
@Composable
fun ForceDarkSignal(content: @Composable () -> Unit) {
    JellyCtrlTheme(dark = true, content = content)
}

/**
 * Pages used to be re-coloured around their artwork. The Signal look keeps one accent across the
 * whole app, so this now just shows [content] in the normal theme. [seed] is ignored.
 */
@Composable
fun ArtworkTint(@Suppress("UNUSED_PARAMETER") seed: Color?, content: @Composable () -> Unit) {
    content()
}
