package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MayoMinderChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP684Test {

    @Test
    fun revision_phase689() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun decision2_setsMayodiol() {
        val prefs = Preferences(MapSettings())
        assertTrue(MayoMinderChoiceSync.apply(1076, 2, prefs))
        assertEquals("Mayodiol", prefs.getString("mayoMinderSetting"))
    }

    @Test
    fun decision6_clearsSetting() {
        val prefs = Preferences(MapSettings())
        prefs.setString("mayoMinderSetting", "Mayonex")
        assertTrue(MayoMinderChoiceSync.apply(1076, 6, prefs))
        assertEquals("", prefs.getString("mayoMinderSetting"))
    }

    @Test
    fun questChoiceRules_wires1076() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1076,
                responseText = "Mayo Minder",
                questDatabase = db,
                decision = 5,
                preferences = prefs,
            ),
        )
        assertEquals("Mayoflex", prefs.getString("mayoMinderSetting"))
    }
}
