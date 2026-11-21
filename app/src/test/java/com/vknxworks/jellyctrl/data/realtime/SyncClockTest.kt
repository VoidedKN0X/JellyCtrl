package com.vknxworks.jellyctrl.data.realtime

import org.junit.Assert.assertEquals
import org.junit.Test

class SyncClockTest {
    @Test fun sameClocksGiveNoOffset() {
        // 40 ms there, 10 ms on the server, 40 ms back.
        assertEquals(0, SyncClock.offsetMs(sent = 1_000, serverIn = 1_040, serverOut = 1_050, received = 1_090))
        assertEquals(80, SyncClock.roundTripMs(1_000, 1_040, 1_050, 1_090))
    }

    @Test fun aServerThatIsAheadGivesAPositiveOffset() {
        // The server's clock is 5 s ahead of the phone's.
        assertEquals(5_000, SyncClock.offsetMs(sent = 1_000, serverIn = 6_040, serverOut = 6_050, received = 1_090))
    }

    @Test fun aServerThatIsBehindGivesANegativeOffset() {
        assertEquals(-3_000, SyncClock.offsetMs(sent = 10_000, serverIn = 7_020, serverOut = 7_020, received = 10_040))
    }

    @Test fun theRoundTripIsNeverNegative() {
        // A server that reports more time than passed on the phone (clock jitter) must not give a negative delay.
        assertEquals(0, SyncClock.roundTripMs(sent = 1_000, serverIn = 2_000, serverOut = 2_500, received = 1_100))
    }
}
