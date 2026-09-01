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

class GameRuntimeLibraryAshP707Test {

    @Test
    fun revision_phase707() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun specifici_requiresItemid() {
        val prefs = Preferences(MapSettings())
        assertFalse(TeaTreeChoiceSync.apply(1105, 1, prefs, choiceUrl = "whichchoice=1105"))
        assertFalse(prefs.getBoolean("_pottedTeaTreeUsed"))
        assertTrue(
            TeaTreeChoiceSync.apply(1105, 1, prefs, choiceUrl = "whichchoice=1105&itemid=1234"),
        )
        assertTrue(prefs.getBoolean("_pottedTeaTreeUsed"))
    }

    @Test
    fun questChoiceRules_wires1105() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1105,
                responseText = "",
                questDatabase = db,
                preferences = prefs,
                choiceUrl = "choice.php?whichchoice=1105&itemid=99",
            ),
        )
        assertTrue(prefs.getBoolean("_pottedTeaTreeUsed"))
    }
}
