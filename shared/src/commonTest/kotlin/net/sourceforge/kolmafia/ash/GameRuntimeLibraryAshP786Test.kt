package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SeadentWaveChoiceSync

class GameRuntimeLibraryAshP786Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun summon_setsZoneAndUsed() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            SeadentWaveChoiceSync.apply(
                choiceId = 1566,
                decision = 1,
                html = "You sweep it down and point at The Sunken Ship.  A huge wave rises from the sea",
                preferences = prefs,
            ),
        )
        assertEquals("The Sunken Ship", prefs.getString("_seadentWaveZone", ""))
        assertTrue(prefs.getBoolean("_seadentWaveUsed", false))
    }

    @Test
    fun otherDecision_noop() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            SeadentWaveChoiceSync.apply(
                1566,
                2,
                "You sweep it down and point at Somewhere.  A huge wave rises from the sea",
                prefs,
            ),
        )
    }

    @Test
    fun questChoiceRules_wires1566() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1566,
                responseText = "sweep it down and point at Oasis.  A huge wave rises from the sea",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals("Oasis", prefs.getString("_seadentWaveZone", ""))
    }
}
