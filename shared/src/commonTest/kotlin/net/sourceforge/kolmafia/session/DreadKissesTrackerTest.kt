package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class DreadKissesTrackerTest {

    @Test
    fun kissesForLocation_returnsMaxOneForDreadZones() {
        val tracker = DreadKissesTracker(Preferences(MapSettings()))
        assertEquals(1L, tracker.kissesForLocation("Dreadsylvanian Woods"))
        assertEquals(1L, tracker.kissesForLocation("Dreadsylvanian Village"))
        assertEquals(1L, tracker.kissesForLocation("Dreadsylvanian Castle"))
        assertEquals(0L, tracker.kissesForLocation("The Spooky Forest"))
    }

    @Test
    fun updateFromFight_parsesKissTitleWithDifficultyBonus() = runBlocking {
        AdventureDatabase.load()
        val prefs = Preferences(MapSettings())
        val tracker = DreadKissesTracker(prefs)
        val html = """<html><body>
            <span title="1 kiss for winning +2 for difficulty">You won!</span>
        </body></html>"""

        tracker.updateFromFight("Dreadsylvanian Woods", html)

        assertEquals(3L, tracker.kissesForLocation("Dreadsylvanian Woods"))
    }

    @Test
    fun updateFromFight_bossKissResetsToZero() = runBlocking {
        AdventureDatabase.load()
        val tracker = DreadKissesTracker(Preferences(MapSettings()))
        tracker.setKissesForTest("Dreadsylvanian Village", 5)
        val html = """<html><body>
            <span title="100 kisses for winning +100 for difficulty">Boss defeated!</span>
        </body></html>"""

        tracker.updateFromFight("Dreadsylvanian Village", html)

        assertEquals(1L, tracker.kissesForLocation("Dreadsylvanian Village"))
    }

    @Test
    fun updateFromFight_ignoresNonDreadsylvaniaZones() = runBlocking {
        AdventureDatabase.load()
        val tracker = DreadKissesTracker(Preferences(MapSettings()))
        val html = """<html><body>
            <span title="1 kiss for winning">You won!</span>
        </body></html>"""

        tracker.updateFromFight("The Spooky Forest", html)

        assertEquals(0L, tracker.kissesForLocation("The Spooky Forest"))
    }

    @Test
    fun persistsAcrossLoad() = runBlocking {
        val settings = MapSettings()
        val tracker1 = DreadKissesTracker(Preferences(settings))
        AdventureDatabase.load()
        tracker1.updateFromFight(
            "Dreadsylvanian Castle",
            """<html><body><span title="1 kiss for winning">win</span></body></html>""",
        )

        val tracker2 = DreadKissesTracker(Preferences(settings))
        tracker2.load()
        assertEquals(1L, tracker2.kissesForLocation("Dreadsylvanian Castle"))
    }
}
