package net.sourceforge.kolmafia.modifiers

import kotlin.test.Test
import kotlin.test.assertEquals

class ElementNamesTest {

    @Test
    fun getImage_knownElements() {
        assertEquals("snowflake.gif", ElementNames.getImage("cold"))
        assertEquals("fire.gif", ElementNames.getImage("hot"))
        assertEquals("circle.gif", ElementNames.getImage("slime"))
    }

    @Test
    fun getImage_unknownReturnsEmpty() {
        assertEquals("", ElementNames.getImage("none"))
        assertEquals("", ElementNames.getImage("xyzzy"))
    }
}
