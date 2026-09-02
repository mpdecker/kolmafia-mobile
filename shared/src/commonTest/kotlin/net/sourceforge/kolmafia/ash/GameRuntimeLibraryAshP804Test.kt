package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PillKeeperChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP804Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_setsFreeUsed() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            PillKeeperChoiceSync.apply(
                choiceId = 1395,
                decision = 1,
                html = "You take a day's worth of pills",
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_freePillKeeperUsed", false))
    }

    @Test
    fun post_decision3_forcesNc() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            PillKeeperChoiceSync.apply(
                choiceId = 1395,
                decision = 3,
                html = "day's worth of pills swallowed",
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("noncombatForcerActive", false))
    }

    @Test
    fun post_secondUse_adjustsSpleen() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_freePillKeeperUsed", true)
        var spleen = 0
        assertTrue(
            PillKeeperChoiceSync.apply(
                choiceId = 1395,
                decision = 2,
                html = "day's worth of pills",
                preferences = prefs,
                adjustSpleen = { spleen += it },
            ),
        )
        assertEquals(3, spleen)
    }

    @Test
    fun post_withoutSuccessText_noop() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            PillKeeperChoiceSync.apply(
                choiceId = 1395,
                decision = 1,
                html = "not enough spleen",
                preferences = prefs,
            ),
        )
    }

    @Test
    fun questChoiceRules_wires1395() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1395,
                responseText = "day's worth of pills",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_freePillKeeperUsed", false))
    }
}
