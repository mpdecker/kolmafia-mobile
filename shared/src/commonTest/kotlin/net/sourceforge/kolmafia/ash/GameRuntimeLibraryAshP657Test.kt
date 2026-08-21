package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestFightStartedSync
import net.sourceforge.kolmafia.session.CryptManager

class GameRuntimeLibraryAshP657Test {

    @Test
    fun revision_phase659() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun encounterBoss_clampsAboveThirteen() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptAlcoveEvilness", 50)
        prefs.setInt("cyrptTotalEvilness", 200)
        assertTrue(CryptManager.encounterBoss("conjoined zmombie", prefs))
        assertEquals(13, prefs.getInt("cyrptAlcoveEvilness"))
        assertEquals(163, prefs.getInt("cyrptTotalEvilness"))
    }

    @Test
    fun encounterBoss_skipsWhenAlreadyThirteenOrLess() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptCrannyEvilness", 13)
        prefs.setInt("cyrptTotalEvilness", 13)
        assertFalse(CryptManager.encounterBoss("huge ghuol", prefs))
        assertEquals(13, prefs.getInt("cyrptCrannyEvilness"))
    }

    @Test
    fun fightStarted_wiresEncounterBoss() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("cyrptNicheEvilness", 40)
        prefs.setInt("cyrptTotalEvilness", 80)
        assertTrue(
            QuestFightStartedSync.apply(
                monster = "gargantulihc",
                html = "",
                preferences = prefs,
                turnsPlayed = 10,
            ),
        )
        assertEquals(13, prefs.getInt("cyrptNicheEvilness"))
        assertEquals(53, prefs.getInt("cyrptTotalEvilness"))
    }
}
