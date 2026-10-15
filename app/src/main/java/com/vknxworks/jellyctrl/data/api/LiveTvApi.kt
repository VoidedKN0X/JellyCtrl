package com.vknxworks.jellyctrl.data.api

import kotlinx.serialization.json.JsonObject

suspend fun JellyfinClient.liveTvChannels(): List<BaseItemDto> =
    get<ItemsResult>(
        "LiveTv/Channels",
        mapOf(
            "userId" to requireUserId(),
            "addCurrentProgram" to true,
            "enableImageTypes" to "Primary",
            "sortBy" to "SortName",
            "fields" to listOf("PrimaryImageAspectRatio", "ChannelInfo"),
        ),
    ).items

suspend fun JellyfinClient.liveTvPrograms(
    channelIds: List<String>,
    minEndIso: String,
    maxStartIso: String,
): List<BaseItemDto> =
    get<ItemsResult>(
        "LiveTv/Programs",
        mapOf(
            "userId" to requireUserId(),
            "channelIds" to channelIds,
            "minEndDate" to minEndIso,
            "maxStartDate" to maxStartIso,
            "sortBy" to "StartDate",
            "fields" to listOf("Overview"),
            "enableTotalRecordCount" to false,
        ),
    ).items

suspend fun JellyfinClient.liveTvRecordings(): List<BaseItemDto> =
    get<ItemsResult>(
        "LiveTv/Recordings",
        mapOf("userId" to requireUserId(), "fields" to LIST_FIELDS, "enableImages" to true),
    ).items

suspend fun JellyfinClient.liveTvTimers(): List<TimerInfo> = get<TimerInfoResult>("LiveTv/Timers").items

suspend fun JellyfinClient.recordProgram(programId: String) {
    // The server hands back a timer template pre-filled for this program; post it back to schedule it.
    val defaults: JsonObject = get("LiveTv/Timers/Defaults", mapOf("programId" to programId))
    postUnit("LiveTv/Timers", body = defaults)
}

suspend fun JellyfinClient.cancelTimer(timerId: String) = deleteUnit("LiveTv/Timers/$timerId")

suspend fun JellyfinClient.deleteRecording(id: String) = deleteUnit("LiveTv/Recordings/$id")
