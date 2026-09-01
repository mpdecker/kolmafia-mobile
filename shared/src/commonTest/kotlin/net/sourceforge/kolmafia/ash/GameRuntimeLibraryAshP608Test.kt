package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.TrapperCabinSync

class GameRuntimeLibraryAshP608Test {

    @Test
    fun revision_phase608() {
        assertEquals("phase4370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun cabin_ore_setsTrapperOreAndStep1() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        assertTrue(
            TrapperCabinSync.applyFromVisit(
                url = "place.php?whichplace=mclargehuge&action=trappercabin",
                html = "Bring me 3 chunks of asbestos ore.",
                questDatabase = db,
                preferences = prefs,
            ),
        )
        assertEquals("asbestos ore", prefs.getString("trapperOre", ""))
        assertEquals("step1", db.getProgress(Quest.TRAPPER))
    }

    @Test
    fun cabin_turnIn_consumesOreAndCheese() {
        val prefs = Preferences(MapSettings())
        prefs.setString("trapperOre", "asbestos ore")
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            TrapperCabinSync.applyFromVisit(
                url = "place.php?whichplace=mclargehuge&action=trappercabin",
                html = "He takes the load of cheese and ore",
                questDatabase = db,
                preferences = prefs,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals("step2", db.getProgress(Quest.TRAPPER))
        assertEquals(
            listOf(TrapperCabinSync.ASBESTOS_ORE to 3, TrapperCabinSync.GOAT_CHEESE to 3),
            consumed,
        )
    }

    @Test
    fun cabin_groar_finishesQuest() {
        val prefs = Preferences(MapSettings())
        val db = QuestDatabase(prefs)
        val consumed = mutableListOf<Pair<Int, Int>>()
        assertTrue(
            TrapperCabinSync.applyFromVisit(
                url = "place.php?whichplace=mclargehuge&action=trappercabin",
                html = "Yeehaw!  I heard the noise and seen them mists",
                questDatabase = db,
                preferences = prefs,
                ascensionNumber = 12,
                consumeItem = { id, qty -> consumed += id to qty },
            ),
        )
        assertEquals(QuestDatabase.FINISHED, db.getProgress(Quest.TRAPPER))
        assertEquals(12, prefs.getInt("lastTr4pz0rQuest", -1))
        assertEquals(listOf(TrapperCabinSync.GROARS_FUR to 1), consumed)
    }
}
