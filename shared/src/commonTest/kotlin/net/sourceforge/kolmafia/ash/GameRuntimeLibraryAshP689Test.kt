package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.FightItemPrefSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.QuestFightRules

class GameRuntimeLibraryAshP689Test {

    @Test
    fun revision_phase689() {
        assertEquals("phase4310", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun mayoLance_reducesMayoLevel() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("mayoLevel", 50)
        assertTrue(
            FightItemPrefSync.apply(
                html = "Everything Looks Yellow",
                monster = "Knob Goblin Chef",
                preferences = prefs,
                combatItemId = FightItemPrefSync.MAYO_LANCE,
            ),
        )
        assertEquals(20, prefs.getInt("mayoLevel"))
    }

    @Test
    fun beehive_consumesOnWall() {
        val prefs = Preferences(MapSettings())
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            FightItemPrefSync.apply(
                html = "The entire wall fattens as bees pour out.",
                monster = "bee",
                preferences = prefs,
                combatItemId = FightItemPrefSync.BEEHIVE,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(consumed.contains(FightItemPrefSync.BEEHIVE to 1))
    }

    @Test
    fun boningKnife_consumesOnBurnout() {
        val prefs = Preferences(MapSettings())
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            FightItemPrefSync.apply(
                html = "The knife's motor burns out.",
                monster = "skeleton",
                preferences = prefs,
                combatItemId = FightItemPrefSync.ELECTRIC_BONING_KNIFE,
                consumeItem = { id, qty -> consumed.add(id to qty) },
            ),
        )
        assertTrue(consumed.contains(FightItemPrefSync.ELECTRIC_BONING_KNIFE to 1))
    }

    @Test
    fun blankOut_incrementsWithoutItemId() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("blankOutUsed", 1)
        assertTrue(
            FightItemPrefSync.apply(
                html = "You smear part of your handful of Blank-Out on the monster",
                monster = "spooky mummy",
                preferences = prefs,
            ),
        )
        assertEquals(2, prefs.getInt("blankOutUsed"))
    }

    @Test
    fun questFightRules_wiresMayoLance() {
        val prefs = Preferences(MapSettings())
        prefs.setInt("mayoLevel", 30)
        val db = QuestDatabase(prefs)
        assertTrue(
            QuestFightRules.applyCombat(
                db,
                "Knob Goblin Chef",
                won = false,
                preferences = prefs,
                responseText = "Everything Looks Yellow",
                combatItemId = FightItemPrefSync.MAYO_LANCE,
            ).advanced,
        )
        assertEquals(0, prefs.getInt("mayoLevel"))
    }
}
