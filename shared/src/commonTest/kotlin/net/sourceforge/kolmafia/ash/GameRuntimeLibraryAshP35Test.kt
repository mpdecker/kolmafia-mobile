package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class GameRuntimeLibraryAshP35Test {

    @Test
    fun adventureDatabase_persistsForceNoncombat() = runBlocking {
        AdventureDatabase.resetForTest()
        val db = GameDatabase()
        db.load()
        val zone = AdventureDatabase.getByName("The Spooky Forest")
        assertEquals(8, zone?.forceNoncombat)
    }

    @Test
    fun locationBracketId_readsBundledAdventure() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("113", outputLib(lib, """print(to_location("The Haunted Pantry")["id"]);""").trim())
        assertEquals("Manor1", outputLib(lib, """print(to_location("The Haunted Pantry")["zone"]);""").trim())
        assertEquals("indoor", outputLib(lib, """print(to_location("The Haunted Pantry")["environment"]);""").trim())
        assertEquals("80.0", outputLib(lib, """print(to_string(to_location("The Haunted Pantry")["combat_percent"]));""").trim())
    }

    @Test
    fun locationBracketForceNoncombat_readsBundledAdventure() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("8", outputLib(lib, """print(to_location("The Spooky Forest")["force_noncombat"]);""").trim())
    }

    @Test
    fun locationBracketParentAndRoot_readsZoneList() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("Manor", outputLib(lib, """print(to_location("The Haunted Pantry")["parent"]);""").trim())
        assertEquals("Town", outputLib(lib, """print(to_location("The Haunted Pantry")["root"]);""").trim())
        assertEquals(
            "Spookyraven Manor",
            outputLib(lib, """print(to_location("The Haunted Pantry")["parentdesc"]);""").trim(),
        )
    }

    @Test
    fun locationBracketBounty_readsBountyDatabase() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "shredded can label",
            outputLib(lib, """print(to_location("The Haunted Pantry")["bounty"]);""").trim(),
        )
    }

    @Test
    fun locationBracketUnknownLocation_returnsZeros() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("0", outputLib(lib, """print(to_location("Nowhere Land")["id"]);""").trim())
        assertEquals("", outputLib(lib, """print(to_location("Nowhere Land")["zone"]);""").trim())
    }

    @Test
    fun locationBracketUnknownField_throwsScriptException() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val failed = runCatching {
            outputLib(lib, """print(to_location("The Haunted Pantry")["water_level"]);""")
        }.isFailure
        assertTrue(!failed)
        val unknownField = runCatching {
            outputLib(lib, """print(to_location("The Haunted Pantry")["bogus_field"]);""")
        }.isFailure
        assertTrue(unknownField)
    }

    @Test
    fun myLocationBracket_readsLastLocationPref() = runBlocking {
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.LAST_LOCATION, "The Haunted Pantry")
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(preferences = prefs, gameDatabase = db)
        assertEquals("113", outputLib(lib, """print(my_location()["id"]);""").trim())
    }

    @Test
    fun pathBracketFamiliars_readsPathMetadata() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(to_string(to_path("You, Robot")["familiars"]));""").trim())
        assertEquals(
            "false",
            outputLib(lib, """print(to_string(to_path("Avatar of Boris")["familiars"]));""").trim(),
        )
    }

    @Test
    fun pathBracketAvatarAndImage_readsPathMetadata() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(to_string(to_path("Avatar of Boris")["avatar"]));""").trim())
        assertEquals("8", outputLib(lib, """print(to_path("Avatar of Boris")["id"]);""").trim())
        assertEquals("trusty.gif", outputLib(lib, """print(to_path("Avatar of Boris")["image"]);""").trim())
        assertEquals("41", outputLib(lib, """print(to_path("You, Robot")["id"]);""").trim())
        assertEquals("robobattery.gif", outputLib(lib, """print(to_path("You, Robot")["image"]);""").trim())
    }

    @Test
    fun pathBracketPoints_readsPreference() = runBlocking {
        val prefs = Preferences(MapSettings())
        prefs.setInt("edPoints", 12)
        val lib = GameRuntimeLibrary(preferences = prefs)
        assertEquals("12", outputLib(lib, """print(to_path("Actually Ed the Undying")["points"]);""").trim())
    }
}
