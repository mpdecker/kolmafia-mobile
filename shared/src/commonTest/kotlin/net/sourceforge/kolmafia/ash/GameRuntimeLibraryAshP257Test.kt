package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP257Test {

    @Test
    fun revision_phase236() {
        assertEquals("phase370", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun elementBracket_imageField() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("snowflake.gif", outputLib(lib, """print(to_element("cold")["image"]);""").trim())
        assertEquals("circle.gif", outputLib(lib, """print(to_element("slime")["image"]);""").trim())
    }
}
