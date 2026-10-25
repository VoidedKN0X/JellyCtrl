package com.vknxworks.jellyctrl.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.vknxworks.jellyctrl.ui.theme.CtrlColors
import com.vknxworks.jellyctrl.ui.theme.CtrlShapes
import com.vknxworks.jellyctrl.ui.theme.CtrlType
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import com.vknxworks.jellyctrl.ui.nav.ProfileRoute
import com.vknxworks.jellyctrl.ui.theme.Gutter
import androidx.compose.ui.res.stringResource
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str

// ---------- Top bars ----------

/** Top bar for pushed screens: square back button and a title (see [CtrlSubTopBar]). */
@Composable
fun ScreenTopBar(
    title: String,
    showBack: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    CtrlSubTopBar(title, showBack, actions)
}

/**
 * Header of the main tabs: the JELLY/CTRL wordmark and your profile picture on top, then a mono
 * label and the big page title.
 */
@Composable
fun TabTitle(
    title: String,
    modifier: Modifier = Modifier,
    padded: Boolean = true,
    showProfile: Boolean = true,
    label: String? = null,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Column(modifier.fillMaxWidth()) {
        CtrlTopBar(padded = padded, showProfile = showProfile, trailing = trailing)
        Column(
            Modifier.fillMaxWidth().padding(horizontal = if (padded) Gutter else 0.dp).padding(top = 14.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (label != null) CtrlSectionLabel(label)
            Text(title, style = MaterialTheme.typography.headlineLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** The signed-in user's Jellyfin picture (or initials); tapping it opens the You page. */
@Composable
fun ProfileButton() {
    CtrlAvatarButton()
}

// ---------- Grouped lists (settings, profile, admin) ----------

/** A card of related rows with a mono label above it; rows are separated by 1dp dividers. */
@Composable
fun Group(
    title: String? = null,
    modifier: Modifier = Modifier,
    footer: String? = null,
    padded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier.fillMaxWidth().padding(horizontal = if (padded) Gutter else 0.dp, vertical = 8.dp)) {
        if (title != null) {
            CtrlSectionLabel(title, Modifier.padding(start = 4.dp, bottom = 8.dp))
        }
        Column(Modifier.fillMaxWidth().ctrlSurface()) {
            // The first row's divider is drawn off the top edge, so lines only sit between rows.
            Column(Modifier.hideFirstDivider(), content = content)
        }
        if (footer != null) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
            )
        }
    }
}

@Composable
fun GroupRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    showChevron: Boolean = false,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .ctrlTopDivider()
            .heightIn(min = 60.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(21.dp), tint = iconTint)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = titleColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(12.dp))
            trailing()
        } else if (showChevron) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun GroupSwitch(title: String, subtitle: String? = null, checked: Boolean, icon: ImageVector? = null, onChange: (Boolean) -> Unit) {
    // The whole row toggles; the switch itself only shows the state, so TalkBack reads one switch.
    Box(Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onChange)) {
        GroupRow(title, subtitle, icon) {
            CtrlSwitch(checked, onCheckedChange = null)
        }
    }
}

/** A row showing the current value; tapping opens a sheet to pick another. */
@Composable
fun <T> GroupChoice(
    title: String,
    options: List<Pair<T, String>>,
    selected: T?,
    icon: ImageVector? = null,
    fallbackLabel: String = "",
    onPick: (T) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val current = options.firstOrNull { it.first == selected }?.second ?: fallbackLabel
    GroupRow(title, current, icon, showChevron = true, onClick = { open = true })
    if (open) {
        ChoiceSheet(title, options, selected, onDismiss = { open = false }) { onPick(it); open = false }
    }
}

@Composable
fun <T> ChoiceSheet(
    title: String,
    options: List<Pair<T, String>>,
    selected: T?,
    onDismiss: () -> Unit,
    onPick: (T) -> Unit,
) {
    CtrlSheet(onDismissRequest = onDismiss) {
        CtrlSectionLabel(title, Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
        LazyColumn(Modifier.navigationBarsPadding(), contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 16.dp)) {
            items(options, key = { it.second }) { (value, label) ->
                val isSel = value == selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .ctrlTopDivider()
                        .heightIn(min = 52.dp)
                        .clickable { onPick(value) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (isSel) CtrlStatusDot(MaterialTheme.colorScheme.primary, Modifier.semantics { contentDescription = str(R.string.selected) })
                }
            }
        }
    }
}

// ---------- Buttons ----------

/** The one main action on a screen (see [CtrlButton]). */
@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    container: Color = CtrlColors.Accent,
    content: Color = CtrlColors.OnAccent,
) {
    CtrlButton(text, onClick, modifier, icon = icon, enabled = enabled, container = container, content = content)
}

/** Square secondary action with a short label underneath. Active ones are filled with the accent. */
@Composable
fun RoundAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    active: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.widthIn(min = 64.dp).clip(CtrlShapes.Control).clickable(role = Role.Button, onClick = onClick).padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CtrlShapes.Button)
                .background(if (active) CtrlColors.Accent else CtrlColors.Surface)
                .border(1.dp, if (active) CtrlColors.Accent else CtrlColors.ControlOutline, CtrlShapes.Button),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, label, Modifier.size(21.dp), tint = if (active) CtrlColors.OnAccent else CtrlColors.Text)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

// ---------- Text helpers ----------

/** Facts about an item laid out with space between them instead of separator characters. */
@Composable
fun MetaRow(parts: List<String>, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.onSurfaceVariant) {
    val visible = parts.filter { it.isNotBlank() }
    if (visible.isEmpty()) return
    FlowRow(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        visible.forEach { Text(it.uppercase(), style = CtrlType.MonoMedium, color = color) }
    }
}

// ---------- Seek bar ----------

/**
 * Slim seek bar with a generous touch area. Tap to jump, drag to scrub.
 * [onScrub] fires while dragging; [onSeek] when the finger lifts.
 */
@Composable
fun SeekBar(
    positionMs: Long,
    durationMs: Long,
    onScrub: (Long) -> Unit,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
    bufferedMs: Long = 0,
    activeColor: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = CtrlColors.ProgressTrack,
    bufferColor: Color = CtrlColors.Muted.copy(alpha = 0.35f),
    height: Dp = 6.dp,
) {
    var dragging by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }
    val dur by rememberUpdatedState(durationMs)
    val fraction = if (dragging) dragFraction else if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
    val buffered = if (durationMs > 0) (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Box(
        modifier
            .fillMaxWidth()
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { o ->
                    if (dur > 0) onSeek((o.x / size.width * dur).toLong().coerceIn(0, dur))
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { o -> dragging = true; dragFraction = (o.x / size.width).coerceIn(0f, 1f) },
                    onDragEnd = { dragging = false; onSeek((dragFraction * dur).toLong()) },
                    onDragCancel = { dragging = false },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        dragFraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        onScrub((dragFraction * dur).toLong())
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxWidth().height(if (dragging) height * 2 else height + 10.dp)) {
            val h = (if (dragging) height * 1.75f else height).toPx()
            val y = (size.height - h) / 2
            val r = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            drawRoundRect(trackColor, Offset(0f, y), Size(size.width, h), r)
            if (buffered > 0f) drawRoundRect(bufferColor, Offset(0f, y), Size(size.width * buffered, h), r)
            drawRoundRect(activeColor, Offset(0f, y), Size(size.width * fraction, h), r)
            // Rounded-square thumb, like the status dots.
            val thumb = if (dragging) 16.dp.toPx() else 12.dp.toPx()
            drawRoundRect(
                activeColor,
                Offset(size.width * fraction - thumb / 2, size.height / 2 - thumb / 2),
                Size(thumb, thumb),
                CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            )
        }
    }
}
