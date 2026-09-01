package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DinseyKioskChoiceSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP675Test {

    @Test
    fun revision_phase677() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun kiosk_sufficientClearsTrashQuest() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        db.setProgress(Quest.FISH_TRASH, "step1")
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            DinseyKioskChoiceSync.apply(
                choiceId = DinseyKioskChoiceSync.KIOSK,
                decision = 1,
                html = "Performance Review:  Sufficient",
                questDatabase = db,
                preferences = prefs,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertEquals(QuestDatabase.UNSTARTED, db.getProgress(Quest.FISH_TRASH))
        assertEquals(0, prefs.getInt("dinseyFilthLevel"))
        assertTrue(consumed.contains(DinseyKioskChoiceSync.TRASH_NET to 1))
    }

    @Test
    fun kiosk_powerPhraseUsesGlobuleCount() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        DinseyKioskChoiceSync.apply(
            choiceId = DinseyKioskChoiceSync.KIOSK,
            decision = 1,
            html = "weren't kidding about the power",
            questDatabase = db,
            preferences = prefs,
            itemCount = { 19 },
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.GIVE_ME_FUEL))
        DinseyKioskChoiceSync.apply(
            choiceId = DinseyKioskChoiceSync.KIOSK,
            decision = 1,
            html = "weren't kidding about the power",
            questDatabase = db,
            preferences = prefs,
            itemCount = { 20 },
        )
        assertEquals("step1", db.getProgress(Quest.GIVE_ME_FUEL))
    }

    @Test
    fun rollercoaster_decision1AndLube() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setBoolean("dinseyRollercoasterNext", true)
        assertTrue(
            DinseyKioskChoiceSync.apply(
                choiceId = DinseyKioskChoiceSync.ROLLERCOASTER,
                decision = 1,
                html = "lubricating every inch of the tracks",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertFalse(prefs.getBoolean("dinseyRollercoasterNext"))
        assertEquals("step2", db.getProgress(Quest.SUPER_LUBER))
    }

    @Test
    fun questChoiceRules_wires1066() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = DinseyKioskChoiceSync.KIOSK,
                responseText = "anatomical diagram of a nasty bear",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.NASTY_BEARS))
    }
}
