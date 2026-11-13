package com.vknxworks.jellyctrl.data.download

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressRulesTest {
    private val hour = 36_000_000_000L // ticks

    private fun p(pos: Long, at: Long = 1, item: String = "i1", account: String = "a1", runtime: Long? = hour) =
        PendingProgress(account, item, pos, runtime, at)

    @Test fun nearTheEndCountsAsWatched() {
        assertEquals(UserDataUpdate(0, played = true), ProgressRules.toUpdate(p(hour * 90 / 100)))
        assertEquals(UserDataUpdate(0, played = true), ProgressRules.toUpdate(p(hour)))
    }

    @Test fun middleIsAResumePoint() {
        assertEquals(UserDataUpdate(hour / 2, played = false), ProgressRules.toUpdate(p(hour / 2)))
    }

    @Test fun barelyStartedSendsNothing() {
        assertNull(ProgressRules.toUpdate(p(hour * 4 / 100)))
    }

    @Test fun unknownLengthSendsThePosition() {
        assertEquals(UserDataUpdate(123, played = false), ProgressRules.toUpdate(p(123, runtime = null)))
        assertEquals(UserDataUpdate(0, played = false), ProgressRules.toUpdate(p(-5, runtime = 0)))
    }

    @Test fun mergeKeepsTheLatestPerAccountAndItem() {
        var list = emptyList<PendingProgress>()
        list = ProgressRules.merge(list, p(10, at = 1))
        list = ProgressRules.merge(list, p(20, at = 3))
        list = ProgressRules.merge(list, p(15, at = 2)) // older: ignored
        list = ProgressRules.merge(list, p(5, at = 1, item = "i2"))
        list = ProgressRules.merge(list, p(7, at = 1, account = "a2"))
        assertEquals(3, list.size)
        assertEquals(20L, list.first { it.itemId == "i1" && it.accountKey == "a1" }.positionTicks)
    }
}
