package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BatfellowItemChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP712Test {

    @Test
    fun revision_phase713() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun conservatory_consumesGlueOrKits() {
        val glue = mutableListOf<Pair<Int, Int>>()
        assertTrue(BatfellowItemChoiceSync.apply(1140, 4) { id, qty -> glue.add(id to qty) })
        assertEquals(listOf(BatfellowItemChoiceSync.GLOB_OF_BAT_GLUE to 1), glue)
        val kits = mutableListOf<Pair<Int, Int>>()
        assertTrue(BatfellowItemChoiceSync.apply(1140, 5) { id, qty -> kits.add(id to qty) })
        assertEquals(listOf(BatfellowItemChoiceSync.FINGERPRINT_DUSTING_KIT to 3), kits)
    }

    @Test
    fun reservoirAndCemetery_consumeAidAndKickballs() {
        val aid = mutableListOf<Pair<Int, Int>>()
        assertTrue(BatfellowItemChoiceSync.apply(1141, 4) { id, qty -> aid.add(id to qty) })
        assertEquals(listOf(BatfellowItemChoiceSync.BAT_AID_BANDAGE to 1), aid)
        val ultra = mutableListOf<Pair<Int, Int>>()
        assertTrue(BatfellowItemChoiceSync.apply(1141, 5) { id, qty -> ultra.add(id to qty) })
        assertEquals(listOf(BatfellowItemChoiceSync.ULTRACOAGULATOR to 3), ultra)
        val bearing = mutableListOf<Pair<Int, Int>>()
        assertTrue(BatfellowItemChoiceSync.apply(1142, 4) { id, qty -> bearing.add(id to qty) })
        assertEquals(listOf(BatfellowItemChoiceSync.BAT_BEARING to 1), bearing)
        val kick = mutableListOf<Pair<Int, Int>>()
        assertTrue(BatfellowItemChoiceSync.apply(1142, 5) { id, qty -> kick.add(id to qty) })
        assertEquals(listOf(BatfellowItemChoiceSync.EXPLODING_KICKBALL to 3), kick)
    }

    @Test
    fun questChoiceRules_wires1140() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1140,
                responseText = "",
                questDatabase = db,
                decision = 4,
                preferences = prefs,
            ),
        )
    }
}
