package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.MerkinColosseumCombatSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules

class GameRuntimeLibraryAshP652Test {

    @Test
    fun revision_phase647() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun colosseumWin_incrementsRound() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastColosseumRoundWon", 3)
        assertTrue(
            MerkinColosseumCombatSync.apply("210", "Mer-kin balldodger", prefs),
        )
        assertEquals(4, prefs.getInt("lastColosseumRoundWon"))
        assertFalse(prefs.getBoolean("isMerkinGladiatorChampion", false))
    }

    @Test
    fun roundFifteen_setsGladiatorPath() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastColosseumRoundWon", 14)
        assertTrue(
            MerkinColosseumCombatSync.apply("210", "Georgepaul, the Balldodger", prefs),
        )
        assertEquals(15, prefs.getInt("lastColosseumRoundWon"))
        assertTrue(prefs.getBoolean("isMerkinGladiatorChampion"))
        assertEquals("gladiator", prefs.getString("merkinQuestPath"))
    }

    @Test
    fun wanderingMonster_doesNotIncrement() {
        val prefs = Preferences(MapSettings())
        assertFalse(MerkinColosseumCombatSync.apply("210", "skeletal sommelier", prefs))
        assertEquals(0, prefs.getInt("lastColosseumRoundWon", 0))
    }

    @Test
    fun otherLocation_isNoOp() {
        val prefs = Preferences(MapSettings())
        assertFalse(MerkinColosseumCombatSync.apply("388", "Mer-kin balldodger", prefs))
        assertEquals(0, prefs.getInt("lastColosseumRoundWon", 0))
    }

    @Test
    fun applyCombatWin_wiresColosseum() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                monster = "Mer-kin netdragger",
                won = true,
                preferences = prefs,
                adventureId = "210",
            ).advanced,
        )
        assertEquals(1, prefs.getInt("lastColosseumRoundWon"))
    }

    @Test
    fun loss_doesNotIncrement() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(
            QuestFightRules.applyCombat(
                db,
                monster = "Mer-kin bladeswitcher",
                won = false,
                preferences = prefs,
                adventureId = "210",
            ).advanced,
        )
        assertEquals(0, prefs.getInt("lastColosseumRoundWon", 0))
    }
}
