package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.HiddenCityCombatSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP579Test {

    @Test
    fun aps_apartment_setsProgress6AndCursesStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            HiddenCityCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                adventureId = "341",
                monster = "Ancient Protector Spirit",
                responseText = "You win.",
                won = true,
            ),
        )
        assertEquals(6, prefs.getInt("hiddenApartmentProgress", 0))
        assertEquals("step1", db.getProgress(Quest.CURSES))
    }

    @Test
    fun aps_hospital_setsDoctorStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            HiddenCityCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                adventureId = "342",
                monster = "Ancient Protector Spirit",
                responseText = "You win.",
                won = true,
            ),
        )
        assertEquals(6, prefs.getInt("hiddenHospitalProgress", 0))
        assertEquals("step1", db.getProgress(Quest.DOCTOR))
    }

    @Test
    fun park_janitor_setsRelocatePref() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            HiddenCityCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                adventureId = "345",
                monster = "pygmy janitor",
                responseText = "You win.",
                won = true,
                ascensionNumber = 4,
            ),
        )
        assertEquals(4, prefs.getInt("relocatePygmyJanitor", -1))
    }

    @Test
    fun accountant_unlocksOfficeProgress6() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            HiddenCityCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                adventureId = "343",
                monster = "pygmy witch accountant",
                responseText = "You win without a file drop.",
                won = true,
                itemCount = { 0 },
            ),
        )
        assertEquals(6, prefs.getInt("hiddenOfficeProgress", 0))
    }
}
