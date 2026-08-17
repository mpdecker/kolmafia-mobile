package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.TownUnlockSync

class GameRuntimeLibraryAshP598Test {

    @Test
    fun townRight_setsHorseryAndVote() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            TownUnlockSync.applyFromTownRight(
                url = "place.php?whichplace=town_right",
                html = "Voting Booth Horsery Madness Bakery",
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_voteToday", false))
        assertTrue(prefs.getBoolean("horseryAvailable", false))
        assertTrue(prefs.getBoolean("madnessBakeryAvailable", false))
    }

    @Test
    fun townWrong_setsNepAndSpeakeasy() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            TownUnlockSync.applyFromTownWrong(
                url = "place.php?whichplace=town_wrong",
                html = """
                    Precinct
                    The Neverending Party
                    Speakeasy <div id=town_speakeasyname title="Cool Kid Club">
                    Overgrown Lot
                """.trimIndent(),
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("hasDetectiveSchool", false))
        assertTrue(prefs.getBoolean("_neverendingPartyToday", false))
        assertTrue(prefs.getBoolean("ownsSpeakeasy", false))
        assertEquals("Cool Kid Club", prefs.getString("speakeasyName", ""))
        assertTrue(prefs.getBoolean("overgrownLotAvailable", false))
    }

    @Test
    fun townMarket_setsSkeletonStore() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            TownUnlockSync.applyFromTownMarket(
                url = "place.php?whichplace=town_market",
                html = "The Skeleton Store",
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("skeletonStoreAvailable", false))
    }

    @Test
    fun speakeasy_setsFreeFightsWhenPaid() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            TownUnlockSync.applyFromSpeakeasy(
                url = "place.php?whichplace=speakeasy",
                html = "welcome",
                preferences = prefs,
            ),
        )
        assertEquals(3, prefs.getInt("_speakeasyFreeFights", 0))
    }

    @Test
    fun town_setsEldritchFlags() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            TownUnlockSync.applyFromTown(
                url = "place.php?whichplace=town",
                html = "town_eincursion town_eicfight2",
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("eldritchFissureAvailable", false))
        assertTrue(prefs.getBoolean("eldritchHorrorAvailable", false))
    }
}
