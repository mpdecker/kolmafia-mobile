package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ModifierEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryModifiersTest {

    private val lib = GameRuntimeLibrary.forTesting()

    private class ModifierTestDb : GameDatabase() {
        override fun skillModifier(name: String): ModifierEntry? = when (name) {
            "Abdominal Muscles" -> ModifierEntry("Skill", name, "Muscle: +15")
            else -> null
        }

        override fun familiarModifier(name: String): ModifierEntry? = when (name) {
            "(none)" -> ModifierEntry("Familiar", name, "Underwater Familiar")
            "Grandpa Brynne" -> ModifierEntry("Familiar", name, "Mysticality: +5, Class: Disco Bandit")
            else -> null
        }
    }

    private val dbLib = GameRuntimeLibrary(gameDatabase = ModifierTestDb())

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
    fun numericModifier_skill_returnsParsedValue() {
        assertEquals("15.0",
            outputLib(dbLib,
                """print(to_string(numeric_modifier(to_skill("Abdominal Muscles"), "Muscle")));"""))
    }

    @Test
    fun numericModifier_familiar_returnsParsedValue() {
        assertEquals("5.0",
            outputLib(dbLib,
                """print(to_string(numeric_modifier(to_familiar("Grandpa Brynne"), "Mysticality")));"""))
    }

    @Test
    fun booleanModifier_item_unknownReturnsFalse() {
        assertEquals("false",
            outputLib(lib, "print(to_string(boolean_modifier(to_item(\"seal tooth\"), \"Softcore Only\")));"))
    }

    @Test
    fun booleanModifier_skill_unknownReturnsFalse() {
        assertEquals("false",
            outputLib(lib,
                """print(to_string(boolean_modifier(to_skill("No Such Skill"), "Underwater Familiar")));"""))
    }

    @Test
    fun booleanModifier_familiar_returnsTrueForUnderwaterTag() {
        assertEquals("true",
            outputLib(dbLib,
                """print(to_string(boolean_modifier(to_familiar("(none)"), "Underwater Familiar")));"""))
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

    @Test
    fun stringModifier_skill_unknownReturnsEmpty() {
        assertEquals("",
            outputLib(dbLib, """print(string_modifier(to_skill("Abdominal Muscles"), "Class"));"""))
    }

    @Test
    fun stringModifier_familiar_returnsClassString() {
        assertEquals("Disco Bandit",
            outputLib(dbLib,
                """print(string_modifier(to_familiar("Grandpa Brynne"), "Class"));"""))
    }
}
