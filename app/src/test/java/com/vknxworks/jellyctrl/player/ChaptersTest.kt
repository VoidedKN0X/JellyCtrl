package com.vknxworks.jellyctrl.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChaptersTest {
    private val starts = listOf(0L, 60_000L, 120_000L)

    @Test fun nextGoesToTheFollowingChapter() {
        assertEquals(1, chapterAfter(starts, 0))
        assertEquals(1, chapterAfter(starts, 59_000))
        assertEquals(2, chapterAfter(starts, 60_000))
    }

    @Test fun nextDoesNothingInTheLastChapter() {
        assertNull(chapterAfter(starts, 120_000))
        assertNull(chapterAfter(starts, 500_000))
        assertNull(chapterAfter(emptyList(), 10))
    }

    @Test fun nextSkipsAChapterThatStartsWithinHalfASecond() {
        // Just before a chapter mark: "next" must not land on the mark that is about to pass anyway.
        assertEquals(2, chapterAfter(starts, 59_800))
    }

    @Test fun previousRestartsTheCurrentChapter() {
        assertEquals(1, chapterBefore(starts, 90_000))
        assertEquals(0, chapterBefore(starts, 30_000))
    }

    @Test fun previousGoesBackOneWhenTheChapterJustBegan() {
        assertEquals(0, chapterBefore(starts, 61_000))
        assertEquals(1, chapterBefore(starts, 122_999))
        assertEquals(2, chapterBefore(starts, 123_000))
    }

    @Test fun previousAtTheVeryStartStaysThere() {
        assertEquals(0, chapterBefore(starts, 0))
        assertEquals(0, chapterBefore(starts, 2_000))
        assertNull(chapterBefore(emptyList(), 5))
    }

    @Test fun beforeTheFirstChapterMarkCountsAsTheFirst() {
        assertEquals(0, chapterBefore(listOf(5_000L, 60_000L), 1_000))
    }
}
