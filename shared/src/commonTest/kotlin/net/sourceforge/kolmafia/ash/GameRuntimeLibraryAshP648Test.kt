package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.ManorDrawerCombatSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules

class GameRuntimeLibraryAshP648Test {

    @Test
    fun revision_phase647() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun drawerHtml_incrementsByCapture() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(ManorDrawerCombatSync.PREF, 3)
        assertTrue(
            ManorDrawerCombatSync.apply(
                "388",
                "You search through <b>5</b> drawers",
                prefs,
            ),
        )
        assertEquals(8, prefs.getInt(ManorDrawerCombatSync.PREF))
    }

    @Test
    fun withoutDrawerHtml_incrementsByOne() {
        val prefs = Preferences(MapSettings())
        assertTrue(ManorDrawerCombatSync.apply("388", "You win the fight", prefs))
        assertEquals(1, prefs.getInt(ManorDrawerCombatSync.PREF))
    }

    @Test
    fun billiardsKey_isNoOp() {
        val prefs = Preferences(MapSettings())
        assertFalse(
            ManorDrawerCombatSync.apply(
                "388",
                "You search through <b>5</b> drawers",
                prefs,
                hasItemId = { it == ManorDrawerCombatSync.BILLIARDS_KEY },
            ),
        )
        assertEquals(0, prefs.getInt(ManorDrawerCombatSync.PREF, 0))
    }

    @Test
    fun otherLocation_isNoOp() {
        val prefs = Preferences(MapSettings())
        assertFalse(ManorDrawerCombatSync.apply("417", "You win the fight", prefs))
        assertEquals(0, prefs.getInt(ManorDrawerCombatSync.PREF, 0))
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
                adventureId = "388",
                responseText = "You search through <b>2</b> drawers",
            ).advanced,
        )
        assertEquals(2, prefs.getInt(ManorDrawerCombatSync.PREF))
    }

    @Test
    fun applyCombatWin_skipsWhenKeyOwned() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            QuestFightRules.applyCombat(
                db,
                monster = "",
                won = true,
                preferences = prefs,
                adventureId = "388",
                responseText = "You search through <b>2</b> drawers",
                hasItemId = { it == ManorDrawerCombatSync.BILLIARDS_KEY },
            ).advanced,
        )
        assertEquals(0, prefs.getInt(ManorDrawerCombatSync.PREF, 0))
    }
}
