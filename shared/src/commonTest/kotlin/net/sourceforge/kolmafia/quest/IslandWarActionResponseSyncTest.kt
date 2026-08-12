package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IslandWarActionResponseSyncTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun context(
        itemCounts: Map<Int, Int> = emptyMap(),
        consumed: MutableList<Pair<Int, Int>> = mutableListOf(),
    ): IslandWarVisitSync.IslandVisitContext =
        IslandWarVisitSync.IslandVisitContext(
            hasItemId = { id -> (itemCounts[id] ?: 0) > 0 },
            consumeItem = { id, qty -> consumed.add(id to qty) },
            itemCount = { id -> itemCounts[id] ?: 0 },
        )

    @Test
    fun parseActionResponse_concertEffectAcquire_setsConcertVisited() {
        val prefs = prefs()
        assertTrue(
            IslandWarActionResponseSync.parseActionResponse(
                url = "bigisland.php?action=concert&option=1",
                html = "You acquire an effect: Moon'd",
                preferences = prefs,
                context = context(),
            ),
        )
        assertTrue(prefs.getBoolean(IslandWarActionResponseSync.PREF_CONCERT_VISITED, false))
    }

    @Test
    fun parseActionResponse_concertTappedOut_setsConcertVisited() {
        val prefs = prefs()
        assertTrue(
            IslandWarActionResponseSync.parseActionResponse(
                url = "postwarisland.php?action=concert&option=2",
                html = "you think you've pretty much tapped out this event's entertainment potential for today",
                preferences = prefs,
                context = context(),
            ),
        )
        assertTrue(prefs.getBoolean(IslandWarActionResponseSync.PREF_CONCERT_VISITED, false))
    }

    @Test
    fun parseActionResponse_concertRockedOut_setsConcertVisited() {
        val prefs = prefs()
        assertTrue(
            IslandWarActionResponseSync.parseActionResponse(
                url = "bigisland.php?action=concert&option=3",
                html = "You're all rocked out.",
                preferences = prefs,
                context = context(),
            ),
        )
        assertTrue(prefs.getBoolean(IslandWarActionResponseSync.PREF_CONCERT_VISITED, false))
    }

    @Test
    fun parseActionResponse_concertUnrelatedHtml_leavesPrefFalse() {
        val prefs = prefs()
        assertFalse(
            IslandWarActionResponseSync.parseActionResponse(
                url = "bigisland.php?action=concert&option=1",
                html = "The stage at the Mysterious Island Arena is empty",
                preferences = prefs,
                context = context(),
            ),
        )
        assertFalse(prefs.getBoolean(IslandWarActionResponseSync.PREF_CONCERT_VISITED, false))
    }

    @Test
    fun parseActionResponse_farmerSuccess_setsFarmerItemsCollected() {
        val prefs = prefs()
        assertTrue(
            IslandWarActionResponseSync.parseActionResponse(
                url = "bigisland.php?action=farmer",
                html = "Ach, here ye are, laddie. Take yer megatofu.",
                preferences = prefs,
                context = context(),
            ),
        )
        assertTrue(prefs.getBoolean(IslandWarActionResponseSync.PREF_FARMER_ITEMS_COLLECTED, false))
    }

    @Test
    fun parseActionResponse_farmerAlreadyCollected_setsFarmerItemsCollected() {
        val prefs = prefs()
        assertTrue(
            IslandWarActionResponseSync.parseActionResponse(
                url = "bigisland.php?action=farmer",
                html = "Ye already got yer stuff today, ye ken?",
                preferences = prefs,
                context = context(),
            ),
        )
        assertTrue(prefs.getBoolean(IslandWarActionResponseSync.PREF_FARMER_ITEMS_COLLECTED, false))
    }

    @Test
    fun parseActionResponse_pyroEyesLightUp_consumesAllGunpowder() {
        val prefs = prefs()
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            IslandWarActionResponseSync.parseActionResponse(
                url = "bigisland.php?action=pyro",
                html = "The Lighthouse Keeper's eyes light up as he sees your gunpowder.",
                preferences = prefs,
                context = context(itemCounts = mapOf(2403 to 3), consumed = consumed),
            ),
        )
        assertEquals(listOf(2403 to 3), consumed)
    }

    @Test
    fun parseActionResponse_pyroWithoutGunpowder_noConsume() {
        val prefs = prefs()
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertFalse(
            IslandWarActionResponseSync.parseActionResponse(
                url = "bigisland.php?action=pyro",
                html = "The Lighthouse Keeper's eyes light up as he sees your gunpowder.",
                preferences = prefs,
                context = context(consumed = consumed),
            ),
        )
        assertTrue(consumed.isEmpty())
    }

    @Test
    fun parseActionResponse_unknownAction_returnsFalse() {
        val prefs = prefs()
        assertFalse(
            IslandWarActionResponseSync.parseActionResponse(
                url = "bigisland.php?action=junkman",
                html = "You acquire an effect: Moon'd",
                preferences = prefs,
                context = context(),
            ),
        )
        assertFalse(prefs.getBoolean(IslandWarActionResponseSync.PREF_CONCERT_VISITED, false))
    }
}
