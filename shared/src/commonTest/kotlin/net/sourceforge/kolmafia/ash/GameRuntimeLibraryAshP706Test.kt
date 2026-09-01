package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.TeaTreeChoiceSync

class GameRuntimeLibraryAshP706Test {

    @Test
    fun revision_phase707() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun treeTea_setsUsedOnDecision1() {
        val prefs = Preferences(MapSettings())
        assertTrue(TeaTreeChoiceSync.apply(1104, 1, prefs))
        assertTrue(prefs.getBoolean("_pottedTeaTreeUsed"))
        val prefs2 = Preferences(MapSettings())
        assertFalse(TeaTreeChoiceSync.apply(1104, 2, prefs2))
        assertFalse(prefs2.getBoolean("_pottedTeaTreeUsed"))
    }

    @Test
    fun questChoiceRules_wires1104() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1104,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_pottedTeaTreeUsed"))
    }
}
