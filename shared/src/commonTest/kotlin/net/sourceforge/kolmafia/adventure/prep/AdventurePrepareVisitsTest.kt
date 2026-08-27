package net.sourceforge.kolmafia.adventure.prep

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdventurePrepareVisitsTest {

    @Test
    fun mysticVisit_emittedForEightBitWhenNoTransfunctioner() = runBlocking {
        val visited = mutableListOf<String>()
        val prefs = Preferences(MapSettings())
        val ctx = AdventureGateContext(preferences = prefs)
        AdventurePrepareVisits.prepareUnlockVisits(
            locationName = "The 8-Bit Realm",
            zoneName = "The 8-Bit Realm",
            ctx = ctx,
            deps = AdventurePrepareActions.PrepareDeps(
                outfitManager = null,
                retrieveItemService = null,
                useItemRequest = null,
                gameDatabase = null,
                visitUrl = { url -> visited += url; true },
            ),
        )
        assertTrue(visited.any { it.contains("fv_mystic") })
        assertTrue(visited.any { it.contains("whichchoice=664") })
    }

    @Test
    fun trapperCabin_visitedWhenQuestUnstarted() = runBlocking {
        val visited = mutableListOf<String>()
        val prefs = Preferences(MapSettings())
        val ctx = AdventureGateContext(preferences = prefs)
        AdventurePrepareVisits.prepareUnlockVisits(
            locationName = "Itznotyerzitz Mine",
            zoneName = "McLargeHuge",
            ctx = ctx,
            deps = AdventurePrepareActions.PrepareDeps(
                outfitManager = null,
                retrieveItemService = null,
                useItemRequest = null,
                gameDatabase = null,
                visitUrl = { url -> visited += url; true },
            ),
        )
        assertEquals(
            listOf("place.php?whichplace=mclargehuge&action=trappercabin"),
            visited,
        )
    }

    @Test
    fun marketFallback_skeletonStoreTalksToMeatsmith() = runBlocking {
        val visited = mutableListOf<String>()
        val prefs = Preferences(MapSettings())
        val ctx = AdventureGateContext(preferences = prefs)
        AdventurePrepareVisits.prepareMarketNpcFallbacks(
            locationName = "The Skeleton Store",
            ctx = ctx,
            deps = AdventurePrepareActions.PrepareDeps(
                outfitManager = null,
                retrieveItemService = null,
                useItemRequest = null,
                gameDatabase = null,
                visitUrl = { url -> visited += url; true },
            ),
        )
        assertTrue(visited.any { it.contains("meatsmith") })
        assertTrue(visited.any { it.contains("whichchoice=1059") })
    }
}
