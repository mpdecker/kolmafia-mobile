package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CandyCaneSwordSync
import net.sourceforge.kolmafia.quest.HiddenCityChoiceSync
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP611Test {

    @Test
    fun revision_phase611() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun shore_decision5_setsCandyCane() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            CandyCaneSwordSync.applyFromChoice(
                choiceId = 793,
                decision = 5,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("candyCaneSwordShore", false))
    }

    @Test
    fun warHippy_andFunHouse_andLyle() {
        val prefs = Preferences(MapSettings())
        assertTrue(CandyCaneSwordSync.applyFromChoice(139, 4, prefs))
        assertTrue(CandyCaneSwordSync.applyFromChoice(151, 3, prefs))
        assertTrue(
            CandyCaneSwordSync.applyFromChoice(
                choiceId = 1309,
                decision = 1,
                preferences = prefs,
                hasCandyCaneSwordEquipped = true,
            ),
        )
        assertTrue(prefs.getBoolean("candyCaneSwordWarHippyBait", false))
        assertTrue(prefs.getBoolean("candyCaneSwordFunHouse", false))
        assertTrue(prefs.getBoolean("_candyCaneSwordLyle", false))
    }

    @Test
    fun hiddenCity_stillOwnedByHiddenCityChoiceSync() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            CandyCaneSwordSync.applyFromChoice(
                choiceId = 780,
                decision = 4,
                preferences = prefs,
            ),
        )
        assertFalse(prefs.getBoolean("candyCaneSwordApartmentBuilding", false))
        val db = QuestDatabase(prefs)
        assertTrue(
            HiddenCityChoiceSync.applyPostChoice(
                choiceId = 780,
                html = "",
                decision = 4,
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("candyCaneSwordApartmentBuilding", false))
    }
}
