package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryModifiersTest {

    private val lib = GameRuntimeLibrary.forTesting()

    @Test
    fun numericModifier_item_unknownReturnsZero() {
        assertEquals("0.0",
            outputLib(lib, "print(to_string(numeric_modifier(to_item(\"seal tooth\"), \"Muscle\")));"))
    }

    @Test
    fun numericModifier_effect_unknownReturnsZero() {
        assertEquals("0.0",
            outputLib(lib, "print(to_string(numeric_modifier(to_effect(\"Saucestorm\"), \"Spell Damage\")));"))
    }

    @Test
    fun booleanModifier_item_unknownReturnsFalse() {
        assertEquals("false",
            outputLib(lib, "print(to_string(boolean_modifier(to_item(\"seal tooth\"), \"Softcore Only\")));"))
    }

    @Test
    fun stringModifier_item_unknownReturnsEmpty() {
        assertEquals("",
            outputLib(lib, "print(string_modifier(to_item(\"seal tooth\"), \"Class\"));"))
    }

    @Test
    fun stringModifier_effect_unknownReturnsEmpty() {
        assertEquals("",
            outputLib(lib, "print(string_modifier(to_effect(\"Saucestorm\"), \"Class\"));"))
    }
}
