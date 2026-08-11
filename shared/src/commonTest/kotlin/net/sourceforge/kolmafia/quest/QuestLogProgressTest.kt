package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.data.QuestLogEntry
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class QuestLogProgressTest {

    private fun entry(vararg steps: Pair<String, String>) = QuestLogEntry(
        prefKey = "questTest",
        title = "Test Quest",
        steps = steps.toList(),
    )

    @Test
    fun findQuestProgress_telegramSection_setsPrefsAndStep() {
        val prefs = Preferences(MapSettings())
        val step = QuestLogProgress.findQuestProgress(
            Quest.TELEGRAM.prefKey,
            "Ask around the Rough Diamond Saloon to see if anybody has seen Jeff.",
            entry("started" to "unused"),
            prefs,
            null,
        )
        assertEquals("step1", step)
        assertEquals("Missing: Fancy Man", prefs.getString("lttQuestName", ""))
        assertEquals(1, prefs.getInt("lttQuestDifficulty", 0))
    }

    @Test
    fun findQuestProgress_partyFairSection_setsPrefsAndStep() {
        val prefs = Preferences(MapSettings())
        val step = QuestLogProgress.findQuestProgress(
            Quest.PARTY_FAIR.prefKey,
            "Clean up the trash: Trash left: ~42 pieces.",
            entry("started" to "unused"),
            prefs,
            null,
        )
        assertEquals("step1", step)
        assertEquals("trash", prefs.getString("_questPartyFairQuest", ""))
        assertEquals("42", prefs.getString("_questPartyFairProgress", ""))
    }

    @Test
    fun findQuestProgress_doctorBagDeliveryStep_setsPrefs() {
        val prefs = Preferences(MapSettings())
        val step = QuestLogProgress.findQuestProgress(
            Quest.DOCTOR_BAG.prefKey,
            "Take a scalpel to the patient in <a><b>Distant Woods</b></a>.",
            entry("started" to "unused"),
            prefs,
            null,
        )
        assertEquals("step1", step)
        assertEquals("scalpel", prefs.getString("doctorBagQuestItem", ""))
        assertEquals("Distant Woods", prefs.getString("doctorBagQuestLocation", ""))
    }

    @Test
    fun findQuestProgress_pirateRealmSkipped() {
        val step = QuestLogProgress.findQuestProgress(
            Quest.PIRATEREALM.prefKey,
            "Sail around the islands.",
            entry("started" to "go sailing", "finished" to "done"),
            null,
            null,
        )
        assertNull(step)
    }

    @Test
    fun findQuestProgress_genericQuest_usesDetectStep() {
        val e = entry(
            "started" to "go find the larva",
            "step1" to "return to the council",
            "finished" to "you found it",
        )
        val step = QuestLogProgress.findQuestProgress(
            Quest.LARVA.prefKey,
            "Return to the council with the larva.",
            e,
            null,
            null,
        )
        assertEquals("step1", step)
    }

    @Test
    fun findQuestProgress_unparseableGeneric_returnsNull() {
        val e = entry("started" to "go find", "finished" to "you found")
        val step = QuestLogProgress.findQuestProgress(
            Quest.LARVA.prefKey,
            "Something completely unrelated.",
            e,
            null,
            null,
        )
        assertNull(step)
    }
}
