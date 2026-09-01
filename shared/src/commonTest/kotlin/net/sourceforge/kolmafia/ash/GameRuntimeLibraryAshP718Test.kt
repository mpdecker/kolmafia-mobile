package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.LoveTunnelChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP718Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun loveTunnel_setsOnDecision1() {
        val prefs = Preferences(MapSettings())
        assertFalse(LoveTunnelChoiceSync.apply(1222, 2, prefs))
        assertFalse(prefs.getBoolean("_loveTunnelUsed"))
        assertTrue(LoveTunnelChoiceSync.apply(1222, 1, prefs))
        assertTrue(prefs.getBoolean("_loveTunnelUsed"))
    }

    @Test
    fun questChoiceRules_wires1222() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1222,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean("_loveTunnelUsed"))
    }
}
