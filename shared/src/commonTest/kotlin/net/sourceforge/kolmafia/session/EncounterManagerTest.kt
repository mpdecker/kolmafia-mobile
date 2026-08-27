package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.EncounterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class EncounterManagerTest {
    private lateinit var prefs: Preferences

    @BeforeTest
    fun setUp() {
        EncounterManager.resetForTest()
        prefs = Preferences(MapSettings())
    }

    @Test
    fun loadAndFindAutostopEncounter() = runBlocking {
        EncounterDatabase.load()
        val enc = EncounterManager.findEncounter("History is Fun!")
        assertNotNull(enc)
        assertEquals(EncounterType.STOP, enc!!.encounterType)
        assertTrue(EncounterManager.isAutoStop("History is Fun!"))
    }

    @Test
    fun underTheKnifeSkipWhenChoice2() = runBlocking {
        EncounterDatabase.load()
        prefs.setString("choiceAdventure21", "2")
        assertFalse(EncounterManager.isAutoStop("Under the Knife", prefs))
        prefs.setString("choiceAdventure21", "1")
        assertTrue(EncounterManager.isAutoStop("Under the Knife", prefs))
    }

    @Test
    fun registerAdventureIncrements() {
        EncounterManager.registerAdventure("The Spooky Forest")
        EncounterManager.registerAdventure("The Spooky Forest")
        EncounterManager.registerAdventure("The Haunted Pantry")
        val list = EncounterManager.adventureListSnapshot()
        assertEquals(2, list.size)
        assertEquals(2, list[0].count)
        assertEquals("The Haunted Pantry", list[1].name)
    }

    @Test
    fun registerEncounterAutostopPending() = runBlocking {
        EncounterDatabase.load()
        EncounterManager.registerEncounter(
            encounterName = "History is Fun!",
            encounterTypeLabel = "Noncombat",
            responseText = "<html>History is Fun!</html>",
            preferences = prefs,
            locationName = "The Haunted Library",
        )
        assertEquals("History is Fun!", EncounterManager.pendingAutoStop)
        val enc = EncounterManager.encounterListSnapshot().single()
        assertEquals("History is Fun!", enc.name)
    }

    @Test
    fun specialEncounterDailyDungeon() {
        EncounterManager.handleSpecialEncounter("Daily Done, John.", "", prefs)
        assertTrue(prefs.getBoolean("dailyDungeonDone"))
        assertEquals(15, prefs.getInt("_lastDailyDungeonRoom", 0))
    }

    @Test
    fun romanticAndDigitizeDetectors() {
        assertTrue(
            EncounterManager.isRomanticEncounter("You hear a wolf whistle from behind", false, prefs),
        )
        assertTrue(
            EncounterManager.isDigitizedEncounter("must have hit CTRL+V somehow", false, prefs),
        )
        assertTrue(
            EncounterManager.isGregariousEncounter("Looks like it's that friend you gregariously made"),
        )
        EncounterManager.noteFightSpecials("must have hit CTRL+V", prefs)
        assertTrue(EncounterManager.ignoreSpecialMonsters)
    }

    @Test
    fun saberForceMonster() {
        prefs.setInt("_saberForceMonsterCount", 2)
        prefs.setString("_saberForceMonster", "fancy bath slug")
        assertTrue(EncounterManager.isSaberForceMonster("fancy bath slug", prefs))
        assertFalse(EncounterManager.isSaberForceMonster("crate", prefs))
    }
}
