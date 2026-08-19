package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules
import net.sourceforge.kolmafia.quest.QuestLocationCombatSync

class GameRuntimeLibraryAshP641Test {

    @Test
    fun revision_phase635() {
        assertEquals("phase671", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun bitRealm_incrementsBonusTurns() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("8BitBonusTurns", 3)
        assertTrue(QuestLocationCombatSync.apply("563", "some monster", prefs))
        assertEquals(4, prefs.getInt("8BitBonusTurns"))
        assertTrue(QuestLocationCombatSync.apply("566", "", prefs))
        assertEquals(5, prefs.getInt("8BitBonusTurns"))
    }

    @Test
    fun villainousMinion_incrementsProgress() {
        val prefs = Preferences(MapSettings())
        assertTrue(QuestLocationCombatSync.apply("495", "Villainous Minion", prefs))
        assertEquals(1, prefs.getInt("_villainLairProgress"))
        assertEquals(0, prefs.getInt("bondVillainsDefeated", 0))
    }

    @Test
    fun villainousVillain_sets999AndIncrementsDefeated() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("bondVillainsDefeated", 2)
        assertTrue(QuestLocationCombatSync.apply("495", "Villainous Villain", prefs))
        assertEquals(999, prefs.getInt("_villainLairProgress"))
        assertEquals(3, prefs.getInt("bondVillainsDefeated"))
    }

    @Test
    fun applyCombatWin_wiresBitRealmBlankMonster() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                monster = "",
                won = true,
                preferences = prefs,
                adventureId = "564",
            ).advanced,
        )
        assertEquals(1, prefs.getInt("8BitBonusTurns"))
    }

    @Test
    fun applyCombatWin_wiresVillain() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                "Villainous Minion",
                won = true,
                preferences = prefs,
                adventureId = "495",
            ).advanced,
        )
        assertEquals(1, prefs.getInt("_villainLairProgress"))
    }

    @Test
    fun otherLocation_isNoOp() {
        val prefs = Preferences(MapSettings())
        assertFalse(QuestLocationCombatSync.apply("325", "Villainous Minion", prefs))
        assertEquals(0, prefs.getInt("_villainLairProgress", 0))
        assertEquals(0, prefs.getInt("8BitBonusTurns", 0))
    }
}
