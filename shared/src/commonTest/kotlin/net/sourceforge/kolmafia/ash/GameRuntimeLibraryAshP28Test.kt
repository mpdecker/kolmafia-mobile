package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP28Test {

    private fun libWithDb(): GameRuntimeLibrary = runBlocking {
        CoinmasterDatabase.load()
        val db = GameDatabase()
        db.load()
        GameRuntimeLibrary(gameDatabase = db)
    }

    @Test
    fun coinmasterIsValid_acceptsKnownCoinmaster() = runBlocking {
        val lib = libWithDb()
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(is_valid(to_coinmaster("dimemaster"))));""").trim(),
        )
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(is_valid(to_coinmaster("Cosmic Ray's Bazaar"))));""").trim(),
        )
        assertEquals("false", outputLib(lib, """print(to_string(is_valid(to_coinmaster("bogus"))));""").trim())
    }

    @Test
    fun toCoinmaster_resolvesCanonicalName() = runBlocking {
        val lib = libWithDb()
        assertEquals("Dimemaster", outputLib(lib, """print(to_coinmaster("dmt"));""").trim())
        assertEquals("", outputLib(lib, """print(to_coinmaster("bogus"));""").trim())
    }

    @Test
    fun modifierIsValid_acceptsKnownModifier() {
        val lib = GameRuntimeLibrary()
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(is_valid(to_modifier("Muscle Percent"))));""").trim(),
        )
        assertEquals("false", outputLib(lib, """print(to_string(is_valid(to_modifier("bogus"))));""").trim())
    }

    @Test
    fun toModifier_resolvesCanonicalName() {
        val lib = GameRuntimeLibrary()
        assertEquals("Muscle Percent", outputLib(lib, """print(to_modifier("mus_pct"));""").trim())
        assertEquals("", outputLib(lib, """print(to_modifier("bogus"));""").trim())
    }

    @Test
    fun coinmasterTypeOf_returnsCoinmaster() = runBlocking {
        val lib = libWithDb()
        assertEquals("coinmaster", outputLib(lib, """print(type_of(to_coinmaster("dimemaster")));""").trim())
    }

    @Test
    fun modifierTypeOf_returnsModifier() {
        val lib = GameRuntimeLibrary()
        assertEquals("modifier", outputLib(lib, """print(type_of(to_modifier("Muscle Percent")));""").trim())
    }

    @Test
    fun coinmasterNumericModifier_returnsZero() = runBlocking {
        val lib = libWithDb()
        assertEquals(
            "0.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier(to_coinmaster("dimemaster"), "Meat Drop")));""",
            ).trim(),
        )
    }
}
