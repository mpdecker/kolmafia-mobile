package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestCombatWinExtrasSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules

class GameRuntimeLibraryAshP638Test {

    @Test
    fun revision_phase635() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun superconductor_setsDefeated() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(QuestCombatWinExtrasSync.apply("The Superconductor", db, prefs))
        assertTrue(prefs.getBoolean("superconductorDefeated"))
    }

    @Test
    fun applyCombatWin_wiresSuperconductor() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                "The Superconductor",
                won = true,
                preferences = prefs,
            ).advanced,
        )
        assertTrue(prefs.getBoolean("superconductorDefeated"))
    }

    @Test
    fun otherMonster_doesNotSetDefeated() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertFalse(QuestCombatWinExtrasSync.apply("oil slick", db, prefs))
        assertFalse(prefs.getBoolean("superconductorDefeated", false))
    }
}
