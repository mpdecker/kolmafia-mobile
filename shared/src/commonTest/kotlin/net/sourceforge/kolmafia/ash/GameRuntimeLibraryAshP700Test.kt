package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BatfellowChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP700Test {

    @Test
    fun revision_phase701() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun begins_setsBatmanOnDecision1() {
        var mode = ""
        assertTrue(BatfellowChoiceSync.apply(1133, 1) { mode = it })
        assertEquals("batman", mode)
        assertFalse(BatfellowChoiceSync.apply(1133, 2) { mode = it })
        assertEquals("batman", mode)
    }

    @Test
    fun ends_clearsOnDecision1() {
        var mode = "batman"
        assertTrue(BatfellowChoiceSync.apply(1134, 1) { mode = it })
        assertEquals("", mode)
        assertFalse(BatfellowChoiceSync.apply(1134, 2) { mode = "kept" })
        assertEquals("", mode)
    }

    @Test
    fun questChoiceRules_wires1133And1134() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val character = KoLCharacter()
        character.updateLimitMode("none")
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1133,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
                setLimitMode = { character.updateLimitMode(it) },
            ),
        )
        assertEquals("batman", character.state.value.limitMode)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1134,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
                setLimitMode = { character.updateLimitMode(it) },
            ),
        )
        assertEquals("", character.state.value.limitMode)
    }
}
