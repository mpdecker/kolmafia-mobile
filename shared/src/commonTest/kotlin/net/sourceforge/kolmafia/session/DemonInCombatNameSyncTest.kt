package net.sourceforge.kolmafia.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.preferences.Preferences

class DemonInCombatNameSyncTest {

    private fun sync(): DemonInCombatNameSync =
        DemonInCombatNameSync(Preferences(MapSettings()))

    private fun radioHtml(greyText: String): String =
        """You try to radio back... static... <i style='color: #999'>$greyText</i>... crackle"""

    @Test
    fun parseRadioResponse_extractsGreyText() {
        val s = sync()
        val result = s.parseRadioResponse(radioHtml("ulH"))
        assertTrue(result.updated)
        assertEquals(setOf("ulH"), s.knownSegmentKeys())
    }

    @Test
    fun updateSegment_mergesWithCount() {
        val prefs = Preferences(MapSettings())
        val s = DemonInCombatNameSync(prefs)
        s.updateSegment("ulH")
        s.updateSegment("ulH")
        assertEquals("ulH:2", prefs.getString(Preferences.DEMON_NAME_14_SEGMENTS, ""))
    }

    @Test
    fun updateSegment_skipsWhenDemonName14Set() {
        val prefs = Preferences(MapSettings())
        prefs.setString(Preferences.DEMON_NAME_14, "Ak'gyxoth")
        val s = DemonInCombatNameSync(prefs)
        val result = s.updateSegment("ulH")
        assertFalse(result.updated)
        assertEquals("", prefs.getString(Preferences.DEMON_NAME_14_SEGMENTS, ""))
    }

    @Test
    fun parseSegmentsPref_roundTrip() {
        val s = sync()
        val map = linkedMapOf("ulH" to 2, "rgB" to 1)
        val formatted = s.formatSegmentsPref(map)
        assertEquals("ulH:2,rgB", formatted)
        assertEquals(map, s.parseSegmentsPref(formatted))
    }

    @Test
    fun hintMessage_whenMoreThanTenSegments() {
        val prefs = Preferences(MapSettings())
        val s = DemonInCombatNameSync(prefs)
        val segments = listOf("a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k")
        var lastHint: String? = null
        for (segment in segments) {
            lastHint = s.updateSegment(segment).hintMessage
        }
        assertTrue(lastHint?.contains("demons solve14") == true)
    }
}
