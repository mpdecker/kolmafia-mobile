package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ColdMedicineChoiceSync
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP753Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase3830", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun visit_parsesConsultsAndEquipment() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            ColdMedicineChoiceSync.applyVisit(
                1455,
                "You have <b>3</b> consultations left. Grab the ice crown.",
                prefs,
            ),
        )
        assertEquals(
            ColdMedicineChoiceSync.CABINET_ITEM_ID,
            prefs.getInt(CampgroundItemSync.CURRENT_WORKSHED_ITEM_ID_PREF, 0),
        )
        assertEquals(2, prefs.getInt(ColdMedicineChoiceSync.CONSULTS_PREF, 0))
        assertEquals(0, prefs.getInt(ColdMedicineChoiceSync.EQUIPMENT_PREF, 0))
    }

    @Test
    fun post_incrementsConsultAndSchedulesNext() {
        val prefs = Preferences(MapSettings())
        assertTrue(ColdMedicineChoiceSync.apply(1455, 2, prefs, turnsPlayed = 100))
        assertEquals(1, prefs.getInt(ColdMedicineChoiceSync.CONSULTS_PREF, 0))
        assertEquals(120, prefs.getInt(ColdMedicineChoiceSync.NEXT_CONSULT_PREF, 0))
    }

    @Test
    fun post_equipmentDecisionCaps() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(ColdMedicineChoiceSync.EQUIPMENT_PREF, 1)
        assertTrue(ColdMedicineChoiceSync.apply(1455, 1, prefs, turnsPlayed = 10))
        assertEquals(2, prefs.getInt(ColdMedicineChoiceSync.EQUIPMENT_PREF, 0))
        assertTrue(ColdMedicineChoiceSync.apply(1455, 1, prefs, turnsPlayed = 10))
        assertEquals(2, prefs.getInt(ColdMedicineChoiceSync.EQUIPMENT_PREF, 0))
    }

    @Test
    fun post_leaveIgnored() {
        assertFalse(ColdMedicineChoiceSync.apply(1455, 6, Preferences(MapSettings()), 0))
    }

    @Test
    fun questChoiceRules_wires1455() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestChoiceRules.apply(
                choiceId = 1455,
                responseText = "",
                questDatabase = db,
                decision = 3,
                preferences = prefs,
                turnsPlayed = 50,
            ),
        )
        assertEquals(70, prefs.getInt(ColdMedicineChoiceSync.NEXT_CONSULT_PREF, 0))
    }
}
