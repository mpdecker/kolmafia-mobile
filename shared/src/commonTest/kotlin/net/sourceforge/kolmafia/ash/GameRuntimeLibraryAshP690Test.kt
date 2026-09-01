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

class GameRuntimeLibraryAshP690Test {

    @Test
    fun revision_phase695() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun bagelmat_consumesDough() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            MadnessBakeryChoiceSync.apply(
                choiceId = 1080,
                decision = 1,
                html = "You shove a wad of dough into the slot",
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(consumed.contains(MadnessBakeryChoiceSync.WAD_OF_DOUGH to 1))
    }

    @Test
    fun bagelmat_requiresPhrase() {
        assertFalse(
            MadnessBakeryChoiceSync.apply(1080, 1, "the machine whirrs"),
        )
    }

    @Test
    fun questChoiceRules_wires1080() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1080,
                responseText = "You shove a wad of dough into the slot",
                questDatabase = db,
                preferences = prefs,
            ),
        )
    }
}
