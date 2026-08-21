package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.HashingChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP816Test {

    @Test
    fun revision_phase826() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_consumesSchematicByIid() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            HashingChoiceSync.apply(
                choiceId = 1551,
                html = "You crush the schematic into little bits of checksum.",
                choiceUrl = "choice.php?whichchoice=1551&option=1&iid=9999",
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(listOf(9999 to 1), consumed)
    }

    @Test
    fun post_withoutCrushText_noop() {
        assertFalse(
            HashingChoiceSync.apply(
                choiceId = 1551,
                html = "nope",
                choiceUrl = "iid=1",
            ),
        )
    }

    @Test
    fun questChoiceRules_wires1551() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1551,
                responseText = "You crush the schematic into little bits of checksum.",
                questDatabase = QuestDatabase(prefs),
                preferences = prefs,
                choiceUrl = "iid=42",
            ),
        )
    }
}
