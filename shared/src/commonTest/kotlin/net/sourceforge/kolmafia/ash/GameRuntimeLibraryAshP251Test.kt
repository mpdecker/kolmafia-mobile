package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP251Test {

    @Test
    fun revision_phase236() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun effectBracket_metadataFields() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("2", outputLib(lib, """print(to_effect("Sleepy")["id"]);""").trim())
        assertEquals("Sleepy", outputLib(lib, """print(to_effect("Sleepy")["name"]);""").trim())
        assertEquals("sleepy.gif", outputLib(lib, """print(to_effect("Sleepy")["image"]);""").trim())
        assertEquals(
            "35b38533640fed03c12460b1dca98e81",
            outputLib(lib, """print(to_effect("Sleepy")["descid"]);""").trim(),
        )
        assertEquals("bad", outputLib(lib, """print(to_effect("Sleepy")["quality"]);""").trim())
        assertEquals("", outputLib(lib, """print(to_effect("Sleepy")["attributes"]);""").trim())
        assertEquals("false", outputLib(lib, """print(to_effect("Sleepy")["song"]);""").trim())
        assertEquals("1", outputLib(lib, """print(to_effect("Synthesis: Hot")["candy_tier"]);""").trim())
    }

    @Test
    fun effectBracket_songAttribute() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals("true", outputLib(lib, """print(to_effect("Aloysius' Antiphon of Aptitude")["song"]);""").trim())
        assertEquals("song", outputLib(lib, """print(to_effect("Aloysius' Antiphon of Aptitude")["attributes"]);""").trim())
    }
}
