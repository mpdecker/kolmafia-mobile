package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DailyDungeonCombatSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules

class GameRuntimeLibraryAshP639Test {

    @Test
    fun revision_phase635() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun dailyDungeonWin_incrementsRoom() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(DailyDungeonCombatSync.PREF, 2)
        assertTrue(DailyDungeonCombatSync.apply("325", prefs))
        assertEquals(3, prefs.getInt(DailyDungeonCombatSync.PREF))
    }

    @Test
    fun otherLocation_isNoOp() {
        val prefs = Preferences(MapSettings())
        assertFalse(DailyDungeonCombatSync.apply("455", prefs))
        assertEquals(0, prefs.getInt(DailyDungeonCombatSync.PREF, 0))
    }

    @Test
    fun applyCombatWin_wiresBlankMonster() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                monster = "",
                won = true,
                preferences = prefs,
                adventureId = "325",
            ).advanced,
        )
        assertEquals(1, prefs.getInt(DailyDungeonCombatSync.PREF))
    }

    @Test
    fun loss_doesNotIncrement() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            QuestFightRules.applyCombat(
                db,
                monster = "skeleton",
                won = false,
                preferences = prefs,
                adventureId = "325",
            ).advanced,
        )
        assertEquals(0, prefs.getInt(DailyDungeonCombatSync.PREF, 0))
    }
}
