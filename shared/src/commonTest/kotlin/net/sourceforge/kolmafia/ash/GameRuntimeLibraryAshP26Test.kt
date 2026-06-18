package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP26Test {

    @Test
    fun servantIsValid_acceptsCanonicalType() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(to_string(is_valid(to_servant("Cat"))));""").trim())
        assertEquals("false", outputLib(lib, """print(to_string(is_valid(to_servant("skeleton"))));""").trim())
    }

    @Test
    fun toServant_resolvesKnownServant() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("Cat", outputLib(lib, """print(to_servant("Cat"));""").trim())
        assertEquals("", outputLib(lib, """print(to_servant("skeleton"));""").trim())
    }

    @Test
    fun haveServant_trueWhenTypeResolves() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(to_string(have_servant(to_servant("Maid"))));""").trim())
        assertEquals("false", outputLib(lib, """print(to_string(have_servant(to_servant("bogus"))));""").trim())
    }

    @Test
    fun toVykea_resolvesLevelCouch() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("level 3 couch", outputLib(lib, """print(to_vykea("level 3 couch"));""").trim())
    }

    @Test
    fun vykeaNumericModifier_couchMeatDrop() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals(
            "30.0",
            outputLib(
                lib,
                """print(to_string(numeric_modifier(to_vykea("level 3 couch"), "Meat Drop")));""",
            ).trim(),
        )
    }

    @Test
    fun servantTypeOf_returnsServant() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("servant", outputLib(lib, """print(type_of(to_servant("Priest")));""").trim())
    }

    @Test
    fun vykeaTypeOf_returnsVykea() = runBlocking {
        val lib = GameRuntimeLibrary()
        assertEquals("vykea", outputLib(lib, """print(type_of(to_vykea("level 1 lamp")));""").trim())
    }
}
