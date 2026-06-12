package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestChoiceRulesTest {

    @Test
    fun apply_choice1088_boomAdvancesStep15() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step14")
        assertTrue(QuestChoiceRules.apply(1088, "BOOOOOOM! The rubble is gone.", db))
        assertEquals("step15", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun apply_choice930_startsCitadel() {
        val db = QuestDatabase(Preferences(MapSettings()))
        assertTrue(QuestChoiceRules.apply(930, "Welcome to the White Citadel quest.", db))
        assertEquals(QuestDatabase.STARTED, db.getProgress(Quest.CITADEL))
    }
}
