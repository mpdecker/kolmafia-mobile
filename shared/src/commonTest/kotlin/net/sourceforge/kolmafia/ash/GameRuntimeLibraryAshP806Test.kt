package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DoctorBagChoiceSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP806Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesMalady() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            DoctorBagChoiceSync.applyVisit(
                choiceId = 1340,
                html = "We've received a report of a patient with an archaic cough, in The Haunted Pantry.",
                preferences = prefs,
            ),
        )
        assertEquals("antique bottle of cough syrup", prefs.getString("doctorBagQuestItem", ""))
        assertEquals("The Haunted Pantry", prefs.getString("doctorBagQuestLocation", ""))
    }

    @Test
    fun post_accept_startsQuest() {
        val prefs = Preferences(MapSettings())
        prefs.setString("doctorBagQuestItem", "cast")
        val quests = QuestDatabase(prefs)
        assertTrue(
            DoctorBagChoiceSync.applyAccept(
                choiceId = 1340,
                decision = 1,
                preferences = prefs,
                questDatabase = quests,
                itemCount = { 0 },
            ),
        )
        assertEquals(QuestDatabase.STARTED, quests.getProgress(Quest.DOCTOR_BAG))
    }

    @Test
    fun questChoiceRules_wiresAccept() {
        val prefs = Preferences(MapSettings())
        prefs.setString("doctorBagQuestItem", "cast")
        val quests = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1340,
                responseText = "",
                questDatabase = quests,
                decision = 1,
                preferences = prefs,
                itemCount = { 0 },
            ),
        )
        assertEquals(QuestDatabase.STARTED, quests.getProgress(Quest.DOCTOR_BAG))
    }

    @Test
    fun questChoiceRules_abandonStillWorks() {
        val prefs = Preferences(MapSettings())
        prefs.setString("doctorBagQuestItem", "cast")
        val quests = QuestDatabase(prefs)
        quests.setProgress(Quest.DOCTOR_BAG, QuestDatabase.STARTED)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1340,
                responseText = "",
                questDatabase = quests,
                decision = 2,
                preferences = prefs,
            ),
        )
        assertEquals(QuestDatabase.UNSTARTED, quests.getProgress(Quest.DOCTOR_BAG))
        assertEquals("", prefs.getString("doctorBagQuestItem", "x"))
    }
}
