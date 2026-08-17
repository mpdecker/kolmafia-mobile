package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase

class GameRuntimeLibraryAshP255Test {

    @Test
    fun revision_phase236() {
        assertEquals("phase605", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun bountyBracket_allFields() = runBlocking {
        val db = GameDatabase()
        db.load()
        val lib = GameRuntimeLibrary(gameDatabase = db)
        assertEquals(
            "bean-shaped rocks",
            outputLib(lib, """print(to_bounty("bean-shaped rock")["plural"]);""").trim(),
        )
        assertEquals("easy", outputLib(lib, """print(to_bounty("bean-shaped rock")["type"]);""").trim())
        assertEquals("low", outputLib(lib, """print(to_bounty("bean-shaped rock")["kol_internal_type"]);""").trim())
        assertEquals("12", outputLib(lib, """print(to_bounty("bean-shaped rock")["number"]);""").trim())
        assertEquals("bean.gif", outputLib(lib, """print(to_bounty("bean-shaped rock")["image"]);""").trim())
        assertEquals("beanbat", outputLib(lib, """print(to_bounty("bean-shaped rock")["monster"]);""").trim())
        assertEquals(
            "The Beanbat Chamber",
            outputLib(lib, """print(to_bounty("bean-shaped rock")["location"]);""").trim(),
        )
    }
}
