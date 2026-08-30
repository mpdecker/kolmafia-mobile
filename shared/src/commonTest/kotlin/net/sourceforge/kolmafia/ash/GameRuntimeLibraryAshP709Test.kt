package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.ZombieBaitChoiceSync

class GameRuntimeLibraryAshP709Test {

    @Test
    fun revision_phase713() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun zombieBait_missingQuantityIsNoOp() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertFalse(
            ZombieBaitChoiceSync.apply(
                choiceId = 599,
                decision = 1,
                choiceUrl = "whichchoice=599&option=1",
                itemCount = { 10 },
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(consumed.isEmpty())
    }

    @Test
    fun zombieBait_clampsToInventory() {
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            ZombieBaitChoiceSync.apply(
                choiceId = 599,
                decision = 2,
                choiceUrl = "choice.php?whichchoice=599&option=2&quantity=8",
                itemCount = { if (it == ZombieBaitChoiceSync.DECENT_BRAIN) 3 else 0 },
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertEquals(listOf(ZombieBaitChoiceSync.DECENT_BRAIN to 3), consumed)
    }

    @Test
    fun questChoiceRules_wires599() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 599,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
                choiceUrl = "whichchoice=599&quantity=1",
                itemCount = { 5 },
            ),
        )
    }
}
