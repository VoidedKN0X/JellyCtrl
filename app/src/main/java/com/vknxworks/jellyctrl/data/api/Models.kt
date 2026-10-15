package com.vknxworks.jellyctrl.data.api

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Jellyfin uses PascalCase JSON. JellyfinJson applies a PascalCase naming strategy, so property names here are camelCase.

const val TICKS_PER_MS = 10_000L
fun Long.ticksToMs(): Long = this / TICKS_PER_MS
fun Long.msToTicks(): Long = this * TICKS_PER_MS

@Serializable
data class PublicSystemInfo(
    val localAddress: String? = null,
    val serverName: String? = null,
    val version: String? = null,
    val productName: String? = null,
    val id: String? = null,
    val startupWizardCompleted: Boolean? = null,
)

@Serializable
data class SystemInfo(
    val serverName: String? = null,
    val version: String? = null,
    val id: String? = null,
    val operatingSystem: String? = null,
    val operatingSystemDisplayName: String? = null,
    val hasPendingRestart: Boolean = false,
    val hasUpdateAvailable: Boolean = false,
    val canSelfRestart: Boolean = true,
    val localAddress: String? = null,
    val programDataPath: String? = null,
    val cachePath: String? = null,
    val logPath: String? = null,
    val transcodingTempPath: String? = null,
    val webPath: String? = null,
    val encoderLocation: String? = null,
    val systemArchitecture: String? = null,
)

@Serializable
data class UserPolicy(
    val isAdministrator: Boolean = false,
    val isDisabled: Boolean = false,
    val isHidden: Boolean = false,
    val enableRemoteControlOfOtherUsers: Boolean = false,
    val enableContentDeletion: Boolean = false,
    val enableContentDownloading: Boolean = true,
    val enableLiveTvManagement: Boolean = false,
    val enableLiveTvAccess: Boolean = true,
)

@Serializable
data class UserDto(
    val name: String? = null,
    val serverId: String? = null,
    val id: String = "",
    val primaryImageTag: String? = null,
    val hasPassword: Boolean = true,
    val hasConfiguredPassword: Boolean = true,
    val lastLoginDate: String? = null,
    val lastActivityDate: String? = null,
    val policy: UserPolicy? = null,
)

@Serializable
data class AuthenticationResult(
    val user: UserDto? = null,
    val accessToken: String? = null,
    val serverId: String? = null,
)

@Serializable
data class QuickConnectResult(
    val authenticated: Boolean = false,
    val secret: String = "",
    val code: String = "",
)

@Serializable
data class UserItemData(
    val playbackPositionTicks: Long = 0,
    val playCount: Int = 0,
    val isFavorite: Boolean = false,
    val played: Boolean = false,
    val unplayedItemCount: Int? = null,
    val playedPercentage: Double? = null,
    val likes: Boolean? = null,
)

@Serializable
data class NameIdPair(val name: String? = null, val id: String? = null)

@Serializable
data class BaseItemPerson(
    val name: String? = null,
    val id: String = "",
    val role: String? = null,
    val type: String? = null,
    val primaryImageTag: String? = null,
)

@Serializable
data class MediaUrl(val url: String? = null, val name: String? = null)

@Serializable
data class ExternalUrl(val name: String? = null, val url: String? = null)

@Serializable
data class ChapterInfo(
    val startPositionTicks: Long = 0,
    val name: String? = null,
    val imageTag: String? = null,
)

@Serializable
data class TrickplayInfo(
    val width: Int = 0,
    val height: Int = 0,
    val tileWidth: Int = 10,
    val tileHeight: Int = 10,
    val thumbnailCount: Int = 0,
    val interval: Int = 10_000,
    val bandwidth: Int = 0,
)

@Serializable
data class MediaStream(
    val codec: String? = null,
    val language: String? = null,
    val title: String? = null,
    val displayTitle: String? = null,
    val type: String? = null, // Video, Audio, Subtitle, EmbeddedImage, Data, Lyric
    val index: Int = 0,
    val isDefault: Boolean = false,
    val isForced: Boolean = false,
    val isExternal: Boolean = false,
    val isTextSubtitleStream: Boolean = false,
    val deliveryMethod: String? = null, // Encode, Embed, External, Hls, Drop
    val deliveryUrl: String? = null,
    val channels: Int? = null,
    val channelLayout: String? = null,
    val bitRate: Long? = null,
    val width: Int? = null,
    val height: Int? = null,
    val averageFrameRate: Float? = null,
    val realFrameRate: Float? = null,
    val profile: String? = null,
    val videoRange: String? = null,
    val videoRangeType: String? = null,
    val bitDepth: Int? = null,
    val sampleRate: Int? = null,
    val path: String? = null,
)

@Serializable
data class MediaSourceInfo(
    val id: String? = null,
    val name: String? = null,
    val path: String? = null,
    val protocol: String? = null, // File, Http, Rtmp, Rtsp, Udp, Rtp, Ftp
    val container: String? = null,
    val size: Long? = null,
    val bitrate: Long? = null,
    val runTimeTicks: Long? = null,
    val eTag: String? = null,
    val isRemote: Boolean = false,
    val supportsDirectPlay: Boolean = false,
    val supportsDirectStream: Boolean = false,
    val supportsTranscoding: Boolean = false,
    val transcodingUrl: String? = null,
    val transcodingSubProtocol: String? = null,
    val transcodingContainer: String? = null,
    val liveStreamId: String? = null,
    val isInfiniteStream: Boolean = false,
    val requiresOpening: Boolean = false,
    val mediaStreams: List<MediaStream> = emptyList(),
    val defaultAudioStreamIndex: Int? = null,
    val defaultSubtitleStreamIndex: Int? = null,
    val videoType: String? = null,
)

@Serializable
data class BaseItemDto(
    val id: String = "",
    val name: String? = null,
    val originalTitle: String? = null,
    val serverId: String? = null,
    val type: String? = null,
    val collectionType: String? = null,
    val mediaType: String? = null,
    val isFolder: Boolean = false,
    val overview: String? = null,
    val taglines: List<String> = emptyList(),
    val productionYear: Int? = null,
    val premiereDate: String? = null,
    val endDate: String? = null,
    val startDate: String? = null,
    val dateCreated: String? = null,
    val officialRating: String? = null,
    val communityRating: Float? = null,
    val criticRating: Float? = null,
    val runTimeTicks: Long? = null,
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val studios: List<NameIdPair> = emptyList(),
    val genreItems: List<NameIdPair> = emptyList(),
    val people: List<BaseItemPerson> = emptyList(),
    val imageTags: Map<String, String> = emptyMap(),
    val backdropImageTags: List<String> = emptyList(),
    val parentBackdropItemId: String? = null,
    val parentBackdropImageTags: List<String> = emptyList(),
    val parentThumbItemId: String? = null,
    val parentThumbImageTag: String? = null,
    val parentLogoItemId: String? = null,
    val parentLogoImageTag: String? = null,
    val parentId: String? = null,
    val seriesId: String? = null,
    val seriesName: String? = null,
    val seriesPrimaryImageTag: String? = null,
    val seasonId: String? = null,
    val seasonName: String? = null,
    val indexNumber: Int? = null,
    val indexNumberEnd: Int? = null,
    val parentIndexNumber: Int? = null,
    val userData: UserItemData? = null,
    val childCount: Int? = null,
    val recursiveItemCount: Int? = null,
    val primaryImageAspectRatio: Double? = null,
    val mediaSources: List<MediaSourceInfo> = emptyList(),
    val mediaStreams: List<MediaStream> = emptyList(),
    val album: String? = null,
    val albumId: String? = null,
    val albumArtist: String? = null,
    val albumPrimaryImageTag: String? = null,
    val artists: List<String> = emptyList(),
    val artistItems: List<NameIdPair> = emptyList(),
    val albumArtists: List<NameIdPair> = emptyList(),
    val chapters: List<ChapterInfo> = emptyList(),
    val remoteTrailers: List<MediaUrl> = emptyList(),
    val localTrailerCount: Int? = null,
    val externalUrls: List<ExternalUrl> = emptyList(),
    val providerIds: Map<String, String?> = emptyMap(),
    val trickplay: Map<String, Map<String, TrickplayInfo>> = emptyMap(),
    val status: String? = null,
    val airTime: String? = null,
    val airDays: List<String> = emptyList(),
    val canDelete: Boolean = false,
    val canDownload: Boolean = false,
    val channelId: String? = null,
    val channelName: String? = null,
    val channelNumber: String? = null,
    val channelPrimaryImageTag: String? = null,
    val currentProgram: BaseItemDto? = null,
    val programId: String? = null,
    val timerId: String? = null,
    val seriesTimerId: String? = null,
    val isSeries: Boolean? = null,
    val isLive: Boolean? = null,
    val isMovie: Boolean? = null,
    val isNews: Boolean? = null,
    val isKids: Boolean? = null,
    val isSports: Boolean? = null,
    val episodeTitle: String? = null,
    val container: String? = null,
    val path: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val locationType: String? = null,
    val playlistItemId: String? = null,
    val normalizationGain: Float? = null,
) {
    val isPlayable: Boolean
        get() = mediaType == "Video" || mediaType == "Audio" || type in PLAYABLE_TYPES

    companion object {
        val PLAYABLE_TYPES = setOf("Movie", "Episode", "Video", "MusicVideo", "Audio", "TvChannel", "Recording", "Trailer")
    }
}

@Serializable
data class ItemsResult(
    val items: List<BaseItemDto> = emptyList(),
    val totalRecordCount: Int = 0,
    val startIndex: Int = 0,
)

@Serializable
data class PlaybackInfoResponse(
    val mediaSources: List<MediaSourceInfo> = emptyList(),
    val playSessionId: String? = null,
    val errorCode: String? = null,
)

@Serializable
data class MediaSegment(
    val id: String? = null,
    val itemId: String? = null,
    val type: String = "Unknown", // Unknown, Commercial, Preview, Recap, Outro, Intro
    val startTicks: Long = 0,
    val endTicks: Long = 0,
)

@Serializable
data class MediaSegmentsResult(val items: List<MediaSegment> = emptyList())

@Serializable
data class LyricLine(val text: String = "", val start: Long? = null)

@Serializable
data class LyricDto(val lyrics: List<LyricLine> = emptyList())

@Serializable
data class PlayerStateInfo(
    val positionTicks: Long? = null,
    val canSeek: Boolean = false,
    val isPaused: Boolean = false,
    val isMuted: Boolean = false,
    val volumeLevel: Int? = null,
    val playMethod: String? = null,
)

@Serializable
data class SessionInfo(
    val id: String = "",
    val userId: String? = null,
    val userName: String? = null,
    val client: String? = null,
    val deviceName: String? = null,
    val deviceId: String? = null,
    val applicationVersion: String? = null,
    val remoteEndPoint: String? = null,
    val lastActivityDate: String? = null,
    val isActive: Boolean = false,
    val supportsRemoteControl: Boolean = false,
    val supportsMediaControl: Boolean = false,
    val nowPlayingItem: BaseItemDto? = null,
    val playState: PlayerStateInfo? = null,
    val transcodingInfo: TranscodingInfo? = null,
)

@Serializable
data class TranscodingInfo(
    val audioCodec: String? = null,
    val videoCodec: String? = null,
    val container: String? = null,
    val isVideoDirect: Boolean = false,
    val isAudioDirect: Boolean = false,
    val bitrate: Long? = null,
    val framerate: Float? = null,
    val completionPercentage: Double? = null,
    val width: Int? = null,
    val height: Int? = null,
    val transcodeReasons: List<String> = emptyList(),
)

// ---------- Admin ----------

@Serializable
data class ActivityLogEntry(
    val id: Long = 0,
    val name: String? = null,
    val overview: String? = null,
    val shortOverview: String? = null,
    val type: String? = null,
    val date: String? = null,
    val userId: String? = null,
    val severity: String? = null,
)

@Serializable
data class ActivityLogResult(val items: List<ActivityLogEntry> = emptyList(), val totalRecordCount: Int = 0)

@Serializable
data class TaskResult(
    val startTimeUtc: String? = null,
    val endTimeUtc: String? = null,
    val status: String? = null,
    val errorMessage: String? = null,
)

@Serializable
data class TaskInfo(
    val name: String? = null,
    val state: String? = null, // Idle, Cancelling, Running
    val currentProgressPercentage: Double? = null,
    val id: String = "",
    val lastExecutionResult: TaskResult? = null,
    val description: String? = null,
    val category: String? = null,
    val isHidden: Boolean = false,
    val key: String? = null,
)

@Serializable
data class PluginInfo(
    val name: String? = null,
    val version: String = "",
    val description: String? = null,
    val id: String = "",
    val canUninstall: Boolean = true,
    val hasImage: Boolean = false,
    val status: String? = null, // Active, Restart, Deleted, Superceded, Malfunctioned, NotSupported, Disabled
)

// /Packages uses camelCase, decoded with the plain Json instance.
@Serializable
data class PackageVersion(
    val version: String = "",
    val changelog: String? = null,
    val targetAbi: String? = null,
    val timestamp: String? = null,
    val repositoryName: String? = null,
)

@Serializable
data class PackageInfo(
    val name: String = "",
    val description: String? = null,
    val overview: String? = null,
    val owner: String? = null,
    val category: String? = null,
    val guid: String = "",
    val versions: List<PackageVersion> = emptyList(),
    val imageUrl: String? = null,
)

@Serializable
data class LogFile(
    val dateCreated: String? = null,
    val dateModified: String? = null,
    val size: Long = 0,
    val name: String = "",
)

@Serializable
data class VirtualFolderInfo(
    val name: String? = null,
    val locations: List<String> = emptyList(),
    val collectionType: String? = null,
    val itemId: String? = null,
    val primaryImageItemId: String? = null,
    val refreshProgress: Double? = null,
    val refreshStatus: String? = null,
)

@Serializable
data class FileSystemEntry(
    val name: String? = null,
    val path: String = "",
    val type: String? = null, // File, Directory, NetworkComputer, NetworkShare
)

@Serializable
data class DeviceInfo(
    val id: String? = null,
    val name: String? = null,
    val customName: String? = null,
    val appName: String? = null,
    val appVersion: String? = null,
    val lastUserName: String? = null,
    val lastUserId: String? = null,
    val dateLastActivity: String? = null,
)

@Serializable
data class DeviceInfoResult(val items: List<DeviceInfo> = emptyList())

@Serializable
data class AuthenticationInfo(
    val id: Long = 0,
    val accessToken: String = "",
    val deviceId: String? = null,
    val appName: String? = null,
    val deviceName: String? = null,
    val dateCreated: String? = null,
    val isActive: Boolean = true,
)

@Serializable
data class AuthenticationInfoResult(val items: List<AuthenticationInfo> = emptyList())

// ---------- Live TV ----------

@Serializable
data class TimerInfo(
    val id: String? = null,
    val channelId: String? = null,
    val channelName: String? = null,
    val programId: String? = null,
    val name: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val status: String? = null,
    val seriesTimerId: String? = null,
)

@Serializable
data class TimerInfoResult(val items: List<TimerInfo> = emptyList())

@Serializable
data class LiveTvInfo(val isEnabled: Boolean = false, val enabledUsers: List<String> = emptyList())

@Serializable
data class GenreResult(val items: List<BaseItemDto> = emptyList())

/** Raw JSON helper so admin screens can edit objects without dropping fields this app doesn't model. */
typealias RawJson = JsonObject
