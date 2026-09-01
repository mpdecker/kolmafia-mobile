package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP259Test {

    @Test
    fun revision_phase245() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun classBracket_idPrimestatAndPathFields() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("1", outputLib(lib, """print(to_class("Seal Clubber")["id"]);""").trim())
        assertEquals("Muscle", outputLib(lib, """print(to_class("Seal Clubber")["primestat"]);""").trim())
        assertEquals("None", outputLib(lib, """print(to_class("Seal Clubber")["path"]);""").trim())
    }
}
