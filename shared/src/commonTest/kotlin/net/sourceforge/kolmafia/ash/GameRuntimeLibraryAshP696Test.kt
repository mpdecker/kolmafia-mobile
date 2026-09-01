package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MadnessBakeryChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP696Test {

    @Test
    fun revision_phase701() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun popularMachine_consumesIngredients() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            MadnessBakeryChoiceSync.apply(
                choiceId = 1084,
                decision = 1,
                html = "A popular tart springs out of the machine",
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(consumed.contains(MadnessBakeryChoiceSync.WAD_OF_DOUGH to 1))
        assertTrue(consumed.contains(MadnessBakeryChoiceSync.STRAWBERRY to 1))
        assertTrue(consumed.contains(MadnessBakeryChoiceSync.ENCHANTED_ICING to 1))
    }

    @Test
    fun popularMachine_requiresPhrase() {
        assertFalse(
            MadnessBakeryChoiceSync.apply(1084, 1, "the machine whirrs"),
        )
    }

    @Test
    fun questChoiceRules_wires1084() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1084,
                responseText = "A popular tart springs out of the machine",
                questDatabase = db,
                preferences = prefs,
            ),
        )
    }
}
