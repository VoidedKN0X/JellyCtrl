package com.vknxworks.jellyctrl.data.api

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.JsonPrimitive

// ---------- system ----------

suspend fun JellyfinClient.systemInfo(): SystemInfo = get("System/Info")
suspend fun JellyfinClient.restartServer() = postUnit("System/Restart")
suspend fun JellyfinClient.shutdownServer() = postUnit("System/Shutdown")

suspend fun JellyfinClient.serverConfig(): JsonObject = get("System/Configuration")
suspend fun JellyfinClient.saveServerConfig(config: JsonObject) = postUnit("System/Configuration", body = config)

/** Named configuration sections: "encoding", "network", "branding", "livetv", "metadata", "xbmcmetadata". */
suspend fun JellyfinClient.namedConfig(key: String): JsonObject = get("System/Configuration/$key")
suspend fun JellyfinClient.saveNamedConfig(key: String, config: JsonObject) = postUnit("System/Configuration/$key", body = config)

suspend fun JellyfinClient.activityLog(startIndex: Int = 0, limit: Int = 100): ActivityLogResult =
    get("System/ActivityLog/Entries", mapOf("startIndex" to startIndex, "limit" to limit))

suspend fun JellyfinClient.serverLogs(): List<LogFile> = get("System/Logs")
suspend fun JellyfinClient.serverLog(name: String): String = getText("System/Logs/Log", mapOf("name" to name))

// ---------- users ----------

suspend fun JellyfinClient.allUsers(): List<UserDto> = get("Users")
suspend fun JellyfinClient.userRaw(id: String): JsonObject = get("Users/$id")

suspend fun JellyfinClient.createUser(name: String, password: String): UserDto =
    post("Users/New", body = buildJsonObject {
        put("Name", name)
        put("Password", password)
    })

suspend fun JellyfinClient.deleteUser(id: String) = deleteUnit("Users/$id")

suspend fun JellyfinClient.updateUserPolicy(id: String, policy: JsonObject) = postUnit("Users/$id/Policy", body = policy)

/** Rename etc. Sends the full user object back. */
suspend fun JellyfinClient.updateUser(id: String, user: JsonObject) = postUnit("Users", mapOf("userId" to id), user)

suspend fun JellyfinClient.adminSetPassword(id: String, newPassword: String) {
    // Reset first so no current password is required, then set the new one.
    postUnit("Users/Password", mapOf("userId" to id), buildJsonObject { put("ResetPassword", true) })
    if (newPassword.isNotEmpty()) {
        postUnit("Users/Password", mapOf("userId" to id), buildJsonObject {
            put("CurrentPw", "")
            put("NewPw", newPassword)
        })
    }
}

// ---------- libraries ----------

suspend fun JellyfinClient.virtualFolders(): List<VirtualFolderInfo> = get("Library/VirtualFolders")

suspend fun JellyfinClient.addVirtualFolder(name: String, collectionType: String?, paths: List<String>) =
    postUnit(
        "Library/VirtualFolders",
        mapOf("name" to name, "collectionType" to collectionType, "refreshLibrary" to true),
        buildJsonObject {
            putJsonObject("LibraryOptions") {
                putJsonArray("PathInfos") {
                    paths.forEach { p -> add(buildJsonObject { put("Path", p) }) }
                }
                put("EnableRealtimeMonitor", true)
                put("EnableChapterImageExtraction", false)
                put("EnableTrickplayImageExtraction", false)
            }
        },
    )

suspend fun JellyfinClient.removeVirtualFolder(name: String) =
    deleteUnit("Library/VirtualFolders", mapOf("name" to name, "refreshLibrary" to true))

suspend fun JellyfinClient.renameVirtualFolder(name: String, newName: String) =
    postUnit("Library/VirtualFolders/Name", mapOf("name" to name, "newName" to newName, "refreshLibrary" to true))

suspend fun JellyfinClient.addLibraryPath(name: String, path: String) =
    postUnit(
        "Library/VirtualFolders/Paths",
        mapOf("refreshLibrary" to true),
        buildJsonObject {
            put("Name", name)
            putJsonObject("PathInfo") { put("Path", path) }
        },
    )

suspend fun JellyfinClient.removeLibraryPath(name: String, path: String) =
    deleteUnit("Library/VirtualFolders/Paths", mapOf("name" to name, "path" to path, "refreshLibrary" to true))

suspend fun JellyfinClient.scanAllLibraries() = postUnit("Library/Refresh")

suspend fun JellyfinClient.drives(): List<FileSystemEntry> = get("Environment/Drives")

suspend fun JellyfinClient.directoryContents(path: String): List<FileSystemEntry> =
    get("Environment/DirectoryContents", mapOf("path" to path, "includeDirectories" to true, "includeFiles" to false))

suspend fun JellyfinClient.parentPath(path: String): String? =
    runCatching { getText("Environment/ParentPath", mapOf("path" to path)).trim('"', ' ', '\n') }.getOrNull()?.ifBlank { null }

// ---------- scheduled tasks ----------

suspend fun JellyfinClient.scheduledTasks(): List<TaskInfo> = get("ScheduledTasks", mapOf("isHidden" to false))
suspend fun JellyfinClient.startTask(id: String) = postUnit("ScheduledTasks/Running/$id")
suspend fun JellyfinClient.stopTask(id: String) = deleteUnit("ScheduledTasks/Running/$id")

// ---------- plugins ----------

suspend fun JellyfinClient.plugins(): List<PluginInfo> = get("Plugins")
suspend fun JellyfinClient.enablePlugin(p: PluginInfo) = postUnit("Plugins/${p.id}/${p.version}/Enable")
suspend fun JellyfinClient.disablePlugin(p: PluginInfo) = postUnit("Plugins/${p.id}/${p.version}/Disable")
suspend fun JellyfinClient.uninstallPlugin(p: PluginInfo) = deleteUnit("Plugins/${p.id}/${p.version}")

suspend fun JellyfinClient.packages(): List<PackageInfo> =
    PlainJson.decodeFromString(kotlinx.serialization.builtins.ListSerializer(PackageInfo.serializer()), getText("Packages"))

suspend fun JellyfinClient.installPackage(pkg: PackageInfo, version: String) =
    postUnit("Packages/Installed/${JellyfinClient.enc(pkg.name).replace("+", "%20")}", mapOf("assemblyGuid" to pkg.guid, "version" to version))

suspend fun JellyfinClient.installingPackages(): List<JsonObject> = runCatching { get<List<JsonObject>>("Packages/Installing") }.getOrDefault(emptyList())

// ---------- devices & keys ----------

suspend fun JellyfinClient.devices(): List<DeviceInfo> = get<DeviceInfoResult>("Devices").items
suspend fun JellyfinClient.deleteDevice(id: String) = deleteUnit("Devices", mapOf("id" to id))

suspend fun JellyfinClient.apiKeys(): List<AuthenticationInfo> = get<AuthenticationInfoResult>("Auth/Keys").items
suspend fun JellyfinClient.createApiKey(app: String) = postUnit("Auth/Keys", mapOf("app" to app))
suspend fun JellyfinClient.revokeApiKey(key: String) = deleteUnit("Auth/Keys/$key")

// ---------- metadata ----------

suspend fun JellyfinClient.metadataEditorInfo(id: String): JsonObject = get("Items/$id/MetadataEditor")

fun JsonObject.withValues(changes: Map<String, Any?>): JsonObject = JsonObject(
    toMutableMap().apply {
        changes.forEach { (k, v) ->
            this[k] = when (v) {
                null -> kotlinx.serialization.json.JsonNull
                is Boolean -> JsonPrimitive(v)
                is Number -> JsonPrimitive(v)
                is String -> JsonPrimitive(v)
                is List<*> -> kotlinx.serialization.json.JsonArray(v.map { JsonPrimitive(it?.toString()) })
                is kotlinx.serialization.json.JsonElement -> v
                else -> JsonPrimitive(v.toString())
            }
        }
    }
)
