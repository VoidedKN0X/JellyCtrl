package com.vknxworks.jellyctrl.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.data.api.ApiException
import com.vknxworks.jellyctrl.data.api.CultureDto
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.MediaStream
import com.vknxworks.jellyctrl.data.api.RemoteSubtitleInfo
import com.vknxworks.jellyctrl.data.api.cultures
import com.vknxworks.jellyctrl.data.api.downloadSubtitleAndFind
import com.vknxworks.jellyctrl.data.api.preferredSubtitleLanguage
import com.vknxworks.jellyctrl.data.api.searchRemoteSubtitles
import com.vknxworks.jellyctrl.data.api.subtitleProviderCount
import com.vknxworks.jellyctrl.str
import com.vknxworks.jellyctrl.ui.theme.CtrlColors
import com.vknxworks.jellyctrl.ui.theme.CtrlShapes
import com.vknxworks.jellyctrl.ui.theme.CtrlType
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/** What the search sheet is showing below the language row. */
private sealed interface SubSearchState {
    data object Loading : SubSearchState
    data class Results(val items: List<RemoteSubtitleInfo>) : SubSearchState
    data object NoProvider : SubSearchState
    data class Failed(val message: String) : SubSearchState
}

/** A three-letter language code and its name, for the language picker. */
private data class SubLanguage(val code: String, val name: String)

private fun CultureDto.codes(): List<String> =
    (listOfNotNull(threeLetterISOLanguageName) + threeLetterISOLanguageNames).map { it.lowercase() }.distinct()

/** Your subtitle language from Jellyfin, otherwise the phone's language, otherwise English. */
private fun startLanguage(preferred: String, cultures: List<CultureDto>): String {
    val pref = preferred.trim().lowercase()
    val phone = runCatching { java.util.Locale.getDefault().isO3Language.lowercase() }.getOrNull().orEmpty()
    for (wanted in listOf(pref, phone)) {
        if (wanted.isBlank()) continue
        // Jellyfin's cultures use the B codes (dut, ger, fre) and also list the T codes (nld, deu, fra) the phone uses.
        cultures.firstOrNull { wanted in it.codes() }?.threeLetterISOLanguageName?.let { return it.lowercase() }
        if (wanted == pref && wanted.length == 3) return wanted
    }
    return "eng"
}

/**
 * Searches the server's subtitle providers (OpenSubtitles and other plugins) for [itemId] and downloads one to the
 * server. [onDownloaded] gets the new subtitle stream of media source [sourceId] (null when the server hasn't listed
 * it yet). Used by the video player and the title page.
 */
@Composable
fun SubtitleSearchSheet(
    client: JellyfinClient,
    itemId: String,
    sourceId: String?,
    title: String?,
    onDismiss: () -> Unit,
    onDownloaded: (MediaStream?) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var languages by remember { mutableStateOf<List<SubLanguage>>(emptyList()) }
    var language by remember { mutableStateOf<String?>(null) }
    var state by remember { mutableStateOf<SubSearchState>(SubSearchState.Loading) }
    var downloadingId by remember { mutableStateOf<String?>(null) }
    var picking by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        val cultures = runCatching { client.cultures() }.getOrDefault(emptyList())
        languages = cultures
            .mapNotNull { c -> c.threeLetterISOLanguageName?.lowercase()?.let { SubLanguage(it, c.displayName ?: c.name ?: it) } }
            .distinctBy { it.code }
            .distinctBy { it.name }
            .sortedBy { it.name }
        language = startLanguage(client.preferredSubtitleLanguage(), cultures)
    }

    LaunchedEffect(itemId, language) {
        val lang = language ?: return@LaunchedEffect
        state = SubSearchState.Loading
        state = try {
            val found = client.searchRemoteSubtitles(itemId, lang)
            if (found.isEmpty() && client.subtitleProviderCount() == 0) SubSearchState.NoProvider
            else SubSearchState.Results(
                // Several providers can return the same subtitle; the list needs every id once.
                found.filter { !it.id.isNullOrBlank() }.distinctBy { it.id }
                    .sortedWith(compareByDescending<RemoteSubtitleInfo> { it.isHashMatch == true }.thenByDescending { it.downloadCount ?: 0 }),
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            when {
                e is ApiException && e.code == 403 -> SubSearchState.Failed(str(R.string.subs_not_allowed))
                client.subtitleProviderCount() == 0 -> SubSearchState.NoProvider
                else -> SubSearchState.Failed(e.friendlyMessage())
            }
        }
    }

    fun download(sub: RemoteSubtitleInfo) {
        val id = sub.id ?: return
        if (downloadingId != null) return
        downloadingId = id
        scope.launch {
            try {
                val stream = client.downloadSubtitleAndFind(itemId, sourceId, id)
                downloadingId = null
                onDownloaded(stream)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                downloadingId = null
                state = SubSearchState.Failed(
                    if (e is ApiException && e.code == 403) str(R.string.subs_not_allowed) else str(R.string.subs_download_failed, e.friendlyMessage()),
                )
            }
        }
    }

    CtrlSheet(onDismissRequest = onDismiss) {
        LazyColumn(Modifier.navigationBarsPadding(), contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 8.dp)) {
                    CtrlSectionLabel(stringResource(R.string.subs_search_title))
                    if (!title.isNullOrBlank()) {
                        Text(title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            item {
                val name = languages.firstOrNull { it.code == language }?.name ?: language?.uppercase().orEmpty()
                GroupRow(
                    stringResource(R.string.subs_language),
                    name,
                    icon = Icons.Outlined.Translate,
                    showChevron = true,
                    onClick = if (languages.isNotEmpty() && downloadingId == null) ({ picking = true }) else null,
                )
            }
            when (val st = state) {
                SubSearchState.Loading -> item {
                    Box(Modifier.fillMaxWidth().heightIn(min = 140.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CtrlColors.AccentText, trackColor = CtrlColors.ProgressTrack, strokeWidth = 3.dp)
                    }
                }
                SubSearchState.NoProvider -> item {
                    SheetMessage(stringResource(R.string.subs_no_provider_title), stringResource(R.string.subs_no_provider_body))
                }
                is SubSearchState.Failed -> item {
                    SheetMessage(stringResource(R.string.subs_failed_title), st.message)
                }
                is SubSearchState.Results -> {
                    if (st.items.isEmpty()) {
                        item { SheetMessage(stringResource(R.string.subs_none_title), stringResource(R.string.subs_none_body)) }
                    } else {
                        items(st.items, key = { it.id.orEmpty() }) { sub ->
                            ResultRow(sub, downloading = downloadingId == sub.id, enabled = downloadingId == null) { download(sub) }
                        }
                    }
                }
            }
        }
    }

    if (picking) {
        ChoiceSheet(stringResource(R.string.subs_language), languages.map { it.code to it.name }, language, onDismiss = { picking = false }) {
            language = it
            picking = false
        }
    }
}

@Composable
private fun SheetMessage(title: String, body: String) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Tag(text: String, accent: Boolean = false) {
    Text(
        text.uppercase(),
        style = CtrlType.Label,
        color = if (accent) CtrlColors.AccentText else CtrlColors.Muted,
        modifier = Modifier
            .border(1.dp, if (accent) CtrlColors.AccentText else CtrlColors.ControlOutline, CtrlShapes.Chip)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun ResultRow(sub: RemoteSubtitleInfo, downloading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .ctrlTopDivider()
            .heightIn(min = 60.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                sub.name ?: sub.comment ?: stringResource(R.string.unknown),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val details = listOfNotNull(
                sub.providerName,
                sub.format?.uppercase(),
                sub.communityRating?.takeIf { it > 0f }?.let { stringResource(R.string.subs_rating_x, "%.1f".format(it)) },
                sub.downloadCount?.takeIf { it > 0 }?.let { stringResource(R.string.subs_downloads_x, it) },
            ).joinToString(" · ")
            if (details.isNotBlank()) {
                Text(details, style = CtrlType.Mono, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val tags = listOfNotNull(
                if (sub.isHashMatch == true) stringResource(R.string.subs_tag_exact) to true else null,
                if (sub.hearingImpaired == true) stringResource(R.string.subs_tag_hi) to false else null,
                if (sub.forced == true) stringResource(R.string.subs_tag_forced) to false else null,
                if (sub.machineTranslated == true || sub.aiTranslated == true) stringResource(R.string.subs_tag_machine) to false else null,
            )
            if (tags.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    tags.forEach { (t, accent) -> Tag(t, accent) }
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            if (downloading) {
                CircularProgressIndicator(Modifier.size(22.dp), color = CtrlColors.AccentText, trackColor = CtrlColors.ProgressTrack, strokeWidth = 2.dp)
            } else {
                Icon(
                    Icons.Outlined.Download,
                    stringResource(R.string.subs_download),
                    tint = if (enabled) CtrlColors.AccentText else CtrlColors.Inactive,
                    modifier = Modifier
                        .size(36.dp)
                        .background(CtrlColors.Raised, CtrlShapes.Control)
                        .padding(8.dp),
                )
            }
        }
    }
}
