package com.vknxworks.jellyctrl.data.settings

/**
 * The rows of the Home screen, in their default order. The names are stored in the settings, so they must
 * not change; a row added in a later version is simply not in a stored order yet (see [homeOrderFrom]).
 */
enum class HomeSection {
    /** The big accent card with the title you were last watching. */
    CONTINUE_CARD,
    /** The artwork carousel of new films and shows. */
    FEATURED,
    CONTINUE_WATCHING,
    NEXT_UP,
    /** Episodes that air soon, from the server's "Upcoming" list. */
    UPCOMING,
    LIVE_TV,
    CONTINUE_LISTENING,
    /** One "New in ..." row per library. */
    LATEST,
}

/** Where in the stored list each section goes; missing or unknown names don't break anything. */
fun homeOrderFrom(stored: String?): List<HomeSection> {
    val known = stored.orEmpty().split(',')
        .mapNotNull { name -> HomeSection.entries.firstOrNull { it.name == name.trim() } }
        .distinct()
    if (known.isEmpty()) return HomeSection.entries
    // A row this version added: placed after the row that comes before it by default, or first.
    val result = known.toMutableList()
    HomeSection.entries.forEachIndexed { i, section ->
        if (section in result) return@forEachIndexed
        val before = HomeSection.entries.take(i).lastOrNull { it in result }
        result.add(if (before == null) 0 else result.indexOf(before) + 1, section)
    }
    return result
}

fun homeHiddenFrom(stored: String?): Set<HomeSection> =
    stored.orEmpty().split(',').mapNotNull { name -> HomeSection.entries.firstOrNull { it.name == name.trim() } }.toSet()

/** [order] with [section] moved one place up ([delta] -1) or down (+1); unchanged at either end. */
fun moveHomeSection(order: List<HomeSection>, section: HomeSection, delta: Int): List<HomeSection> {
    val from = order.indexOf(section)
    val to = from + delta
    if (from < 0 || to < 0 || to > order.lastIndex) return order
    return order.toMutableList().apply { add(to, removeAt(from)) }
}
