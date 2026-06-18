package net.sourceforge.kolmafia.thrall

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PastaThrallCharpaneSyncTest {

    @Test
    fun parse_vampieroghiFromCharpane() {
        val html = """
            <a href="desc_guardian.php?whichguardian=1">guardian</a>
            <img src="itemimages/vampieroghi.gif">
            <b>Count Alfredo</b> is the Lvl. 7 Vampieroghi</font>
        """.trimIndent()
        val parsed = PastaThrallCharpaneSync.parse(html)
        requireNotNull(parsed)
        assertEquals("Count Alfredo", parsed.customName)
        assertEquals(7, parsed.level)
        assertEquals("Vampieroghi", parsed.type)
    }

    @Test
    fun parse_htmlEntityName() {
        val html = """
            desc_guardian.php?whichguardian=7
            itemimages/spiceghost.gif
            <b>&Eacute;lan Vital</b> blah the Lvl. 10 Spice Ghost</font>
        """.trimIndent()
        val parsed = PastaThrallCharpaneSync.parse(html)
        requireNotNull(parsed)
        assertEquals("Élan Vital", parsed.customName)
        assertEquals(10, parsed.level)
        assertEquals("Spice Ghost", parsed.type)
    }

    @Test
    fun parse_returnsNullWhenMissing() {
        assertNull(PastaThrallCharpaneSync.parse("<html><body>No thrall here</body></html>"))
    }
}
