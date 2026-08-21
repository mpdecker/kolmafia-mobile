package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules
import net.sourceforge.kolmafia.quest.SmutOrcCombatSync

class GameRuntimeLibraryAshP636Test {

    @Test
    fun revision_phase635() {
        assertEquals("phase1070", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun shiver_incrementsByOne() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(SmutOrcCombatSync.PREF, 4)
        assertTrue(
            SmutOrcCombatSync.apply(
                "smut orc jacker",
                "Out of the corner of your eye, you see another smut orc shiver and disappear",
                prefs,
            ),
        )
        assertEquals(5, prefs.getInt(SmutOrcCombatSync.PREF))
    }

    @Test
    fun twoOrcs_incrementsByTwo() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            SmutOrcCombatSync.apply(
                "smut orc nailer",
                "you see two smut orcs give one another a meaningful glance",
                prefs,
            ),
        )
        assertEquals(2, prefs.getInt(SmutOrcCombatSync.PREF))
    }

    @Test
    fun huddling_incrementsByThree() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            SmutOrcCombatSync.apply(
                "smut orc pipelayer",
                "You see a nearby group of smut orcs huddling together for warmth",
                prefs,
            ),
        )
        assertEquals(3, prefs.getInt(SmutOrcCombatSync.PREF))
    }

    @Test
    fun windows_incrementsByFour() {
        val prefs = Preferences(MapSettings())
        assertTrue(
            SmutOrcCombatSync.apply(
                "smut orc screwer",
                "You hear a bunch of windows being slammed shut against the cold",
                prefs,
            ),
        )
        assertEquals(4, prefs.getInt(SmutOrcCombatSync.PREF))
    }

    @Test
    fun dozens_incrementsByFiveAndCapsAtFifteen() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(SmutOrcCombatSync.PREF, 12)
        assertTrue(
            SmutOrcCombatSync.apply(
                "Smut Orc Jacker",
                "Dozens of nearby smut orcs, shivering in the cold, rush into their shacks",
                prefs,
            ),
        )
        assertEquals(15, prefs.getInt(SmutOrcCombatSync.PREF))
    }

    @Test
    fun unmatchedHtml_isNoOp() {
        val prefs = Preferences(MapSettings())
        assertFalse(SmutOrcCombatSync.apply("smut orc jacker", "You win the fight", prefs))
        assertEquals(0, prefs.getInt(SmutOrcCombatSync.PREF, 0))
    }

    @Test
    fun applyCombatWin_wiresSmutOrc() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                "smut orc jacker",
                won = true,
                preferences = prefs,
                responseText = "another smut orc shiver",
            ).advanced,
        )
        assertEquals(1, prefs.getInt(SmutOrcCombatSync.PREF))
    }
}
