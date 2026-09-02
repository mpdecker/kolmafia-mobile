package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DesertCombatSync
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

class GameRuntimeLibraryAshP570Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase4430", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun clingyMonster_noExplorationIncrement() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            DesertCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                adventureId = "364",
                responseText = "You win the fight. No clingy marker.",
                won = true,
            ),
        )
        assertEquals(0, prefs.getInt("desertExploration", 0))
    }

    @Test
    fun baseExploration_plusCompass() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            DesertCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                adventureId = "364",
                responseText = "Desert exploration continues.",
                won = true,
                context = DesertCombatSync.DesertCombatContext(
                    hasEquipped = { it == DesertCombatSync.UV_RESISTANT_COMPASS },
                ),
            ),
        )
        assertEquals(2, prefs.getInt("desertExploration", 0))
    }

    @Test
    fun oasisUnlock_setsPrefWithoutExtraKnifeBonus() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            DesertCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                adventureId = "364",
                responseText = "Desert exploration. You discover a verdant oasis.",
                won = true,
                context = DesertCombatSync.DesertCombatContext(
                    hasEquipped = { it == DesertCombatSync.SURVIVAL_KNIFE },
                    hasEffect = { it == DesertCombatSync.ULTRAHYDRATED },
                ),
            ),
        )
        assertTrue(prefs.getBoolean("oasisAvailable", false))
        assertEquals(1, prefs.getInt("desertExploration", 0))
    }

    @Test
    fun melodramedaryAndBond_stack() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("desertExploration", 10)
        prefs.setBoolean("bondDesert", true)
        val db = QuestDatabase(prefs)
        assertTrue(
            DesertCombatSync.applyCombatWin(
                questDatabase = db,
                preferences = prefs,
                adventureId = "364",
                responseText = "Desert exploration.",
                won = true,
                context = DesertCombatSync.DesertCombatContext(
                    familiarId = DesertCombatSync.MELODRAMEDARY,
                ),
            ),
        )
        // base 1 + melodrama 1 + bond 2 = 4 → 14
        assertEquals(14, prefs.getInt("desertExploration", 0))
    }
}
