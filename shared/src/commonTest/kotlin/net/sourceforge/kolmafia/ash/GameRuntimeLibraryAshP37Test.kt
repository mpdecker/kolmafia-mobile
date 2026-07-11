package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.DreadKissesTracker
import net.sourceforge.kolmafia.session.WildfireCampManager

class GameRuntimeLibraryAshP37Test {

    @Test
    fun adventureDatabase_computeWaterLevel_heuristics() {
        assertEquals(1, AdventureDatabase.computeWaterLevel("outdoor", 0, null))
        assertEquals(3, AdventureDatabase.computeWaterLevel("indoor", 0, null))
        assertEquals(5, AdventureDatabase.computeWaterLevel("underground", 0, null))
        assertEquals(4, AdventureDatabase.computeWaterLevel("indoor", 40, null))
        assertEquals(0, AdventureDatabase.computeWaterLevel("underwater", 50, null))
        assertEquals(6, AdventureDatabase.computeWaterLevel("none", 0, 6))
    }

    @Test
    fun locationBracketWaterLevel_raincoreReadsZoneLevel() = runBlocking {
        val db = GameDatabase()
        db.load()
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = AscensionPath.HEAVY_RAINS.apiName))
        }
        val lib = GameRuntimeLibrary(
            character = char,
            gameDatabase = db,
        )
        assertEquals(
            "4",
            outputLib(lib, """print(to_location("Dreadsylvanian Village")["water_level"]);""").trim(),
        )
    }

    @Test
    fun locationBracketWaterLevel_nonRaincoreReturnsZero() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "0",
            outputLib(lib, """print(to_location("Dreadsylvanian Village")["water_level"]);""").trim(),
        )
    }

    @Test
    fun locationBracketFireLevel_wildfireReadsCaptainMap() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        val wildfire = WildfireCampManager(prefs)
        wildfire.setFireLevelForTest("Dreadsylvanian Woods", 3)
        val char = KoLCharacter().also {
            it.updateFromApiResponse(CharacterApiResponse(path = AscensionPath.WILDFIRE.apiName))
        }
        val lib = GameRuntimeLibrary(
            character = char,
            gameDatabase = db,
            preferences = prefs,
            wildfireCampManager = wildfire,
        )
        assertEquals(
            "3",
            outputLib(lib, """print(to_location("Dreadsylvanian Woods")["fire_level"]);""").trim(),
        )
    }

    @Test
    fun locationBracketFireLevel_nonWildfireReturnsZero() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        val wildfire = WildfireCampManager(prefs)
        wildfire.setFireLevelForTest("Dreadsylvanian Woods", 3)
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            preferences = prefs,
            wildfireCampManager = wildfire,
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_location("Dreadsylvanian Woods")["fire_level"]);""").trim(),
        )
    }

    @Test
    fun locationBracketKisses_readsDreadTracker() = runBlocking {
        val db = GameDatabase()
        db.load()
        val prefs = Preferences(MapSettings())
        val kisses = DreadKissesTracker(prefs)
        kisses.setKissesForTest("Dreadsylvanian Woods", 4)
        val lib = GameRuntimeLibrary(
            gameDatabase = db,
            preferences = prefs,
            dreadKissesTracker = kisses,
        )
        assertEquals(
            "4",
            outputLib(lib, """print(to_location("Dreadsylvanian Woods")["kisses"]);""").trim(),
        )
    }
}
