package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.PartyFairChoiceSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP688Test {

    @Test
    fun revision_phase689() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun beginning_trashInvokesQuestLogCallback() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setString("_questPartyFairQuest", "trash")
        var synced = false
        assertTrue(
            PartyFairChoiceSync.apply(
                choiceId = PartyFairChoiceSync.BEGINNING,
                decision = 1,
                html = "",
                questDatabase = db,
                preferences = prefs,
                resyncQuestLogPage1 = { synced = true },
            ),
        )
        assertTrue(synced)
        assertEquals("step1", db.getProgress(Quest.PARTY_FAIR))
    }

    @Test
    fun beginning_wootsDoesNotResync() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setString("_questPartyFairQuest", "woots")
        var synced = false
        PartyFairChoiceSync.apply(
            choiceId = PartyFairChoiceSync.BEGINNING,
            decision = 1,
            html = "",
            questDatabase = db,
            preferences = prefs,
            resyncQuestLogPage1 = { synced = true },
        )
        assertFalse(synced)
        assertEquals(10, prefs.getInt("_questPartyFairProgress"))
    }

    @Test
    fun questChoiceRules_wiresTrashResync() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        prefs.setString("_questPartyFairQuest", "trash")
        var synced = false
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = PartyFairChoiceSync.BEGINNING,
                responseText = "",
                questDatabase = db,
                decision = 1,
                preferences = prefs,
                resyncQuestLogPage1 = { synced = true },
            ),
        )
        assertTrue(synced)
    }
}
