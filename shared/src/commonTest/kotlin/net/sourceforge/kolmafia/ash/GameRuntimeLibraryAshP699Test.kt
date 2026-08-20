package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SnojoChoiceSync

class GameRuntimeLibraryAshP699Test {

    @Test
    fun revision_phase701() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun postChoice_setsSettingFromDecision() {
        val prefs = Preferences(MapSettings())
        assertTrue(SnojoChoiceSync.apply(1118, 2, prefs))
        assertEquals("MYSTICALITY", prefs.getString("snojoSetting"))
        assertTrue(SnojoChoiceSync.apply(1118, 4, prefs))
        assertEquals("TOURNAMENT", prefs.getString("snojoSetting"))
    }

    @Test
    fun visit_parsesModeOrClears() {
        val prefs = Preferences(MapSettings())
        prefs.setString("snojoSetting", "MUSCLE")
        assertTrue(
            SnojoChoiceSync.applyVisit(
                1118,
                "<b>MOXIE MODE</b>",
                prefs,
            ),
        )
        assertEquals("MOXIE", prefs.getString("snojoSetting"))
        assertTrue(SnojoChoiceSync.applyVisit(1118, "no console heading", prefs))
        assertEquals("", prefs.getString("snojoSetting"))
    }

    @Test
    fun questChoiceRules_wires1118() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1118,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals("MUSCLE", prefs.getString("snojoSetting"))
    }
}
