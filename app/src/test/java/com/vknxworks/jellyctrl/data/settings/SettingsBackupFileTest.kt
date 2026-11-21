package com.vknxworks.jellyctrl.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class SettingsBackupFileTest {
    private val values = mapOf<String, Any>("autoplayNext" to false, "seekStepSeconds" to 30, "engine" to "MPV")

    @Test fun whatIsWrittenReadsBackTheSame() {
        val text = SettingsBackupFile.write("0.9.0", "2026-11-21T20:00:00+01:00", values)
        val file = SettingsBackupFile.read(text)
        assertEquals(values, file.values)
        assertEquals("0.9.0", file.appVersion)
        assertEquals("2026-11-21T20:00:00+01:00", file.createdAt)
    }

    @Test fun theAppLockIsNeverWritten() {
        val text = SettingsBackupFile.write("0.9.0", "now", values + mapOf("appLock" to true, "lockTimeout" to "RESTART"))
        assertFalse(text.contains("appLock"))
        assertFalse(text.contains("lockTimeout"))
    }

    @Test fun theAppLockIsNeverReadEvenFromAnEditedFile() {
        val text = """{"format":"jellyctrl-settings","version":1,"settings":{"appLock":false,"lockTimeout":"RESTART","app_lock":false,"gestures":false}}"""
        assertEquals(mapOf<String, Any>("gestures" to false), SettingsBackupFile.read(text).values)
    }

    @Test fun otherFilesAreRefused() {
        listOf("", "hello", "[]", "{}", """{"format":"printctrl-settings","version":1,"settings":{}}""", """{"format":"jellyctrl-settings","settings":{}}""",
            """{"format":"jellyctrl-settings","version":1}""", """{"format":"jellyctrl-settings","version":0,"settings":{}}""").forEach { text ->
            try {
                SettingsBackupFile.read(text)
                fail("accepted: $text")
            } catch (e: BackupFormatException) {
                assertFalse(e.newer)
            }
        }
    }

    @Test fun aNewerFileSaysSo() {
        try {
            SettingsBackupFile.read("""{"format":"jellyctrl-settings","version":2,"settings":{}}""")
            fail("accepted a newer file")
        } catch (e: BackupFormatException) {
            assertTrue(e.newer)
        }
    }

    @Test fun valuesThatAreNotPlainAreSkipped() {
        val text = """{"format":"jellyctrl-settings","version":1,"settings":{"a":[1],"b":{"x":1},"c":null,"d":1.5,"e":7,"f":"x","g":true}}"""
        assertEquals(mapOf<String, Any>("e" to 7, "f" to "x", "g" to true), SettingsBackupFile.read(text).values)
    }

    @Test fun aHugeFileIsRefused() {
        try {
            SettingsBackupFile.read(" ".repeat(SettingsBackupFile.MAX_BYTES + 1))
            fail("accepted a huge file")
        } catch (e: BackupFormatException) {
            assertFalse(e.newer)
        }
    }
}
