package com.vknxworks.jellyctrl.ui.nav

import androidx.compose.ui.graphics.vector.ImageVector

/** One main tab in the bottom bar ([com.vknxworks.jellyctrl.ui.components.CtrlBottomBar]) or the side rail. */
data class TabItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector)
