package com.vknxworks.jellyctrl.data.realtime

/**
 * One measurement of the server's clock against this phone's, the way NTP does it: the phone notes when it
 * asked ([sent]) and when the answer came ([received]); the server says when it got the question ([serverIn])
 * and when it answered ([serverOut]). All in ms.
 */
object SyncClock {
    /** Time spent on the way (there and back), without the time the server took to answer. Never negative. */
    fun roundTripMs(sent: Long, serverIn: Long, serverOut: Long, received: Long): Long =
        ((received - sent) - (serverOut - serverIn)).coerceAtLeast(0)

    /** Server clock minus phone clock, assuming the way there took as long as the way back. */
    fun offsetMs(sent: Long, serverIn: Long, serverOut: Long, received: Long): Long =
        ((serverIn - sent) + (serverOut - received)) / 2
}
