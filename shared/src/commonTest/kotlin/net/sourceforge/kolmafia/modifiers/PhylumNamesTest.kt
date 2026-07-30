package net.sourceforge.kolmafia.modifiers

import kotlin.test.Test
import kotlin.test.assertEquals

class PhylumNamesTest {

    @Test
    fun getImage_knownPhylum() {
        assertEquals("beastflavor.gif", PhylumNames.getImage("beast"))
        assertEquals("beastflavor.gif", PhylumNames.getImage("beasts"))
    }

    @Test
    fun getImage_unknownReturnsEmpty() {
        assertEquals("", PhylumNames.getImage("none"))
        assertEquals("", PhylumNames.getImage("xyzzy"))
    }
}
