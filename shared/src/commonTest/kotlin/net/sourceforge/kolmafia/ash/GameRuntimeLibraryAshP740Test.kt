package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CrimboShrubChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP740Test {

    @Test
    fun decoratesFromUrlFields() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            CrimboShrubChoiceSync.apply(
                choiceId = 999,
                decision = 1,
                preferences = prefs,
                choiceUrl = "topper=2&lights=1&garland=3&gift=2",
            ),
        )
        assertTrue(prefs.getBoolean("_shrubDecorated"))
        assertEquals("Mysticality", prefs.getString("shrubTopper", ""))
        assertEquals("prismatic", prefs.getString("shrubLights", ""))
        assertEquals("blocking", prefs.getString("shrubGarland", ""))
        assertEquals("meat", prefs.getString("shrubGifts", ""))
    }

    @Test
    fun questChoiceRules_wires999() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 999,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
                choiceUrl = "topper=1&lights=2&garland=1&gift=1",
            ),
        )
        assertEquals("Muscle", prefs.getString("shrubTopper", ""))
        assertEquals("Hot", prefs.getString("shrubLights", ""))
    }

    @Test
    fun ignoresNonDecorateDecision() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            CrimboShrubChoiceSync.apply(999, 2, prefs, "topper=1"),
        )
    }
}
