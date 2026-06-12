package net.sourceforge.kolmafia.quest

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuestFightRulesTest {

    @Test
    fun applyCombat_lossAdvancesToStep18() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step17")
        assertTrue(QuestFightRules.applyCombat(db, "menacing thug", won = false))
        assertEquals("step18", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyCombat_winAdvancesToStep19() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step18")
        assertTrue(QuestFightRules.applyCombat(db, "menacing thug", won = true))
        assertEquals("step19", db.getProgress(Quest.NEMESIS))
    }

    @Test
    fun applyCombat_volcanoMapFinishesStep25() {
        val db = QuestDatabase(Preferences(MapSettings()))
        db.setProgress(Quest.NEMESIS, "step24")
        assertTrue(
            QuestFightRules.applyCombat(
                db, "", won = true, itemsGained = listOf("volcano map"),
            )
        )
        assertEquals("step25", db.getProgress(Quest.NEMESIS))
    }
}
