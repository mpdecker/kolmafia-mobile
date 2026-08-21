package net.sourceforge.kolmafia.ash

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP243Test {

    @BeforeTest
    fun setUp() = runTest {
        GameDatabase().load()
    }

    @Test
    fun revision_isphase226() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun itemBracket_flagFieldsSmoke() = runTest {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "true",
            outputLib(lib, """print(to_item("acceptable bagel")["tradeable"]);""").trim(),
        )
        assertEquals(
            "true",
            outputLib(lib, """print(to_item("ten-leaf clover")["multi"]);""").trim(),
        )
        assertEquals(
            "true",
            outputLib(lib, """print(to_item("spider web")["combat"]);""").trim(),
        )
        assertEquals(
            "false",
            outputLib(lib, """print(to_item("spider web")["usable"]);""").trim(),
        )
        assertEquals(
            "true",
            outputLib(lib, """print(to_item("Dolphin King's map")["quest"]);""").trim(),
        )
    }
}
