package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules
import net.sourceforge.kolmafia.session.CryptManager

class GameRuntimeLibraryAshP658Test {

    @Test
    fun revision_phase659() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun handleFightEvilness_singleBeepAndVacuum() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptAlcoveEvilness", 50)
        prefs.setInt("cyrptTotalEvilness", 200)
        assertTrue(
            CryptManager.handleFightEvilness(
                html = "Your Evilometer emits a single beep. The ghost vacuum sucks up some extra evil.",
                adventureId = CryptManager.DEFILED_ALCOVE.toString(),
                preferences = prefs,
            ),
        )
        assertEquals(48, prefs.getInt("cyrptAlcoveEvilness"))
        assertEquals(198, prefs.getInt("cyrptTotalEvilness"))
    }

    @Test
    fun handleFightEvilness_beepPattern() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptNookEvilness", 40)
        prefs.setInt("cyrptTotalEvilness", 80)
        assertTrue(
            CryptManager.handleFightEvilness(
                html = "Your Evilometer beeps 4 times.",
                adventureId = CryptManager.DEFILED_NOOK.toString(),
                preferences = prefs,
            ),
        )
        assertEquals(36, prefs.getInt("cyrptNookEvilness"))
        assertEquals(76, prefs.getInt("cyrptTotalEvilness"))
    }

    @Test
    fun handleFightEvilness_nightmareFuel() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptNookEvilness", 40)
        prefs.setInt("cyrptTotalEvilness", 80)
        prefs.setInt("_nightmareFuelCharges", 3)
        assertTrue(
            CryptManager.handleFightEvilness(
                html = "The evil of the nightmare fuel in your system is in a different phase.",
                adventureId = CryptManager.DEFILED_NOOK.toString(),
                preferences = prefs,
            ),
        )
        assertEquals(38, prefs.getInt("cyrptNookEvilness"))
        assertEquals(78, prefs.getInt("cyrptTotalEvilness"))
        assertEquals(2, prefs.getInt("_nightmareFuelCharges"))
    }

    @Test
    fun handleFightEvilness_lovebugAndLocationFallback() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptCrannyEvilness", 20)
        prefs.setInt("cyrptTotalEvilness", 20)
        assertTrue(
            CryptManager.handleFightEvilness(
                html = "The Defiled Cranny. Your Evilometer beeps once. Some gravy sloshes.",
                adventureId = "",
                preferences = prefs,
            ),
        )
        assertEquals(18, prefs.getInt("cyrptCrannyEvilness"))
        assertEquals(18, prefs.getInt("cyrptTotalEvilness"))
    }

    @Test
    fun applyCombat_wiresBeepsWithoutRequiringWin() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptNicheEvilness", 30)
        prefs.setInt("cyrptTotalEvilness", 60)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                monster = "lihc",
                won = false,
                preferences = prefs,
                adventureId = CryptManager.DEFILED_NICHE.toString(),
                responseText = "Your Evilometer beeps three times.",
            ).advanced,
        )
        assertEquals(27, prefs.getInt("cyrptNicheEvilness"))
    }

    @Test
    fun handleFightEvilness_withoutKeywordsIsNoOp() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptAlcoveEvilness", 50)
        assertFalse(
            CryptManager.handleFightEvilness(
                html = "You win the fight!",
                adventureId = CryptManager.DEFILED_ALCOVE.toString(),
                preferences = prefs,
            ),
        )
        assertEquals(50, prefs.getInt("cyrptAlcoveEvilness"))
    }
}
