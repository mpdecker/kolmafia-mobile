package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.event.GameEventBus
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IslandWarVisitLogSyncTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun sessionLogger(prefs: Preferences): SessionLogger =
        SessionLogger(prefs, GameEventBus())

    private fun sessionLog(prefs: Preferences): String =
        prefs.getString(SessionLogger.SESSION_LOG_KEY, "")

    private fun context(
        hasItem: Set<Int> = emptySet(),
    ): IslandWarVisitSync.IslandVisitContext =
        IslandWarVisitSync.IslandVisitContext(
            hasItemId = { id -> id in hasItem },
        )

    @Test
    fun register_concertPlace_logsArena() {
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        assertTrue(
            IslandWarVisitLogSync.register(
                url = "bigisland.php?place=concert",
                html = "",
                preferences = prefs,
                context = context(),
                sessionLogger = logger,
            ),
        )
        assertTrue(sessionLog(prefs).contains("Visiting the Mysterious Island Arena"))
    }

    @Test
    fun register_concertAction_logsOption() {
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        assertTrue(
            IslandWarVisitLogSync.register(
                url = "bigisland.php?action=concert&option=2",
                html = "",
                preferences = prefs,
                context = context(),
                sessionLogger = logger,
            ),
        )
        assertTrue(sessionLog(prefs).contains("concert 2"))
    }

    @Test
    fun register_junkman_logsYossarian() {
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        assertTrue(
            IslandWarVisitLogSync.register(
                url = "bigisland.php?action=junkman",
                html = "",
                preferences = prefs,
                context = context(),
                sessionLogger = logger,
            ),
        )
        assertTrue(sessionLog(prefs).contains("Visiting Yossarian"))
    }

    @Test
    fun register_pyro_logsGunpowderCount() {
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        assertTrue(
            IslandWarVisitLogSync.register(
                url = "bigisland.php?action=pyro",
                html = "",
                preferences = prefs,
                context = context(hasItem = setOf(2403)),
                sessionLogger = logger,
            ),
        )
        assertTrue(sessionLog(prefs).contains("1 barrel"))
    }

    @Test
    fun register_camp_setsLastCampVisited() = kotlinx.coroutines.runBlocking {
        net.sourceforge.kolmafia.data.GameDatabase().load()
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        assertTrue(
            IslandWarVisitLogSync.register(
                url = "bigisland.php?place=camp&whichcamp=1",
                html = "",
                preferences = prefs,
                context = context(),
                sessionLogger = logger,
            ),
        )
        assertEquals("dimemaster", prefs.getString(IslandWarVisitLogSync.PREF_LAST_CAMP_VISITED, ""))
        assertFalse(sessionLog(prefs).contains("Visiting"))
    }

    @Test
    fun register_bossfight_afterDimemaster_setsBattlefieldLocation() {
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        prefs.setString(IslandWarVisitLogSync.PREF_LAST_CAMP_VISITED, "dimemaster")
        assertTrue(
            IslandWarVisitLogSync.register(
                url = "bigisland.php?action=bossfight",
                html = "",
                preferences = prefs,
                context = context(),
                sessionLogger = logger,
            ),
        )
        assertEquals("The Battlefield (Frat Uniform)", prefs.getString(Preferences.LAST_LOCATION, ""))
        assertTrue(sessionLog(prefs).contains("Hippy Camp"))
    }

    @Test
    fun register_bossfight_withoutCamp_fallsBackToHeadquarters() {
        val prefs = prefs()
        val logger = sessionLogger(prefs)
        assertTrue(
            IslandWarVisitLogSync.register(
                url = "bigisland.php?action=bossfight",
                html = "",
                preferences = prefs,
                context = context(),
                sessionLogger = logger,
            ),
        )
        assertEquals("The Battlefield (Hippy Uniform)", prefs.getString(Preferences.LAST_LOCATION, ""))
        assertTrue(sessionLog(prefs).contains("Headquarters"))
    }

    @Test
    fun register_unknownAction_returnsFalse() {
        val prefs = prefs()
        assertFalse(
            IslandWarVisitLogSync.register(
                url = "bigisland.php?action=unknown",
                html = "",
                preferences = prefs,
                context = context(),
                sessionLogger = null,
            ),
        )
    }

    @Test
    fun register_nonBigisland_returnsFalse() {
        val prefs = prefs()
        assertFalse(
            IslandWarVisitLogSync.register(
                url = "postwarisland.php?action=junkman",
                html = "",
                preferences = prefs,
                context = context(),
                sessionLogger = null,
            ),
        )
    }
}
