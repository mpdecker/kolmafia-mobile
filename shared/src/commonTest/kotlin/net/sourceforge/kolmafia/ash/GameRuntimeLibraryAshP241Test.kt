package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP241Test {

    @BeforeTest
    fun setUp() = runTest {
        GameDatabase().load()
    }

    @AfterTest
    fun tearDown() {
        ConsumableDatabase.resetForTest()
    }

    @Test
    fun revision_isphase226() {
        assertEquals("phase300", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun itemBracket_fullnessSmoke() = runTest {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "3",
            outputLib(lib, """print(to_item("acceptable bagel")["fullness"]);""").trim(),
        )
        assertEquals(
            "good",
            outputLib(lib, """print(to_item("acceptable bagel")["quality"]);""").trim(),
        )
        assertEquals(
            "0",
            outputLib(lib, """print(to_item("battery (AAA)")["fullness"]);""").trim(),
        )
    }
}
