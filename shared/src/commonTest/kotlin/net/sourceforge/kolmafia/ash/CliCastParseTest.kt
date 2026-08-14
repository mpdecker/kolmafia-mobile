package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CliCastParseTest {

    @Test
    fun parseCliCastSegment_caretEffect() {
        val target = parseCliCastSegment("1 Empathy of the Newt ^ Empathy")
        assertEquals(
            CliCastTarget(count = 1, skillName = "Empathy of the Newt", effectName = "Empathy"),
            target,
        )
    }

    @Test
    fun parseCliCastSegment_odeShorthand() {
        assertEquals(
            CliCastTarget(count = 1, skillName = "The Ode to Booze", effectName = null),
            parseCliCastSegment("ode"),
        )
        assertEquals(
            CliCastTarget(count = 3, skillName = "The Ode to Booze", effectName = null),
            parseCliCastSegment("3 ode"),
        )
    }

    @Test
    fun parseCliCastSegment_stripsOnPlayer() {
        val target = parseCliCastSegment("1 Empathy of the Newt ^ Empathy on Buffy")
        assertEquals("Empathy of the Newt", target?.skillName)
        assertEquals("Empathy", target?.effectName)
    }

    @Test
    fun parseCliCastList_commas() {
        val list = parseCliCastList(
            "1 Leash of Linguini, 2 Empathy of the Newt ^ Empathy",
        )
        assertEquals(2, list.size)
        assertEquals(CliCastTarget(1, "Leash of Linguini", null), list[0])
        assertEquals(CliCastTarget(2, "Empathy of the Newt", "Empathy"), list[1])
        assertTrue(parseCliCastList("").isEmpty())
        assertNull(parseCliCastSegment("   "))
    }

    @Test
    fun parseCastCountAndName_starTreatedAsOne() {
        assertEquals(1 to "Leash of Linguini", parseCastCountAndName("* Leash of Linguini"))
    }
}
