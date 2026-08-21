package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.AirportCombatSync
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP634Test {

    @Test
    fun revision_phase635() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun warehouseGuard_increments() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("warehouseProgress", 4)
        val db = QuestDatabase(prefs)
        assertTrue(AirportCombatSync.apply("warehouse guard", "", db, prefs))
        assertEquals(5, prefs.getInt("warehouseProgress"))
    }

    @Test
    fun warehouseJanitor_increments() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(AirportCombatSync.apply("warehouse janitor", "", db, prefs))
        assertEquals(1, prefs.getInt("warehouseProgress"))
    }

    @Test
    fun warehouseClerk_increments() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("warehouseProgress", 11)
        val db = QuestDatabase(prefs)
        assertTrue(AirportCombatSync.apply("Warehouse Clerk", "", db, prefs))
        assertEquals(12, prefs.getInt("warehouseProgress"))
    }

    @Test
    fun unknownMonster_isNoOp() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(AirportCombatSync.apply("knob goblin", "", db, prefs))
        assertEquals(0, prefs.getInt("warehouseProgress", 0))
    }
}
