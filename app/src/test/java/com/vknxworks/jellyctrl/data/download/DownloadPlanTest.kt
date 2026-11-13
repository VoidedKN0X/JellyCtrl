package com.vknxworks.jellyctrl.data.download

import com.vknxworks.jellyctrl.data.api.BaseItemDto
import com.vknxworks.jellyctrl.data.api.JellyfinClient
import com.vknxworks.jellyctrl.data.api.MediaSourceInfo
import com.vknxworks.jellyctrl.data.api.MediaStream
import com.vknxworks.jellyctrl.data.api.UserItemData
import com.vknxworks.jellyctrl.data.settings.PlayerEngineChoice
import com.vknxworks.jellyctrl.player.EngineKind
import com.vknxworks.jellyctrl.player.PlaybackResolver
import com.vknxworks.jellyctrl.player.SubSelection
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadPlanTest {
    private val client = JellyfinClient("https://jf.example.test/jellyfin", "secret-token", "user1", "dev1", "Phone", OkHttpClient())

    private val subs = listOf(
        MediaStream(type = "Subtitle", index = 2, codec = "subrip", language = "eng"),
        MediaStream(type = "Subtitle", index = 3, codec = "PGSSUB", language = "fre"),
        MediaStream(type = "Subtitle", index = 4, codec = "ass", language = "jpn", isExternal = true),
        MediaStream(type = "Subtitle", index = 5, codec = "webvtt", language = "ger", isExternal = true),
        MediaStream(type = "Audio", index = 1, codec = "dts", language = "eng"),
        MediaStream(type = "Video", index = 0, codec = "hevc"),
    )
    private val source = MediaSourceInfo(id = "src1", size = 4_000_000_000, runTimeTicks = 72_000_000_000, mediaStreams = subs, defaultAudioStreamIndex = 1, defaultSubtitleStreamIndex = 4)
    private val movie = BaseItemDto(id = "m1", type = "Movie", mediaType = "Video", runTimeTicks = 72_000_000_000, mediaSources = listOf(source))
    private val song = BaseItemDto(id = "a1", type = "Audio", mediaType = "Audio", runTimeTicks = 2_400_000_000, mediaSources = listOf(MediaSourceInfo(id = "a1", size = 30_000_000)))

    // ---- Quality and addresses ----

    @Test fun bitrateComesFromTheRightSetting() {
        assertEquals(8_000_000L, DownloadPlan.bitrateFor(movie, videoMbps = 8, musicKbps = 192))
        assertEquals(192_000L, DownloadPlan.bitrateFor(song, videoMbps = 8, musicKbps = 192))
        assertEquals(0L, DownloadPlan.bitrateFor(movie, videoMbps = 0, musicKbps = 192))
    }

    @Test fun originalUsesTheDownloadEndpointWithoutToken() {
        val url = DownloadPlan.mediaUrl(client, movie, 0, "ps").toHttpUrl()
        assertEquals("/jellyfin/Items/m1/Download", url.encodedPath)
        assertTrue(url.queryParameterNames.isEmpty())
        assertFalse(url.toString().contains("secret-token"))
    }

    @Test fun convertedVideoAsksForH264AacMp4() {
        val url = DownloadPlan.mediaUrl(client, movie, 8_000_000, "ps1").toHttpUrl()
        assertEquals("/jellyfin/Videos/m1/stream.mp4", url.encodedPath)
        assertEquals("false", url.queryParameter("static"))
        assertEquals("h264", url.queryParameter("videoCodec"))
        assertEquals("aac", url.queryParameter("audioCodec"))
        assertEquals("7808000", url.queryParameter("videoBitRate"))
        assertEquals("192000", url.queryParameter("audioBitRate"))
        assertEquals("src1", url.queryParameter("mediaSourceId"))
        assertEquals("1", url.queryParameter("audioStreamIndex"))
        assertEquals("ps1", url.queryParameter("playSessionId"))
        assertEquals("dev1", url.queryParameter("deviceId"))
        assertEquals("Drop", url.queryParameter("subtitleMethod"))
        assertFalse(url.toString().contains("secret-token"))
    }

    @Test fun convertedMusicAsksForMp3() {
        val url = DownloadPlan.mediaUrl(client, song, 192_000, "ps").toHttpUrl()
        assertEquals("/jellyfin/Audio/a1/stream.mp3", url.encodedPath)
        assertEquals("mp3", url.queryParameter("audioCodec"))
        assertEquals("192000", url.queryParameter("audioBitRate"))
        assertEquals("mp3", DownloadPlan.convertedExtension(song))
        assertEquals("mp4", DownloadPlan.convertedExtension(movie))
    }

    @Test fun partTag() {
        assertEquals("orig", DownloadPlan.partTag(0))
        assertEquals("b8000000", DownloadPlan.partTag(8_000_000))
    }

    // ---- Subtitles ----

    @Test fun subtitleFormats() {
        assertEquals("srt", DownloadPlan.subtitleFormat("subrip"))
        assertEquals("srt", DownloadPlan.subtitleFormat("mov_text"))
        assertEquals("ass", DownloadPlan.subtitleFormat("SSA"))
        assertEquals("vtt", DownloadPlan.subtitleFormat("webvtt"))
        assertNull(DownloadPlan.subtitleFormat("PGSSUB"))
        assertNull(DownloadPlan.subtitleFormat("dvd_subtitle"))
        assertNull(DownloadPlan.subtitleFormat(null))
    }

    @Test fun originalsFetchOnlySeparateTextSubtitles() {
        assertEquals(listOf(4, 5), DownloadPlan.subtitlesToFetch(source, converted = false).map { it.index })
    }

    @Test fun conversionsFetchEveryTextSubtitle() {
        assertEquals(listOf(2, 4, 5), DownloadPlan.subtitlesToFetch(source, converted = true).map { it.index })
        assertTrue(DownloadPlan.subtitlesToFetch(null, converted = true).isEmpty())
    }

    @Test fun subtitleAddressAndLocalName() {
        val url = DownloadPlan.subtitleUrl(client, "m1", "src1", 4, "ass").toHttpUrl()
        assertEquals("/jellyfin/Videos/m1/src1/Subtitles/4/0/Stream.ass", url.encodedPath)
        assertEquals("abc.sub4.ass", DownloadPlan.subtitleFileName("abc", 4, "ass"))
    }

    // ---- Space ----

    @Test fun expectedSizeAndLimit() {
        assertEquals(4_000_000_000L, DownloadPlan.expectedBytes(movie, 0))
        // 2 hours at 8 Mbps = 7.2 GB
        assertEquals(8_000_000L / 8 * 7200, DownloadPlan.expectedBytes(movie, 8_000_000))
        val gb = 1024L * 1024 * 1024
        assertFalse(DownloadPlan.exceedsLimit(100 * gb, 100 * gb, 0))
        assertFalse(DownloadPlan.exceedsLimit(gb, gb, 2 * gb))
        assertTrue(DownloadPlan.exceedsLimit(gb, gb + 1, 2 * gb))
    }

    // ---- Next N unwatched ----

    private fun ep(s: Int, e: Int, played: Boolean = false, virtual: Boolean = false) = BaseItemDto(
        id = "s${s}e$e", type = "Episode", parentIndexNumber = s, indexNumber = e,
        userData = UserItemData(played = played), locationType = if (virtual) "Virtual" else "FileSystem",
    )

    @Test fun nextUnwatchedStartsAfterTheLastWatched() {
        val eps = listOf(ep(2, 1), ep(1, 2, played = true), ep(1, 1, played = true), ep(1, 3), ep(2, 2), ep(0, 1))
        assertEquals(listOf("s1e3", "s2e1"), DownloadPlan.nextUnwatched(eps, 2).map { it.id })
    }

    @Test fun nextUnwatchedSkipsRewatchedGapsAndMissingEpisodes() {
        // Watched up to s1e4 with s1e3 skipped: s1e3 is behind you, so it's not "next".
        val eps = listOf(ep(1, 1, true), ep(1, 2, true), ep(1, 3), ep(1, 4, true), ep(1, 5, virtual = true), ep(1, 6), ep(1, 7))
        assertEquals(listOf("s1e6", "s1e7"), DownloadPlan.nextUnwatched(eps, 5).map { it.id })
    }

    @Test fun nextUnwatchedFromTheStartAndEdgeCases() {
        val eps = listOf(ep(1, 2), ep(1, 1))
        assertEquals(listOf("s1e1"), DownloadPlan.nextUnwatched(eps, 1).map { it.id })
        assertTrue(DownloadPlan.nextUnwatched(eps, 0).isEmpty())
        assertTrue(DownloadPlan.nextUnwatched(listOf(ep(1, 1, true)), 3).isEmpty())
    }

    // ---- Offline playback with saved subtitles ----

    @Test fun offlineOriginalSideLoadsSavedSubtitlesWithServerIndexes() {
        val saved = listOf(DownloadedSub(4, "ass", "jpn", "Japanese", "x.sub4.ass") to "/data/x.sub4.ass")
        val pb = PlaybackResolver.offline(movie, "/data/x.mkv", PlayerEngineChoice.MEDIA3, converted = false, subtitles = saved)
        assertEquals(EngineKind.MEDIA3, pb.engine)
        assertEquals("file:///data/x.mkv", pb.url)
        assertEquals("file:///data/x.sub4.ass", pb.externalSubs.single().url)
        assertEquals(SubSelection.External(4), pb.subtitle) // the server's default subtitle was saved
        // Embedded subtitles stay; the server's other separate file (5, not saved) is gone.
        assertEquals(listOf(2, 3, 4), pb.subtitleStreams.map { it.index })
        assertTrue(pb.offline)
    }

    @Test fun offlineConvertedHasOneAudioTrackAndNoEmbeddedSubtitles() {
        val saved = listOf(DownloadedSub(2, "srt", "eng", null, "x.sub2.srt") to "/data/x.sub2.srt")
        val pb = PlaybackResolver.offline(movie, "/data/x.mp4", PlayerEngineChoice.MPV, converted = true, subtitles = saved)
        assertEquals(EngineKind.MPV, pb.engine)
        assertEquals("/data/x.mp4", pb.url)
        assertEquals("/data/x.sub2.srt", pb.externalSubs.single().url)
        assertEquals(1, pb.audioStreams.size)
        assertEquals("aac", pb.audioStreams.single().codec)
        assertEquals("eng", pb.audioStreams.single().language)
        assertEquals(0, pb.audioOrdinal)
        assertEquals(listOf(2), pb.subtitleStreams.map { it.index })
        // The server's default (4) wasn't saved, so subtitles start off.
        assertEquals(SubSelection.Off, pb.subtitle)
        assertNull(pb.subtitleIndex)
    }
}
