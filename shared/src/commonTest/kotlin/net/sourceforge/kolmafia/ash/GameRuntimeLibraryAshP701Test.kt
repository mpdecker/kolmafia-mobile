package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BatfellowChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP701Test {

    @Test
    fun revision_phase701() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun timeout_clearsLimitMode() {
        var mode = "batman"
        assertTrue(BatfellowChoiceSync.apply(1168, 0) { mode = it })
        assertEquals("", mode)
    }

    @Test
    fun questChoiceRules_wires1168() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val character = KoLCharacter()
        character.updateLimitMode("batman")
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1168,
                responseText = "",
                questDatabase = db,
                decision = 0,
                preferences = prefs,
                setLimitMode = { character.updateLimitMode(it) },
            ),
        )
        assertEquals("", character.state.value.limitMode)
    }
}
