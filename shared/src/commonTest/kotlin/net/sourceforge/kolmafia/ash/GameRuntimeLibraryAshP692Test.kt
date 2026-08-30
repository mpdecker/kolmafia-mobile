package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AutopsyChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP692Test {

    @Test
    fun revision_phase695() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun autopsy_consumesTweezersAndIncrements() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("autopsyTweezersUsed", 2)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            AutopsyChoiceSync.apply(
                choiceId = 589,
                html = "The tweezers you used dissolve in the caustic fluid. Rats.",
                preferences = prefs,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(consumed.contains(AutopsyChoiceSync.AUTOPSY_TWEEZERS to 1))
        assertEquals(3, prefs.getInt("autopsyTweezersUsed"))
    }

    @Test
    fun questChoiceRules_wires589() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 589,
                responseText = "dissolve in the caustic fluid",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt("autopsyTweezersUsed"))
    }
}
