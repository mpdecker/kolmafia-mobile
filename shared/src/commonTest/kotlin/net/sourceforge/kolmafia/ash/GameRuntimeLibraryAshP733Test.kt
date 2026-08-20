package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.VillainLairChoiceSync

class GameRuntimeLibraryAshP733Test {

    @Test
    fun panel_plus10() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(VillainLairChoiceSync.PROGRESS_PREF, 0)
        assertTrue(
            VillainLairChoiceSync.apply(
                choiceId = 1260,
                decision = 1,
                html = "You take out 10 casualties among the guards.",
                preferences = prefs,
            ),
        )
        assertEquals(10, prefs.getInt(VillainLairChoiceSync.PROGRESS_PREF, 0))
        assertTrue(prefs.getBoolean(VillainLairChoiceSync.COLOR_USED_PREF))
    }

    @Test
    fun panel_defaultMinus7() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(VillainLairChoiceSync.PROGRESS_PREF, 10)
        assertTrue(
            VillainLairChoiceSync.apply(
                choiceId = 1260,
                decision = 2,
                html = "Wrong button. Oops.",
                preferences = prefs,
            ),
        )
        assertEquals(3, prefs.getInt(VillainLairChoiceSync.PROGRESS_PREF, 0))
    }

    @Test
    fun setting_plus20() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            VillainLairChoiceSync.apply(
                choiceId = 1262,
                decision = 1,
                html = "You blast 20 of the minions.",
                preferences = prefs,
            ),
        )
        assertEquals(20, prefs.getInt(VillainLairChoiceSync.PROGRESS_PREF, 0))
        assertTrue(prefs.getBoolean(VillainLairChoiceSync.SYMBOLOGY_USED_PREF))
    }

    @Test
    fun setting_minus15() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(VillainLairChoiceSync.PROGRESS_PREF, 30)
        assertTrue(
            VillainLairChoiceSync.apply(
                choiceId = 1262,
                decision = 3,
                html = "You wake 15 aquanats from slumber.",
                preferences = prefs,
            ),
        )
        assertEquals(15, prefs.getInt(VillainLairChoiceSync.PROGRESS_PREF, 0))
    }

    @Test
    fun questChoiceRules_wires1262() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1262,
                responseText = "You blast 20 soldiers.",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertTrue(prefs.getBoolean(VillainLairChoiceSync.SYMBOLOGY_USED_PREF))
    }

    @Test
    fun ignoresOtherChoices() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            VillainLairChoiceSync.apply(
                choiceId = 1219,
                decision = 1,
                html = "10 casualties",
                preferences = prefs,
            ),
        )
    }
}
