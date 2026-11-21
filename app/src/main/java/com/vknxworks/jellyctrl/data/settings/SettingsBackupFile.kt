package com.vknxworks.jellyctrl.data.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

/** Thrown for a file that isn't a JellyCtrl settings file, or one written by a newer, incompatible version. */
class BackupFormatException(val newer: Boolean) : Exception()

/**
 * A settings file the user keeps: JellyCtrl's own settings as plain JSON, and nothing else. It never holds
 * a password, an access token, the app PIN or any key, and no account or server: those stay on the phone.
 * [values] are the settings by name; a value is a Boolean, an Int or a String.
 */
data class SettingsBackupFile(val appVersion: String?, val createdAt: String?, val values: Map<String, Any>) {
    companion object {
        const val FORMAT = "jellyctrl-settings"
        const val VERSION = 1

        /** A settings file is a few kilobytes; anything far bigger isn't one. */
        const val MAX_BYTES = 128 * 1024

        /**
         * Never written and never read, even from a file edited by hand: the app lock can't be turned on, off
         * or loosened by a file. (Passwords, tokens and keys aren't settings and have no name here at all.)
         */
        val NEVER = setOf("appLock", "lockTimeout", "app_lock", "lock_timeout")

        private val json = Json { prettyPrint = true }

        fun write(appVersion: String, createdAt: String, values: Map<String, Any>): String = json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("format", FORMAT)
                put("version", VERSION)
                put("appVersion", appVersion)
                put("createdAt", createdAt)
                put("settings", buildJsonObject {
                    values.filterKeys { it !in NEVER }.toSortedMap().forEach { (name, value) ->
                        when (value) {
                            is Boolean -> put(name, value)
                            is Int -> put(name, value)
                            is String -> put(name, value)
                            else -> error("unsupported setting type for $name")
                        }
                    }
                })
            },
        )

        fun read(text: String): SettingsBackupFile {
            if (text.length > MAX_BYTES) throw BackupFormatException(newer = false)
            val root = runCatching { Json.parseToJsonElement(text) as? JsonObject }.getOrNull() ?: throw BackupFormatException(newer = false)
            if ((root["format"] as? JsonPrimitive)?.takeIf { it.isString }?.content != FORMAT) throw BackupFormatException(newer = false)
            val version = (root["version"] as? JsonPrimitive)?.intOrNull ?: throw BackupFormatException(newer = false)
            if (version > VERSION) throw BackupFormatException(newer = true)
            if (version < 1) throw BackupFormatException(newer = false)
            val settings = root["settings"] as? JsonObject ?: throw BackupFormatException(newer = false)
            val values = buildMap<String, Any> {
                settings.forEach { (name, element) ->
                    if (name in NEVER) return@forEach
                    val p = element as? JsonPrimitive ?: return@forEach
                    val value: Any? = when {
                        p.isString -> p.content.takeIf { it.length <= 400 }
                        p.booleanOrNull != null -> p.booleanOrNull
                        else -> p.intOrNull
                    }
                    if (value != null) put(name, value)
                }
            }
            fun text(name: String) = (root[name] as? JsonPrimitive)?.takeIf { it.isString }?.content?.take(40)
            return SettingsBackupFile(text("appVersion"), text("createdAt"), values)
        }
    }
}
