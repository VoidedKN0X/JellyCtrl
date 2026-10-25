package com.vknxworks.jellyctrl.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.container
import com.vknxworks.jellyctrl.ui.nav.LocalNavigator
import com.vknxworks.jellyctrl.ui.nav.ProfileRoute
import com.vknxworks.jellyctrl.ui.nav.TabItem
import com.vknxworks.jellyctrl.ui.theme.CtrlColors
import com.vknxworks.jellyctrl.ui.theme.CtrlShapes
import com.vknxworks.jellyctrl.ui.theme.CtrlType
import com.vknxworks.jellyctrl.ui.theme.Gutter

// Shared components of the Ctrl / Signal look. Every screen builds on these.

// ---------- Small pieces ----------

/** "JELLY/CTRL": JetBrains Mono, the slash in the accent colour. */
@Composable
fun CtrlWordmark(modifier: Modifier = Modifier, name: String = "JELLY") {
    Text(
        buildAnnotatedString {
            append(name)
            withStyle(SpanStyle(color = CtrlColors.AccentText)) { append("/") }
            append("CTRL")
        },
        style = CtrlType.Wordmark,
        color = CtrlColors.Text,
        modifier = modifier,
    )
}

/** Mono section label, upper case and spaced out. */
@Composable
fun CtrlSectionLabel(text: String, modifier: Modifier = Modifier, color: Color = CtrlColors.Muted, strong: Boolean = false) {
    Text(
        text.uppercase(),
        style = if (strong) CtrlType.LabelStrong else CtrlType.Label,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/** 8dp square status dot. */
@Composable
fun CtrlStatusDot(color: Color, modifier: Modifier = Modifier, size: Dp = 8.dp) {
    Box(modifier.size(size).clip(CtrlShapes.Dot).background(color))
}

/** A 1dp list divider along the top edge. */
@Composable
fun Modifier.ctrlTopDivider(color: Color = CtrlColors.Divider): Modifier = drawBehind {
    drawRect(color, size = Size(size.width, 1.dp.toPx()))
}

/** Surface fill with the 1dp card border. */
@Composable
fun Modifier.ctrlSurface(shape: Shape = CtrlShapes.Card, color: Color = CtrlColors.Surface, border: Color = CtrlColors.Outline): Modifier =
    clip(shape).background(color).border(1.dp, border, shape)

// ---------- Square buttons and the profile avatar ----------

/** 44dp rounded-square icon button: surface fill and a hairline border. */
@Composable
fun CtrlIconSquare(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconSize: Dp = 20.dp,
    shape: Shape = CtrlShapes.Control,
    container: Color = CtrlColors.Surface,
    content: Color = CtrlColors.Text,
    border: Color? = CtrlColors.ControlOutline,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .size(size)
            .clip(shape)
            .background(container)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .then(if (enabled) Modifier else Modifier.alpha(0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(iconSize), tint = content)
    }
}

/**
 * The signed-in user's picture (or initials) in a 44dp rounded square; tapping it opens the You page,
 * where the settings live. A small accent square shows while signed in to a server.
 */
@Composable
fun CtrlAvatarButton(modifier: Modifier = Modifier) {
    val nav = LocalNavigator.current
    val session by LocalContext.current.container.sessions.active.collectAsStateWithLifecycle()
    val acc = session?.account
    val client = session?.client
    val you = stringResource(R.string.you)
    Box(modifier.size(44.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CtrlShapes.Control)
                .background(CtrlColors.Surface)
                .border(1.dp, CtrlColors.ControlOutline, CtrlShapes.Control)
                .clickable(role = Role.Button, onClickLabel = you) { nav.navigate(ProfileRoute) }
                .semantics { contentDescription = you },
            contentAlignment = Alignment.Center,
        ) {
            when {
                acc?.userImageTag != null && client != null ->
                    ArtImage(client.userImageUrl(acc.userId, acc.userImageTag), Modifier.fillMaxSize())
                acc != null && acc.userName.isNotEmpty() ->
                    Text(initials(acc.userName), style = MaterialTheme.typography.labelLarge, color = CtrlColors.Text)
                else -> Icon(Icons.Outlined.AccountCircle, null, Modifier.size(22.dp), tint = CtrlColors.Muted)
            }
        }
        if (session != null) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 3.dp, y = 3.dp)
                    .size(10.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(CtrlColors.Background)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(CtrlColors.Accent),
            )
        }
    }
}

/** Up to two initials, e.g. "axel" -> "AX", "Axel Smith" -> "AS". */
fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    return when {
        parts.size >= 2 -> (parts[0].take(1) + parts[1].take(1)).uppercase()
        else -> name.trim().take(2).uppercase()
    }
}

// ---------- Top bars ----------

/** Main-tab top bar: wordmark on the left, optional actions, then the profile avatar. No colour fill. */
@Composable
fun CtrlTopBar(
    modifier: Modifier = Modifier,
    padded: Boolean = true,
    showProfile: Boolean = true,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = if (padded) Gutter else 0.dp)
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CtrlWordmark()
        Spacer(Modifier.weight(1f))
        trailing()
        if (showProfile) CtrlAvatarButton()
    }
}

/** Top bar for pushed screens: square back button and the title in Space Grotesk. Handles the status bar inset. */
@Composable
fun CtrlSubTopBar(
    title: String,
    showBack: Boolean = true,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val nav = LocalNavigator.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(CtrlColors.Background)
            .statusBarsPadding()
            .heightIn(min = 68.dp)
            .padding(start = Gutter, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            CtrlIconSquare(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.back), onClick = nav::back)
            Spacer(Modifier.width(14.dp))
        }
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        CompositionLocalProvider(LocalContentColor provides CtrlColors.Muted) { actions() }
    }
}

// ---------- Cards ----------

/** Secondary card: surface fill, 1dp border, 22dp corners. */
@Composable
fun CtrlCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(10.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .ctrlSurface()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        verticalArrangement = verticalArrangement,
        content = content,
    )
}

/** The one most important thing on a screen: accent fill with near-black content and 26dp corners. */
@Composable
fun CtrlHeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    CompositionLocalProvider(LocalContentColor provides CtrlColors.OnAccent) {
        Column(
            modifier
                .clip(CtrlShapes.Hero)
                .background(CtrlColors.Accent)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

// ---------- Buttons ----------

/** Primary button: accent fill, near-black text, 56dp tall for main actions. */
@Composable
fun CtrlButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    container: Color = CtrlColors.Accent,
    content: Color = CtrlColors.OnAccent,
    minHeight: Dp = 56.dp,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = minHeight),
        shape = CtrlShapes.MainButton,
        colors = ButtonDefaults.buttonColors(
            containerColor = container,
            contentColor = content,
            disabledContainerColor = CtrlColors.Raised,
            disabledContentColor = CtrlColors.Inactive,
        ),
        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Secondary button: surface fill and a 1dp border. */
@Composable
fun CtrlSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    minHeight: Dp = 56.dp,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = minHeight),
        shape = CtrlShapes.MainButton,
        border = BorderStroke(1.dp, CtrlColors.ControlOutline),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = CtrlColors.Surface,
            contentColor = CtrlColors.Text,
            disabledContainerColor = CtrlColors.Surface,
            disabledContentColor = CtrlColors.Inactive,
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, style = MaterialTheme.typography.titleSmall.copy(fontSize = 16.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

// ---------- Switch ----------

/**
 * Rounded-rectangle switch, 52x32dp: accent track and near-black knob when on, grey when off.
 * Announced as a switch to TalkBack. With [onCheckedChange] null it only shows the state (the row handles taps).
 */
@Composable
fun CtrlSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val track by animateColorAsState(if (checked) CtrlColors.Accent else CtrlColors.SwitchTrackOff, label = "switch-track")
    val knob by animateColorAsState(if (checked) CtrlColors.OnAccent else CtrlColors.SwitchKnobOff, label = "switch-knob")
    val x by animateDpAsState(if (checked) 20.dp else 0.dp, label = "switch-x")
    val toggle = if (onCheckedChange != null) {
        Modifier.toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
    } else {
        // The surrounding row is the toggleable switch; this is only its picture.
        Modifier
    }
    Box(
        modifier
            .then(if (onCheckedChange != null) Modifier.minimumInteractiveComponentSize() else Modifier)
            .then(toggle)
            .then(if (enabled) Modifier else Modifier.alpha(0.4f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(width = 52.dp, height = 32.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(track)
                .padding(4.dp),
        ) {
            Box(Modifier.offset(x = x).size(24.dp).clip(RoundedCornerShape(7.dp)).background(knob))
        }
    }
}

// ---------- Progress ----------

/** 6dp progress bar with 3dp corners. Light fill by default; pass the accent for the main figure. */
@Composable
fun CtrlProgressBar(
    progress: () -> Float,
    modifier: Modifier = Modifier,
    color: Color = CtrlColors.ProgressFill,
    trackColor: Color = CtrlColors.ProgressTrack,
    @Suppress("UNUSED_PARAMETER") drawStopIndicator: (DrawScope.() -> Unit)? = null,
) {
    val p = progress().coerceIn(0f, 1f)
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(trackColor)
            .semantics { progressBarRangeInfo = ProgressBarRangeInfo(p, 0f..1f) },
    ) {
        Box(Modifier.fillMaxWidth(p).fillMaxHeight().background(color))
    }
}

// ---------- Text fields ----------

@Composable
fun ctrlTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = CtrlColors.Surface,
    unfocusedContainerColor = CtrlColors.Surface,
    disabledContainerColor = CtrlColors.Surface,
    errorContainerColor = CtrlColors.Surface,
    focusedBorderColor = CtrlColors.AccentText,
    unfocusedBorderColor = CtrlColors.ControlOutline,
    disabledBorderColor = CtrlColors.Outline,
    cursorColor = CtrlColors.AccentText,
    focusedLabelColor = CtrlColors.AccentText,
    unfocusedLabelColor = CtrlColors.Muted,
    focusedTextColor = CtrlColors.Text,
    unfocusedTextColor = CtrlColors.Text,
    focusedPlaceholderColor = CtrlColors.Muted,
    unfocusedPlaceholderColor = CtrlColors.Muted,
    focusedLeadingIconColor = CtrlColors.Muted,
    unfocusedLeadingIconColor = CtrlColors.Muted,
    focusedTrailingIconColor = CtrlColors.Muted,
    unfocusedTrailingIconColor = CtrlColors.Muted,
)

/** Text field: surface fill, 1dp border, 16dp corners; accent border and cursor when focused. */
@Composable
fun CtrlTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = CtrlShapes.Field,
    colors: TextFieldColors = ctrlTextFieldColors(),
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        shape = shape,
        colors = colors,
    )
}

// ---------- Dialogs, sheets and list rows ----------

/** Alert dialog in the Signal style: surface fill, 26dp corners, 1dp border. Same slots as Material's AlertDialog. */
@Composable
fun CtrlAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) {
    BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier, properties = properties) {
        Surface(
            shape = CtrlShapes.Dialog,
            color = CtrlColors.Surface,
            contentColor = CtrlColors.Text,
            border = BorderStroke(1.dp, CtrlColors.Outline),
        ) {
            Column(Modifier.padding(24.dp)) {
                if (icon != null) {
                    Box(Modifier.align(Alignment.CenterHorizontally).padding(bottom = 16.dp)) {
                        CompositionLocalProvider(LocalContentColor provides CtrlColors.AccentText) { icon() }
                    }
                }
                if (title != null) {
                    Box(Modifier.align(if (icon == null) Alignment.Start else Alignment.CenterHorizontally).padding(bottom = 14.dp)) {
                        ProvideTextStyle(MaterialTheme.typography.headlineSmall) { title() }
                    }
                }
                if (text != null) {
                    Box(Modifier.weight(1f, fill = false).align(Alignment.Start).padding(bottom = 22.dp)) {
                        CompositionLocalProvider(LocalContentColor provides CtrlColors.Muted) {
                            ProvideTextStyle(MaterialTheme.typography.bodyMedium) { text() }
                        }
                    }
                }
                Row(Modifier.align(Alignment.End), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    dismissButton?.invoke()
                    confirmButton()
                }
            }
        }
    }
}

/** Bottom sheet in the Signal style: surface fill, 26dp top corners, 1dp border. */
@Composable
fun CtrlSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.border(1.dp, CtrlColors.Outline, CtrlShapes.SheetTop),
        shape = CtrlShapes.SheetTop,
        containerColor = CtrlColors.Surface,
        contentColor = CtrlColors.Text,
        dragHandle = { BottomSheetDefaults.DragHandle(color = CtrlColors.ControlOutline) },
        content = content,
    )
}

/** Plain list row with a 1dp top divider, no fill. Same slots as Material's ListItem. */
@Composable
fun CtrlListItem(
    headlineContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    overlineContent: @Composable (() -> Unit)? = null,
    supportingContent: @Composable (() -> Unit)? = null,
    leadingContent: @Composable (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = headlineContent,
        modifier = modifier.ctrlTopDivider(),
        overlineContent = overlineContent,
        supportingContent = supportingContent,
        leadingContent = leadingContent,
        trailingContent = trailingContent,
        colors = ListItemDefaults.colors(
            containerColor = Color.Transparent,
            headlineColor = CtrlColors.Text,
            supportingColor = CtrlColors.Muted,
            overlineColor = CtrlColors.Muted,
            leadingIconColor = CtrlColors.Muted,
            trailingIconColor = CtrlColors.Muted,
        ),
    )
}

/** Hides the first row's top divider inside a card, so dividers only sit between rows. */
fun Modifier.hideFirstDivider(): Modifier = layout { measurable, constraints ->
    val shift = 1.dp.roundToPx()
    val p = measurable.measure(constraints)
    layout(p.width, (p.height - shift).coerceAtLeast(0)) { p.place(0, -shift) }
}

// ---------- Bottom navigation ----------

/**
 * Floating bottom bar: 64dp tall, 22dp corners, surface at 92% with a 1dp border.
 * The selected tab is an accent rounded rectangle with icon and label; the others show only their icon.
 */
@Composable
fun CtrlBottomBar(
    items: List<TabItem>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .padding(horizontal = 20.dp)
            .height(64.dp)
            .clip(CtrlShapes.Card)
            .background(CtrlColors.Surface.copy(alpha = 0.92f))
            .border(1.dp, CtrlColors.ControlOutline, CtrlShapes.Card)
            .padding(horizontal = 8.dp)
            .selectableGroup(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEachIndexed { i, item ->
            val selected = i == selectedIndex
            val shape = RoundedCornerShape(16.dp)
            Row(
                Modifier
                    .height(48.dp)
                    .widthIn(min = 48.dp)
                    .clip(shape)
                    .background(if (selected) CtrlColors.Accent else Color.Transparent)
                    .selectable(
                        selected = selected,
                        onClick = {
                            if (!selected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelect(i)
                        },
                        role = Role.Tab,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                    )
                    .padding(horizontal = if (selected) 18.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    if (selected) item.selectedIcon else item.icon,
                    contentDescription = if (selected) null else item.label,
                    tint = if (selected) CtrlColors.OnAccent else CtrlColors.Muted,
                    modifier = Modifier.size(20.dp),
                )
                if (selected) {
                    Spacer(Modifier.width(8.dp))
                    Text(item.label, style = MaterialTheme.typography.labelLarge, color = CtrlColors.OnAccent, maxLines = 1)
                }
            }
        }
    }
}
