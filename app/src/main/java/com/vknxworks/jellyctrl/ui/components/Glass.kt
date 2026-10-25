package com.vknxworks.jellyctrl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vknxworks.jellyctrl.ui.theme.CtrlColors

/**
 * Space taken at the bottom of the screen by floating bars. Scrolling screens add it to their
 * bottom content padding, so their last items can scroll clear of the bar.
 */
val LocalBottomClearance = compositionLocalOf { 0.dp }

/**
 * Floating surface of the Signal look (bottom bar, mini player): the surface colour at 92% with a
 * 1dp border. No blur and no shadow.
 */
@Composable
fun Modifier.glass(
    shape: Shape,
    @Suppress("UNUSED_PARAMETER") elevation: Dp = 0.dp,
    tint: Color = CtrlColors.Surface,
): Modifier = this
    .clip(shape)
    .background(tint.copy(alpha = 0.92f))
    .border(1.dp, CtrlColors.ControlOutline, shape)
