package net.sourceforge.kolmafia.vykea

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import net.sourceforge.kolmafia.modifiers.VykeaCompanionData
import net.sourceforge.kolmafia.preferences.Preferences

class VykeaCharpaneSyncTest {

    @Test
    fun parse_namedLampFromCharpane() {
        val html = """
            <font size=2><b>VYKEA Companion</b></font><br><font size=2><b>CHEBLI</b> the level 5 lamp<br>
        """.trimIndent()
        val companion = VykeaCharpaneSync.parse(html, savedRune = "blood")
        requireNotNull(companion)
        assertEquals("CHEBLI", companion.name)
        assertEquals(5, companion.level)
        assertEquals(VykeaCompanionData.VykeaType.LAMP, companion.type)
        assertEquals(VykeaCompanionData.VykeaRune.BLOOD, companion.rune)
    }

    @Test
    fun parse_htmlEntityName() {
        val html = """
            <font size=2><b>VYKEA Companion</b></font><br><font size=2><b>&Aring;VOB&Eacute;</b> the level 3 couch<br>
        """.trimIndent()
        val companion = VykeaCharpaneSync.parse(html, savedRune = "")
        requireNotNull(companion)
        assertEquals("ÅVOBÉ", companion.name)
        assertEquals(3, companion.level)
        assertEquals(VykeaCompanionData.VykeaType.COUCH, companion.type)
        assertEquals(VykeaCompanionData.VykeaRune.NONE, companion.rune)
    }

    @Test
    fun parse_ceilingFanType() {
        val html = """
            <b>VYKEA Companion</b></font><br><font size=2><b>Spinny</b> the level 2 ceiling fan<br>
        """.trimIndent()
        val companion = VykeaCharpaneSync.parse(html, savedRune = "frenzy")
        requireNotNull(companion)
        assertEquals(VykeaCompanionData.VykeaType.CEILING_FAN, companion.type)
        assertEquals(VykeaCompanionData.VykeaRune.FRENZY, companion.rune)
    }

    @Test
    fun parse_returnsNullWhenMissing() {
        assertNull(VykeaCharpaneSync.parse("<html><body>No companion</body></html>", ""))
    }
}
