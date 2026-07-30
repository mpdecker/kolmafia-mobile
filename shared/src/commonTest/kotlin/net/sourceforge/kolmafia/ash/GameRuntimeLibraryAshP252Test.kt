package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP252Test {

    @Test
    fun effectBracket_defaultAndAll() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "use 1 decorative fountain",
            outputLib(lib, """print(to_effect("Sleepy")["default"]);""").trim(),
        )
        val all = outputLib(lib, """print(to_effect("Sleepy")["all"]);""").trim()
        assertTrue(all.contains("use 1 decorative fountain"))
        assertTrue(all.contains("eat 1 hippy herbal tea"))
    }

    @Test
    fun effectBracket_noteOnlyWhenHashPrefixed() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("", outputLib(lib, """print(to_effect("Beaten Up")["note"]);""").trim())
        assertEquals(
            "wang used on you",
            outputLib(lib, """print(to_effect("Wanged")["note"]);""").trim(),
        )
    }

    @Test
    fun effectBracket_eitherExpansion() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        val all = outputLib(lib, """print(to_effect("Confused")["all"]);""").trim()
        assertTrue(all.contains("use 1 Now and Earlier"))
        assertTrue(all.contains("use 1 Senior Mints"))
        assertTrue(all.contains("use 1 shingle"))
        assertTrue(all.contains("eat 1 herb brownies"))
    }
}
