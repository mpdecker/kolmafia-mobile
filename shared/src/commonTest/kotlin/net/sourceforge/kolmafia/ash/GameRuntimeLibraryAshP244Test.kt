package net.sourceforge.kolmafia.ash

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP244Test {

    @BeforeTest
    fun setUp() = runTest {
        GameDatabase().load()
    }

    @Test
    fun revision_isPhase226() {
        assertEquals("phase230", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun itemBracket_metadataFieldsSmoke() = runTest {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "Unspaded",
            outputLib(lib, """print(to_item("candy rations")["notes"]);""").trim(),
        )
        assertEquals(
            "simple",
            outputLib(lib, """print(to_item("tamarind-flavored chewing gum")["candy_type"]);""").trim(),
        )
        assertEquals(
            "true",
            outputLib(lib, """print(to_item("fancy chocolate")["chocolate"]);""").trim(),
        )
        assertEquals(
            "7",
            outputLib(lib, """print(to_item("aspirin")["name_length"]);""").trim(),
        )
    }
}
