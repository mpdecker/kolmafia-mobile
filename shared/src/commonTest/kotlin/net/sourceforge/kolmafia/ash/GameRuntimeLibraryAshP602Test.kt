package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FantasyRealmCombatSync

class GameRuntimeLibraryAshP602Test {

    @Test
    fun firstKill_appends() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            FantasyRealmCombatSync.applyCombatWin("swamp troll", "505", prefs, won = true),
        )
        assertEquals("swamp troll:1,", prefs.getString("_frMonstersKilled", ""))
    }

    @Test
    fun secondKill_increments() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_frMonstersKilled", "swamp troll:1,")
        assertTrue(
            FantasyRealmCombatSync.applyCombatWin("swamp troll", "505", prefs, won = true),
        )
        assertEquals("swamp troll:2,", prefs.getString("_frMonstersKilled", ""))
    }

    @Test
    fun phoenix_incrementsQuotedName() {
        val prefs = Preferences(MapSettings())
        prefs.setString("_frMonstersKilled", "\"Phoenix\":1,")
        FantasyRealmCombatSync.addFantasyRealmKill("\"Phoenix\"", prefs)
        assertEquals("\"Phoenix\":2,", prefs.getString("_frMonstersKilled", ""))
    }

    @Test
    fun spookyGhost_skipsDreadVillage() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            FantasyRealmCombatSync.applyCombatWin(
                "spooky ghost",
                FantasyRealmCombatSync.DREAD_VILLAGE.toString(),
                prefs,
                won = true,
            ),
        )
        assertTrue(
            FantasyRealmCombatSync.applyCombatWin("spooky ghost", "518", prefs, won = true),
        )
        assertEquals("spooky ghost:1,", prefs.getString("_frMonstersKilled", ""))
    }
}
