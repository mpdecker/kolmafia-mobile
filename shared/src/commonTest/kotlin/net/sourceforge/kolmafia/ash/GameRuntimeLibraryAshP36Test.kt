package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.CombatDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.PoisonLevels
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.AdventureSpentTracker

class GameRuntimeLibraryAshP36Test {

    @Test
    fun locationBracketTurnsSpent_readsTracker() = runBlocking {
        val prefs = Preferences(MapSettings())
        val tracker = AdventureSpentTracker(prefs)
        tracker.addTurn("The Haunted Pantry")
        tracker.addTurn("The Haunted Pantry")
        tracker.addTurn("The Haunted Pantry")
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            gameDatabase = db,
            adventureSpentTracker = tracker,
        )
        assertEquals("3", outputLib(lib, """print(to_location("The Haunted Pantry")["turns_spent"]);""").trim())
        assertEquals("3", outputLib(lib, """print(my_total_turns_spent());""").trim())
    }

    @Test
    fun locationBracketLastNoncombat_readsPreference() = runBlocking {
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastNoncombat15", 4)
        val tracker = AdventureSpentTracker(prefs)
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            gameDatabase = db,
            adventureSpentTracker = tracker,
        )
        assertEquals("4", outputLib(lib, """print(to_location("The Spooky Forest")["last_noncombat_turns_spent"]);""").trim())
    }

    @Test
    fun locationBracketLastNoncombat_returnsNegativeOneWithoutForceNc() = runBlocking {
        val prefs = Preferences(MapSettings())
        val tracker = AdventureSpentTracker(prefs)
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            gameDatabase = db,
            adventureSpentTracker = tracker,
        )
        assertEquals("-1", outputLib(lib, """print(to_location("The Haunted Pantry")["last_noncombat_turns_spent"]);""").trim())
    }

    @Test
    fun turnsUntilForcedNoncombat_computesFromTrackerAndPrefs() = runBlocking {
        val prefs = Preferences(MapSettings())
        prefs.setInt("lastNoncombat15", 3)
        val tracker = AdventureSpentTracker(prefs)
        repeat(10) { tracker.addTurn("The Spooky Forest") }
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(
            preferences = prefs,
            gameDatabase = db,
            adventureSpentTracker = tracker,
        )
        assertEquals(
            "1",
            outputLib(lib, """print(turns_until_forced_noncombat(to_location("The Spooky Forest")));""").trim(),
        )
    }

    @Test
    fun locationBracketPoison_readsBundledMonsterData() = runBlocking {
        val db = GameDatabase()
        db.load()
        MonsterDatabase.load()
        CombatDatabase.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val poison = outputLib(lib, """print(to_location("The Sleazy Back Alley")["poison"]);""").trim().toLong()
        assertTrue(poison < Int.MAX_VALUE.toLong())
        assertEquals(6L, poison)
    }

    @Test
    fun monsterDatabase_parsesPoisonField() = runBlocking {
        MonsterDatabase.load()
        val spider = MonsterDatabase.getByName("big creepy spider")
        assertEquals(6, spider?.poison)
    }

    @Test
    fun poisonLevels_mapsEffectNames() {
        assertEquals(2, PoisonLevels.levelForEffectName("Majorly Poisoned"))
        assertEquals(6, PoisonLevels.levelForEffectName("Hardly Poisoned at All"))
        assertEquals(1, PoisonLevels.levelForEffectName("Toad In The Hole"))
    }

    @Test
    fun adventureSpentTracker_persistsAcrossLoad() {
        val settings = MapSettings()
        val prefs1 = Preferences(settings)
        val tracker1 = AdventureSpentTracker(prefs1)
        tracker1.addTurn("The Haunted Pantry")
        tracker1.addTurn("The Spooky Forest")

        val prefs2 = Preferences(settings)
        val tracker2 = AdventureSpentTracker(prefs2)
        tracker2.load()
        assertEquals(1, tracker2.getTurns("The Haunted Pantry"))
        assertEquals(1, tracker2.getTurns("The Spooky Forest"))
        assertEquals(2, tracker2.getTotalTrackedTurns())
    }
}
