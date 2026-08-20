package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PalindomeSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.SpookyravenManorVisitSync

class GameRuntimeLibraryAshP686Test {

    @Test
    fun revision_phase689() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun edChoice_finishesPalindomeAndMacguffin() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            PalindomeSync.applyFromEdChoice(
                html = "Rot in a jar of dog paws!",
                questDatabase = db,
                itemCount = { 0 },
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.PALINDOME))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.MACGUFFIN))
        assertTrue(consumed.contains(SpookyravenManorVisitSync.ED_FATS_STAFF to 1))
    }

    @Test
    fun edChoice_keepsMacguffinIfEyeRemains() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        PalindomeSync.applyFromEdChoice(
            html = "Rot in a jar of dog paws!",
            questDatabase = db,
            itemCount = { id -> if (id == SpookyravenManorVisitSync.ED_EYE) 1 else 0 },
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.PALINDOME))
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.MACGUFFIN))
    }

    @Test
    fun questChoiceRules_wires872() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 872,
                responseText = "Rot in a jar of dog paws!",
                questDatabase = db,
                itemCount = { 0 },
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.PALINDOME))
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.MACGUFFIN))
    }
}
