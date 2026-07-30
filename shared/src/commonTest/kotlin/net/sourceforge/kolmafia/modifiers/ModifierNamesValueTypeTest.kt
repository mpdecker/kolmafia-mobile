package net.sourceforge.kolmafia.modifiers

import kotlin.test.Test
import kotlin.test.assertEquals

class ModifierNamesValueTypeTest {

    @Test
    fun canonicalName_muscle() {
        assertEquals("Muscle", ModifierNames.canonicalName("Muscle"))
    }

    @Test
    fun valueType_muscleIsNumeric() {
        assertEquals("numeric", ModifierNames.valueType("Muscle"))
    }

    @Test
    fun valueType_classIsString() {
        assertEquals("string", ModifierNames.valueType("Class"))
    }

    @Test
    fun valueType_effectIsMultistring() {
        assertEquals("multistring", ModifierNames.valueType("Effect"))
    }

    @Test
    fun valueType_unknownIsNone() {
        assertEquals("none", ModifierNames.valueType("not-a-modifier"))
    }
}
