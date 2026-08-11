package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP253Test {

    @Test
    fun revision_phase236() {
        assertEquals("phase450", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun familiarBracket_metadataFields() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("1", outputLib(lib, """print(to_familiar("Mosquito")["id"]);""").trim())
        assertEquals("familiar1.gif", outputLib(lib, """print(to_familiar("Mosquito")["image"]);""").trim())
        assertEquals("true", outputLib(lib, """print(to_familiar("Mosquito")["combat"]);""").trim())
        assertEquals("true", outputLib(lib, """print(to_familiar("Mosquito")["physical_damage"]);""").trim())
        assertEquals(
            "mosquito larva",
            outputLib(lib, """print(to_familiar("Mosquito")["hatchling"]);""").trim(),
        )
    }
}
