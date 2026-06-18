package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP27Test {

    private fun libWithDb(): GameRuntimeLibrary = runBlocking {
        val db = GameDatabase()
        db.load()
        GameRuntimeLibrary(gameDatabase = db)
    }

    @Test
    fun bountyIsValid_acceptsKnownBounty() = runBlocking {
        val lib = libWithDb()
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(is_valid(to_bounty("bean-shaped rock"))));""").trim(),
        )
        assertEquals("false", outputLib(lib, """print(to_string(is_valid(to_bounty("bogus"))));""").trim())
    }

    @Test
    fun toBounty_resolvesCanonicalName() = runBlocking {
        val lib = libWithDb()
        assertEquals("bean-shaped rock", outputLib(lib, """print(to_bounty("bean-shaped rock"));""").trim())
        assertEquals("", outputLib(lib, """print(to_bounty("bogus"));""").trim())
    }

    @Test
    fun toSlot_resolvesAcc1() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("acc1", outputLib(lib, """print(to_slot("acc1"));""").trim())
        assertEquals("", outputLib(lib, """print(to_slot("bogus"));""").trim())
    }

    @Test
    fun toPhylum_resolvesUndead() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("undead", outputLib(lib, """print(to_phylum("undead"));""").trim())
    }

    @Test
    fun slotTypeOf_returnsSlot() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("slot", outputLib(lib, """print(type_of(to_slot("hat")));""").trim())
    }

    @Test
    fun bountyNumericModifier_returnsZero() = runBlocking {
        val lib = libWithDb()
        assertEquals(
            "0.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier(to_bounty("bean-shaped rock"), "Meat Drop")));""",
            ).trim(),
        )
    }
}
