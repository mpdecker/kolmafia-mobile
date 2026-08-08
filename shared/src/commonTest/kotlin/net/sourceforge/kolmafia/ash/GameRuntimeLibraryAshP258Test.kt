package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP258Test {

    @Test
    fun revision_phase245() {
        assertEquals("phase320", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun modifierBracket_nameAndTypeFields() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("Muscle", outputLib(lib, """print(to_modifier("Muscle")["name"]);""").trim())
        assertEquals("numeric", outputLib(lib, """print(to_modifier("Muscle")["type"]);""").trim())
    }
}
