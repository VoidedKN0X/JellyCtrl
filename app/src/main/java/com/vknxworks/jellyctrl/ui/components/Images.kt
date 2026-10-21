package com.vknxworks.jellyctrl.ui.components

import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.R
import com.vknxworks.jellyctrl.str

/** Image URL helpers that fall back to parent (series/album) artwork the way the Jellyfin web client does. */
fun JellyfinClient.primaryImage(item: BaseItemDto, width: Int = 400): String? {
    item.imageTags["Primary"]?.let { return imageUrl(item.id, "Primary", it, width) }
    if (item.type == "Episode" || item.type == "Season") {
        item.seriesPrimaryImageTag?.let { tag -> item.seriesId?.let { return imageUrl(it, "Primary", tag, width) } }
    }
    item.albumPrimaryImageTag?.let { tag -> item.albumId?.let { return imageUrl(it, "Primary", tag, width) } }
    item.channelPrimaryImageTag?.let { tag -> item.channelId?.let { return imageUrl(it, "Primary", tag, width) } }
    return null
}

fun JellyfinClient.backdropImage(item: BaseItemDto, width: Int = 1280): String? {
    item.backdropImageTags.firstOrNull()?.let { return imageUrl(item.id, "Backdrop", it, width, index = 0) }
    val parent = item.parentBackdropItemId
    item.parentBackdropImageTags.firstOrNull()?.let { tag -> if (parent != null) return imageUrl(parent, "Backdrop", tag, width, index = 0) }
    return null
}

/** Landscape artwork for "continue watching" style cards. */
fun JellyfinClient.landscapeImage(item: BaseItemDto, width: Int = 640): String? {
    if (item.type == "Episode") {
        item.imageTags["Primary"]?.let { return imageUrl(item.id, "Primary", it, width) }
    }
    item.imageTags["Thumb"]?.let { return imageUrl(item.id, "Thumb", it, width) }
    item.parentThumbImageTag?.let { tag -> item.parentThumbItemId?.let { return imageUrl(it, "Thumb", tag, width) } }
    return backdropImage(item, width) ?: primaryImage(item, width)
}

fun JellyfinClient.logoImage(item: BaseItemDto, width: Int = 600): String? {
    item.imageTags["Logo"]?.let { return imageUrl(item.id, "Logo", it, width) }
    item.parentLogoImageTag?.let { tag -> item.parentLogoItemId?.let { return imageUrl(it, "Logo", tag, width) } }
    return null
}

fun BaseItemDto.posterAspect(): Float = when (type) {
    "Episode", "Video", "TvChannel", "Program", "Recording", "Photo", "PhotoAlbum", "CollectionFolder", "UserView" -> 16f / 9f
    "MusicAlbum", "Audio", "MusicArtist", "Playlist", "MusicGenre", "AudioBook" -> 1f
    else -> primaryImageAspectRatio?.toFloat()?.takeIf { it in 0.5f..2.0f } ?: (2f / 3f)
}

fun formatRuntime(ticks: Long?): String? {
    if (ticks == null || ticks <= 0) return null
    val totalMin = ticks / 600_000_000L
    val h = totalMin / 60
    val m = totalMin % 60
    return if (h > 0) str(R.string.short_h_m, h, m) else str(R.string.short_m, m)
}

fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

fun formatBytes(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var v = bytes / 1024.0
    var i = 0
    while (v >= 1024 && i < units.lastIndex) { v /= 1024; i++ }
    return "%.1f %s".format(v, units[i])
}

fun BaseItemDto.subtitleLine(): String? = when (type) {
    "Episode" -> buildString {
        if (parentIndexNumber != null) append("S$parentIndexNumber")
        if (indexNumber != null) append(if (isEmpty()) "E$indexNumber" else ":E$indexNumber")
        if (isNotEmpty() && name != null) append(" - ")
        append(name ?: "")
    }.ifBlank { null }
    "MusicAlbum" -> albumArtist ?: artists.firstOrNull()
    "Audio" -> artists.joinToString().ifBlank { albumArtist }
    "Season" -> seriesName
    "Series" -> productionYear?.let { y -> if (status == "Continuing") str(R.string.x_present, y) else endDate?.take(4)?.let { if (it != y.toString()) "$y - $it" else "$y" } ?: "$y" }
    "TvChannel" -> currentProgram?.name
    else -> productionYear?.toString()
}

/** The headline shown on a card: episodes show their series name. */
fun BaseItemDto.cardTitle(): String = when (type) {
    "Episode" -> seriesName ?: name ?: ""
    else -> name ?: ""
}

fun BaseItemDto.resumeProgress(): Float? {
    val pos = userData?.playbackPositionTicks ?: return null
    val total = runTimeTicks ?: return null
    if (pos <= 0 || total <= 0) return null
    return (pos.toFloat() / total).coerceIn(0f, 1f)
}

fun isoToEpochMs(iso: String?): Long? = iso?.let { runCatching { java.time.Instant.parse(normalizeIso(it)).toEpochMilli() }.getOrNull() }

private fun normalizeIso(s: String): String {
    // Jellyfin sends 7 fractional digits and sometimes no zone; Instant.parse wants a zone.
    var t = s
    if (!t.endsWith("Z") && !Regex("[+-]\\d\\d:\\d\\d$").containsMatchIn(t)) t += "Z"
    return t
}

fun formatDate(iso: String?): String? = isoToEpochMs(iso)?.let {
    java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy").withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(it))
}

fun formatDateTime(iso: String?): String? = isoToEpochMs(iso)?.let {
    java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm").withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(it))
}

fun formatTime(iso: String?): String? = isoToEpochMs(iso)?.let {
    java.time.format.DateTimeFormatter.ofPattern("HH:mm").withZone(java.time.ZoneId.systemDefault()).format(java.time.Instant.ofEpochMilli(it))
}
