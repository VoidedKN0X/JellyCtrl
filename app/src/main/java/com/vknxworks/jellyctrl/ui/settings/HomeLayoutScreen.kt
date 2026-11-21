package com.vknxworks.jellyctrl.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.data.settings.AppSettings
import com.vknxworks.jellyctrl.data.settings.HomeSection
import com.vknxworks.jellyctrl.data.settings.moveHomeSection
import com.vknxworks.jellyctrl.ui.components.CtrlIconSquare
import com.vknxworks.jellyctrl.ui.components.CtrlSwitch
import com.vknxworks.jellyctrl.ui.components.Group
import com.vknxworks.jellyctrl.ui.components.GroupRow
import com.vknxworks.jellyctrl.ui.components.ScreenTopBar
import com.vknxworks.jellyctrl.ui.components.ctrlTopDivider
import kotlinx.coroutines.launch

@get:StringRes
private val HomeSection.title: Int
    get() = when (this) {
        HomeSection.CONTINUE_CARD -> R.string.home_section_continue_card
        HomeSection.FEATURED -> R.string.home_section_featured
        HomeSection.CONTINUE_WATCHING -> R.string.continue_watching
        HomeSection.NEXT_UP -> R.string.next_up
        HomeSection.UPCOMING -> R.string.upcoming_episodes
        HomeSection.LIVE_TV -> R.string.on_tv_now
        HomeSection.CONTINUE_LISTENING -> R.string.keep_listening
        HomeSection.LATEST -> R.string.home_section_latest
    }

@get:StringRes
private val HomeSection.summary: Int
    get() = when (this) {
        HomeSection.CONTINUE_CARD -> R.string.home_section_continue_card_summary
        HomeSection.FEATURED -> R.string.home_section_featured_summary
        HomeSection.CONTINUE_WATCHING -> R.string.home_section_continue_watching_summary
        HomeSection.NEXT_UP -> R.string.home_section_next_up_summary
        HomeSection.UPCOMING -> R.string.home_section_upcoming_summary
        HomeSection.LIVE_TV -> R.string.home_section_live_tv_summary
        HomeSection.CONTINUE_LISTENING -> R.string.home_section_continue_listening_summary
        HomeSection.LATEST -> R.string.home_section_latest_summary
    }

/** Settings > Home screen: which rows Home shows, and in what order. */
@Composable
fun HomeLayoutScreen() {
    val c = LocalContext.current.container
    val scope = rememberCoroutineScope()
    val s by c.settings.settings.collectAsStateWithLifecycle()
    fun update(t: (AppSettings) -> AppSettings) { scope.launch { c.settings.update(t) } }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { ScreenTopBar(stringResource(R.string.home_layout)) },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
            Group(stringResource(R.string.home_layout_rows), footer = stringResource(R.string.home_layout_footer)) {
                s.homeOrder.forEachIndexed { i, section ->
                    val shown = section !in s.homeHidden
                    Row(
                        Modifier.fillMaxWidth().ctrlTopDivider().heightIn(min = 64.dp).padding(start = 16.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(section.title),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                                color = if (shown) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                stringResource(section.summary),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        val name = stringResource(section.title)
                        CtrlIconSquare(
                            Icons.Outlined.KeyboardArrowUp,
                            stringResource(R.string.home_move_up_x, name),
                            onClick = { update { it.copy(homeOrder = moveHomeSection(it.homeOrder, section, -1)) } },
                            size = 40.dp,
                            enabled = i > 0,
                        )
                        Spacer(Modifier.width(6.dp))
                        CtrlIconSquare(
                            Icons.Outlined.KeyboardArrowDown,
                            stringResource(R.string.home_move_down_x, name),
                            onClick = { update { it.copy(homeOrder = moveHomeSection(it.homeOrder, section, 1)) } },
                            size = 40.dp,
                            enabled = i < s.homeOrder.lastIndex,
                        )
                        Spacer(Modifier.width(10.dp))
                        CtrlSwitch(shown, onCheckedChange = { on ->
                            update { it.copy(homeHidden = if (on) it.homeHidden - section else it.homeHidden + section) }
                        })
                    }
                }
            }
            Group {
                GroupRow(
                    stringResource(R.string.home_layout_reset),
                    stringResource(R.string.home_layout_reset_summary),
                    icon = Icons.Outlined.Restore,
                    onClick = { update { it.copy(homeOrder = HomeSection.entries, homeHidden = emptySet()) } },
                )
            }
        }
    }
}
