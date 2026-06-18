package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP29Test {

    @Test
    fun classIsValid_acceptsKnownClass() {
        val lib = GameRuntimeLibrary()
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(is_valid(to_class("Seal Clubber"))));""").trim(),
        )
        assertEquals("false", outputLib(lib, """print(to_string(is_valid(to_class("bogus"))));""").trim())
    }

    @Test
    fun toClass_resolvesCanonicalName() {
        val lib = GameRuntimeLibrary()
        assertEquals("Seal Clubber", outputLib(lib, """print(to_class("seal clubber"));""").trim())
        assertEquals("", outputLib(lib, """print(to_class("bogus"));""").trim())
    }

    @Test
    fun elementIsValid_acceptsKnownElement() {
        val lib = GameRuntimeLibrary()
        assertEquals(
            "true",
            outputLib(lib, """print(to_string(is_valid(to_element("cold"))));""").trim(),
        )
        assertEquals("false", outputLib(lib, """print(to_string(is_valid(to_element("bogus"))));""").trim())
    }

    @Test
    fun toElement_resolvesCanonicalName() {
        val lib = GameRuntimeLibrary()
        assertEquals("cold", outputLib(lib, """print(to_element("Cold"));""").trim())
        assertEquals("", outputLib(lib, """print(to_element("bogus"));""").trim())
    }

    @Test
    fun classTypeOf_returnsClass() {
        val lib = GameRuntimeLibrary()
        assertEquals("class", outputLib(lib, """print(type_of(to_class("Seal Clubber")));""").trim())
    }

    @Test
    fun elementTypeOf_returnsElement() {
        val lib = GameRuntimeLibrary()
        assertEquals("element", outputLib(lib, """print(type_of(to_element("cold")));""").trim())
    }
}
