package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP256Test {

    @Test
    fun revision_phase236() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun phylumBracket_imageField() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("beastflavor.gif", outputLib(lib, """print(to_phylum("beast")["image"]);""").trim())
    }
}
