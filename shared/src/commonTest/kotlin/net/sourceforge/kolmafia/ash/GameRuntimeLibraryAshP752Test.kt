package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PowerPlantChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP752Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_zerosHarvestedStalk() {
        val prefs = Preferences(MapSettings())
        prefs.setString(PowerPlantChoiceSync.STALKS_PREF, "3,2,7,1,0,4,5")
        assertTrue(
            PowerPlantChoiceSync.apply(
                choiceId = 1448,
                html = "You acquire an item: battery",
                preferences = prefs,
                choiceUrl = "choice.php?whichchoice=1448&option=1&pp=3",
            ),
        )
        assertEquals("3,2,0,1,0,4,5", prefs.getString(PowerPlantChoiceSync.STALKS_PREF, ""))
    }

    @Test
    fun post_requiresAcquire() {
        val prefs = Preferences(MapSettings())
        prefs.setString(PowerPlantChoiceSync.STALKS_PREF, "3,2,7,1,0,4,5")
        assertFalse(
            PowerPlantChoiceSync.apply(1448, "nothing happened", prefs, "pp=1"),
        )
    }

    @Test
    fun questChoiceRules_wires1448() {
        val prefs = Preferences(MapSettings())
        prefs.setString(PowerPlantChoiceSync.STALKS_PREF, "9,,,,,,")
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1448,
                responseText = "You acquire an item: something",
                questDatabase = db,
                preferences = prefs,
                choiceUrl = "pp=1",
            ),
        )
        assertTrue(prefs.getString(PowerPlantChoiceSync.STALKS_PREF, "").startsWith("0,"))
    }
}
