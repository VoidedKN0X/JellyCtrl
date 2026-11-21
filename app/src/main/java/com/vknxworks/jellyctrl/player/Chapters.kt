package com.vknxworks.jellyctrl.player

/** Tapping "previous chapter" this soon after a chapter began goes to the chapter before it. */
private const val CHAPTER_RESTART_MS = 3_000L

/** Index of the first chapter that starts after [positionMs] ([starts] in ms, ascending), or null at the last one. */
fun chapterAfter(starts: List<Long>, positionMs: Long): Int? =
    starts.indexOfFirst { it > positionMs + 500 }.takeIf { it >= 0 }

/**
 * Index of the chapter "previous chapter" goes to: the start of the current one, or the one before it when
 * the current one began less than three seconds ago. Null when there are no chapters.
 */
fun chapterBefore(starts: List<Long>, positionMs: Long): Int? {
    if (starts.isEmpty()) return null
    val current = starts.indexOfLast { it <= positionMs }.coerceAtLeast(0)
    return if (positionMs - starts[current] < CHAPTER_RESTART_MS && current > 0) current - 1 else current
}
