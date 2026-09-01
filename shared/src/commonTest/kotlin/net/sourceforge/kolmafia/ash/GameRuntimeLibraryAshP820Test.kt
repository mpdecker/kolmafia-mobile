package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DaycareChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP820Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesDaycareStats() {
        val prefs = Preferences(MapSettings())
        val html = """
            Looks like 1,234 pieces in all. 56 toddlers are training with an instructor
            <font color=blue><b>[1,000 Meat]</b></font>
        """.trimIndent()
        assertTrue(DaycareChoiceSync.applyVisit(1336, html, prefs))
        assertEquals("1234", prefs.getString("daycareEquipment", ""))
        assertEquals("56", prefs.getString("daycareToddlers", ""))
        assertEquals("1", prefs.getString("daycareInstructors", ""))
        assertEquals(1, prefs.getInt("_daycareRecruits", 0))
    }

    @Test
    fun post_recruitIncrements() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            DaycareChoiceSync.apply(
                choiceId = 1336,
                decision = 1,
                html = "You attract 3 new children",
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt("_daycareRecruits", 0))
    }

    @Test
    fun post_scavengeHalvesWithBreakfast() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            DaycareChoiceSync.apply(
                choiceId = 1336,
                decision = 2,
                html = "You manage to find 20 used pieces",
                preferences = prefs,
                hasBoxingDayBreakfast = true,
            ),
        )
        assertEquals(10, prefs.getInt("daycareLastScavenge", 0))
        assertEquals(1, prefs.getInt("_daycareGymScavenges", 0))
    }

    @Test
    fun post_sparSetsFights() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            DaycareChoiceSync.apply(
                choiceId = 1336,
                decision = 4,
                html = "You step into the ring",
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_daycareFights", false))
    }

    @Test
    fun questChoiceRules_wires1336() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1336,
                responseText = "You attract 1 new children",
                questDatabase = QuestDatabase(prefs),
                decision = 1,
                preferences = prefs,
            ),
        )
        assertEquals(1, prefs.getInt("_daycareRecruits", 0))
    }
}
