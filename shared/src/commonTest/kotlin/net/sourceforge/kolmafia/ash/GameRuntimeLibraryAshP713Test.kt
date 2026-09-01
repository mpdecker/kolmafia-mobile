package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.BatfellowItemChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP713Test {

    @Test
    fun revision_phase713() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun decision4_consumesZoneItems() {
        val mapping = listOf(
            1143 to BatfellowItemChoiceSync.BAT_OOMERANG,
            1144 to BatfellowItemChoiceSync.BAT_O_MITE,
            1145 to BatfellowItemChoiceSync.BAT_JUTE,
            1146 to BatfellowItemChoiceSync.EXPLODING_KICKBALL,
            1147 to BatfellowItemChoiceSync.ULTRACOAGULATOR,
            1148 to BatfellowItemChoiceSync.FINGERPRINT_DUSTING_KIT,
        )
        for ((choiceId, itemId) in mapping) {
            val consumed = mutableListOf<Pair<Int, Int>>()
            assertTrue(BatfellowItemChoiceSync.apply(choiceId, 4) { id, qty -> consumed.add(id to qty) })
            assertEquals(listOf(itemId to 1), consumed)
            assertFalse(BatfellowItemChoiceSync.apply(choiceId, 1) { _, _ -> })
        }
    }

    @Test
    fun questChoiceRules_wires1148() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1148,
                responseText = "",
                questDatabase = db,
                decision = 4,
                preferences = prefs,
            ),
        )
    }
}
