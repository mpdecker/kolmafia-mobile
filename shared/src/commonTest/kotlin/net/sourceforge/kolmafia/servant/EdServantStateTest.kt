package net.sourceforge.kolmafia.servant

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.preferences.Preferences

class EdServantStateTest {

    private fun prefs(): Preferences = Preferences(MapSettings())

    @Test
    fun upsertAndGetRecord_roundTrips() {
        val prefs = prefs()
        val record = EdServantRecord("Cat", "Hethys", 14, 221)
        EdServantState.upsert(prefs, record)
        assertEquals(record, EdServantState.getRecord(prefs, "Cat"))
        assertEquals(record, EdServantState.getRecord(prefs, "cat"))
    }

    @Test
    fun getAllRecords_returnsMultipleServants() {
        val prefs = prefs()
        EdServantState.upsert(prefs, EdServantRecord("Cat", "Hethys", 14, 221))
        EdServantState.upsert(prefs, EdServantRecord("Maid", "Chambermaid", 5, 42))
        assertEquals(2, EdServantState.getAllRecords(prefs).size)
    }

    @Test
    fun addCombatExperience_incrementsXpAndLevelsUp() {
        val prefs = prefs()
        EdServantState.upsert(prefs, EdServantRecord("Cat", "Hethys", 1, 0))
        val afterOne = EdServantState.addCombatExperience(prefs, "Cat", crownOfEdEquipped = false)
        assertEquals(1, afterOne?.experience)
        assertEquals(1, afterOne?.level)

        EdServantState.upsert(prefs, EdServantRecord("Cat", "Hethys", 1, 3))
        val leveled = EdServantState.addCombatExperience(prefs, "Cat", crownOfEdEquipped = false)
        assertEquals(4, leveled?.experience)
        assertEquals(2, leveled?.level)
    }

    @Test
    fun addCombatExperience_crownOfEdDoublesDelta() {
        val prefs = prefs()
        EdServantState.upsert(prefs, EdServantRecord("Cat", "Hethys", 1, 0))
        val after = EdServantState.addCombatExperience(prefs, "Cat", crownOfEdEquipped = true)
        assertEquals(2, after?.experience)
    }

    @Test
    fun addCombatExperience_capsAt441() {
        val prefs = prefs()
        EdServantState.upsert(prefs, EdServantRecord("Cat", "Hethys", 21, 440))
        val after = EdServantState.addCombatExperience(prefs, "Cat", crownOfEdEquipped = false)
        assertEquals(441, after?.experience)
        assertEquals(21, after?.level)
    }

    @Test
    fun addCombatExperience_noActiveRecord_returnsNull() {
        val prefs = prefs()
        assertNull(EdServantState.addCombatExperience(prefs, "Cat", crownOfEdEquipped = false))
    }
}
