package com.vknxworks.jellyctrl.data.settings

import com.vknxworks.jellyctrl.data.settings.HomeSection.CONTINUE_CARD
import com.vknxworks.jellyctrl.data.settings.HomeSection.CONTINUE_LISTENING
import com.vknxworks.jellyctrl.data.settings.HomeSection.CONTINUE_WATCHING
import com.vknxworks.jellyctrl.data.settings.HomeSection.FEATURED
import com.vknxworks.jellyctrl.data.settings.HomeSection.LATEST
import com.vknxworks.jellyctrl.data.settings.HomeSection.LIVE_TV
import com.vknxworks.jellyctrl.data.settings.HomeSection.NEXT_UP
import com.vknxworks.jellyctrl.data.settings.HomeSection.UPCOMING
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeLayoutTest {
    @Test fun nothingStoredGivesTheDefaultOrder() {
        assertEquals(HomeSection.entries, homeOrderFrom(null))
        assertEquals(HomeSection.entries, homeOrderFrom(""))
        assertEquals(HomeSection.entries, homeOrderFrom("NOPE,,ALSO_NOT"))
    }

    @Test fun aStoredOrderIsKept() {
        val stored = "LATEST,UPCOMING,NEXT_UP,CONTINUE_WATCHING,FEATURED,CONTINUE_CARD,LIVE_TV,CONTINUE_LISTENING"
        assertEquals(
            listOf(LATEST, UPCOMING, NEXT_UP, CONTINUE_WATCHING, FEATURED, CONTINUE_CARD, LIVE_TV, CONTINUE_LISTENING),
            homeOrderFrom(stored),
        )
    }

    @Test fun aRowMissingFromTheStoredOrderGoesAfterItsDefaultNeighbour() {
        // An order saved before "Upcoming" existed: it lands right after Next up, wherever that was moved to.
        assertEquals(
            listOf(LATEST, NEXT_UP, UPCOMING, CONTINUE_CARD, FEATURED, CONTINUE_WATCHING, LIVE_TV, CONTINUE_LISTENING),
            homeOrderFrom("LATEST,NEXT_UP,CONTINUE_CARD,FEATURED,CONTINUE_WATCHING,LIVE_TV,CONTINUE_LISTENING"),
        )
        // Nothing that comes before it by default is stored: it goes first.
        assertEquals(listOf(CONTINUE_CARD, FEATURED, CONTINUE_WATCHING, NEXT_UP, UPCOMING, LIVE_TV, CONTINUE_LISTENING, LATEST).first(), homeOrderFrom("LATEST").first())
        assertEquals(HomeSection.entries.size, homeOrderFrom("LATEST").size)
        assertEquals(LATEST, homeOrderFrom("LATEST").last())
    }

    @Test fun duplicatesAndUnknownNamesAreDropped() {
        assertEquals(HomeSection.entries.size, homeOrderFrom("LATEST,LATEST,WHAT,FEATURED").size)
        assertEquals(HomeSection.entries.toSet(), homeOrderFrom("LATEST,LATEST,WHAT,FEATURED").toSet())
    }

    @Test fun hiddenIgnoresUnknownNames() {
        assertEquals(setOf(LIVE_TV, UPCOMING), homeHiddenFrom("LIVE_TV, UPCOMING,GONE"))
        assertEquals(emptySet<HomeSection>(), homeHiddenFrom(null))
    }

    @Test fun movingStaysInsideTheList() {
        val order = HomeSection.entries
        assertEquals(order, moveHomeSection(order, CONTINUE_CARD, -1))
        assertEquals(order, moveHomeSection(order, LATEST, 1))
        assertEquals(listOf(FEATURED, CONTINUE_CARD), moveHomeSection(order, CONTINUE_CARD, 1).take(2))
        assertEquals(listOf(LATEST, CONTINUE_LISTENING), moveHomeSection(order, LATEST, -1).takeLast(2))
    }
}
