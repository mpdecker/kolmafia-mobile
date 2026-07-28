package net.sourceforge.kolmafia.character

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ZodiacSignTest {

    @Test
    fun find_muscleSigns() {
        assertTrue(ZodiacSign.find("Mongoose")!!.isMuscle)
        assertTrue(ZodiacSign.find("platypus")!!.isMuscle)
        assertTrue(ZodiacSign.find("WOMBAT")!!.isMuscle)
    }

    @Test
    fun find_mysticalitySigns() {
        assertTrue(ZodiacSign.find("Wallaby")!!.isMysticality)
        assertTrue(ZodiacSign.find("opossum")!!.isMysticality)
        assertTrue(ZodiacSign.find("Blender")!!.isMysticality)
    }

    @Test
    fun find_moxieSigns() {
        assertTrue(ZodiacSign.find("Vole")!!.isMoxie)
        assertTrue(ZodiacSign.find("marmot")!!.isMoxie)
        assertTrue(ZodiacSign.find("Packrat")!!.isMoxie)
    }

    @Test
    fun find_badMoon() {
        assertTrue(ZodiacSign.find("Bad Moon")!!.isBadMoon)
        assertFalse(ZodiacSign.find("Bad Moon")!!.isMuscle)
    }

    @Test
    fun find_unknown_returnsNull() {
        assertNull(ZodiacSign.find(""))
        assertNull(ZodiacSign.find("bogus"))
    }
}
