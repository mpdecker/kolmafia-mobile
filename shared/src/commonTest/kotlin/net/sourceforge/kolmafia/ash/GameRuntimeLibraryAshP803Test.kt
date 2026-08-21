package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.GarbageToteChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP803Test {

    @Test
    fun revision_phase826() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun post_firstChangeResetsCharges() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            GarbageToteChoiceSync.apply(
                choiceId = 1275,
                decision = 1,
                html = "",
                preferences = prefs,
            ),
        )
        assertEquals(1000, prefs.getInt("garbageTreeCharge", 0))
        assertEquals(11, prefs.getInt("garbageChampagneCharge", 0))
        assertEquals(37, prefs.getInt("garbageShirtCharge", 0))
        assertEquals(true, prefs.getBoolean("_garbageItemChanged", false))
    }

    @Test
    fun post_parsesChargePatterns() {
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("_garbageItemChanged", true)
        val html = """
            Looks like it has 42 needles left
            Looks like it has 7 ounces remaining
            Looks like you can read roughly 9 scraps
        """.trimIndent()
        assertTrue(
            GarbageToteChoiceSync.apply(
                choiceId = 1275,
                decision = 0,
                html = html,
                preferences = prefs,
            ),
        )
        assertEquals(42, prefs.getInt("garbageTreeCharge", 0))
        assertEquals(7, prefs.getInt("garbageChampagneCharge", 0))
        assertEquals(9, prefs.getInt("garbageShirtCharge", 0))
    }

    @Test
    fun questChoiceRules_wires1275() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1275,
                responseText = "Looks like it has 3 ounces",
                questDatabase = QuestDatabase(prefs),
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals(true, prefs.getBoolean("_garbageItemChanged", false))
        assertEquals(3, prefs.getInt("garbageChampagneCharge", 0))
    }
}
