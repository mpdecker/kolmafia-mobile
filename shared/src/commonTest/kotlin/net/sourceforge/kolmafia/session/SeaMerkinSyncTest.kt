package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.preferences.Preferences

class SeaMerkinSyncTest {

    private fun prefs() = Preferences(MapSettings())

    @Test
    fun parseTemple_seaPathLeft_setsShubDefeated() {
        val prefs = prefs()

        SeaMerkinSync.parseTemple(
            url = "sea_merkin.php?action=temple&subaction=left",
            html = "This part of the temple is now empty.",
            inSeaPath = true,
            preferences = prefs,
            sessionLogger = null,
        )

        assertTrue(prefs.getBoolean("shubJigguwattDefeated", false))
        assertFalse(prefs.getBoolean("yogUrtDefeated", false))
    }

    @Test
    fun parseTemple_seaPathRight_setsYogUrtDefeated() {
        val prefs = prefs()

        SeaMerkinSync.parseTemple(
            url = "sea_merkin.php?action=temple&subaction=right",
            html = "This part of the temple is now empty.",
            inSeaPath = true,
            preferences = prefs,
            sessionLogger = null,
        )

        assertTrue(prefs.getBoolean("yogUrtDefeated", false))
        assertFalse(prefs.getBoolean("shubJigguwattDefeated", false))
    }

    @Test
    fun parseTemple_nonSeaPathEmpty_setsQuestDone() {
        val prefs = prefs()
        prefs.setString("merkinQuestPath", "scholar")

        SeaMerkinSync.parseTemple(
            url = "sea_merkin.php?action=temple",
            html = "The temple is empty.",
            inSeaPath = false,
            preferences = prefs,
            sessionLogger = null,
        )

        assertEquals("done", prefs.getString("merkinQuestPath", ""))
    }

    @Test
    fun parseTemple_wrongAction_noOp() {
        val prefs = prefs()
        prefs.setString("merkinQuestPath", "scholar")

        SeaMerkinSync.parseTemple(
            url = "sea_merkin.php?action=colosseum",
            html = "The temple is empty.",
            inSeaPath = false,
            preferences = prefs,
            sessionLogger = null,
        )

        assertEquals("scholar", prefs.getString("merkinQuestPath", ""))
    }

    @Test
    fun parseColosseum_admirers_setsGladiatorPrefs() {
        val prefs = prefs()

        SeaMerkinSync.parseColosseum(
            url = "adventure.php?snarfblat=${SeaMerkinSync.MERKIN_COLOSSEUM_SNARFBLEAT}",
            html = "your crowd of Mer-kin admirers has gone home.",
            preferences = prefs,
            sessionLogger = null,
        )

        assertTrue(prefs.getBoolean("isMerkinGladiatorChampion", false))
        assertEquals("gladiator", prefs.getString("merkinQuestPath", ""))
        assertEquals(15, prefs.getInt("lastColosseumRoundWon", 0))
    }

    @Test
    fun parseColosseum_highPriest_setsScholarPrefs() {
        val prefs = prefs()

        SeaMerkinSync.parseColosseum(
            url = "adventure.php?snarfblat=${SeaMerkinSync.MERKIN_COLOSSEUM_SNARFBLEAT}",
            html = "Praise be to the High Priest!",
            preferences = prefs,
            sessionLogger = null,
        )

        assertTrue(prefs.getBoolean("isMerkinHighPriest", false))
        assertEquals("scholar", prefs.getString("merkinQuestPath", ""))
    }

    @Test
    fun parseColosseum_questDone_noOp() {
        val prefs = prefs()
        prefs.setString("merkinQuestPath", "done")

        SeaMerkinSync.parseColosseum(
            url = "adventure.php?snarfblat=${SeaMerkinSync.MERKIN_COLOSSEUM_SNARFBLEAT}",
            html = "your crowd of Mer-kin admirers has gone home.",
            preferences = prefs,
            sessionLogger = null,
        )

        assertFalse(prefs.getBoolean("isMerkinGladiatorChampion", false))
        assertEquals("done", prefs.getString("merkinQuestPath", ""))
    }

    @Test
    fun parseColosseum_wrongSnarfblat_noOp() {
        val prefs = prefs()

        SeaMerkinSync.parseColosseum(
            url = "adventure.php?snarfblat=1",
            html = "your crowd of Mer-kin admirers has gone home.",
            preferences = prefs,
            sessionLogger = null,
        )

        assertFalse(prefs.getBoolean("isMerkinGladiatorChampion", false))
        assertEquals("", prefs.getString("merkinQuestPath", ""))
    }

    @Test
    fun processVisitResponseHooks_routesSeaMerkinTemple() {
        val prefs = prefs()
        val lib = GameRuntimeLibrary(preferences = prefs)

        lib.processVisitResponseHooks(
            html = "The temple is empty.",
            url = "sea_merkin.php?action=temple",
        )

        assertEquals("done", prefs.getString("merkinQuestPath", ""))
    }
}
