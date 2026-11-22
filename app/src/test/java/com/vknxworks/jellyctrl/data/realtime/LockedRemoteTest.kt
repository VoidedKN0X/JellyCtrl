package com.vknxworks.jellyctrl.data.realtime

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockedRemoteTest {
    @Test fun quieterCommandsWorkWhileLocked() {
        for (name in listOf("Mute", "Unmute", "ToggleMute", "VolumeDown")) assertTrue(name, generalAllowedWhileLocked(name))
    }

    @Test fun louderOrTrackCommandsWaitForUnlock() {
        for (name in listOf("VolumeUp", "SetVolume", "SetAudioStreamIndex", "SetSubtitleStreamIndex", "DisplayMessage", "Unknown")) {
            assertFalse(name, generalAllowedWhileLocked(name))
        }
    }
}
