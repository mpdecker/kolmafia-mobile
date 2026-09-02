package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.CouncilChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP787Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun freeRalph_setsPrefAndCallback() {
        val prefs = Preferences(MapSettings())
        var liberated = false
        assertTrue(
            CouncilChoiceSync.apply(
                choiceId = 1565,
                html = "You free King Ralph, signalling a triumphant end to your submaritime adventure!",
                preferences = prefs,
                setKingLiberated = { liberated = true },
            ),
        )
        assertTrue(prefs.getBoolean("kingLiberated", false))
        assertTrue(liberated)
    }

    @Test
    fun withoutText_noop() {
        val prefs = Preferences(MapSettings())
        assertFalse(CouncilChoiceSync.apply(1565, "council business as usual", prefs))
    }

    @Test
    fun questChoiceRules_wires1565() {
        val prefs = Preferences(MapSettings())
        var called = false
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1565,
                responseText = "You free King Ralph, signalling a triumphant end to your submaritime adventure",
                questDatabase = QuestDatabase(prefs),
                preferences = prefs,
                setKingLiberated = { called = true },
            ),
        )
        assertTrue(called)
        assertTrue(prefs.getBoolean("kingLiberated", false))
    }
}
