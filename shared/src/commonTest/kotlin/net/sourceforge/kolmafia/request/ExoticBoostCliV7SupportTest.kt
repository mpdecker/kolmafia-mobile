package net.sourceforge.kolmafia.request

import com.russhwolf.settings.MapSettings
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.BeachHeadAvailability
import net.sourceforge.kolmafia.session.RabbitHoleAvailability
import net.sourceforge.kolmafia.session.SkateParkAvailability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExoticBoostCliV7SupportTest {

    private fun prefs(block: MapSettings.() -> Unit = {}): Preferences {
        val s = MapSettings()
        s.block()
        return Preferences(s)
    }

    @Test
    fun beach_resolveHead_and_parse() {
        assertEquals(1, BeachHeadAvailability.resolveHead("1")?.id)
        assertEquals(1, BeachHeadAvailability.resolveHead("Hot-Headed")?.id)
        assertEquals(1, BeachHeadAvailability.resolveHead("hot")?.id)
        assertEquals(2, BeachHeadAvailability.resolveHead("cold")?.id)
        assertNull(BeachHeadAvailability.resolveHead("xyzzy"))
        assertEquals("Hot-Headed", BeachCombRequest.parseHeadQuery("head Hot-Headed"))
        assertNull(BeachCombRequest.parseHeadQuery("visit"))
        assertNull(BeachCombRequest.parseHeadQuery("head"))

        val p = prefs()
        assertTrue(BeachHeadAvailability.headAvailable("Hot-Headed", p))
        BeachHeadAvailability.markHeadUsed(p, 1)
        assertFalse(BeachHeadAvailability.headAvailable("Hot-Headed", p))
        assertTrue(p.getString(BeachHeadAvailability.HEADS_USED_PREF, "").contains("1"))
    }

    @Test
    fun skate_placeToBuff_and_action() {
        assertEquals(SkateParkAvailability.LUTZ, SkateParkAvailability.placeToBuff("lutz"))
        assertEquals(SkateParkAvailability.COMET, SkateParkAvailability.placeToBuff("comet"))
        assertEquals(SkateParkAvailability.BAND_SHELL, SkateParkAvailability.placeToBuff("band shell"))
        assertEquals(SkateParkAvailability.ECLECTIC_EELS, SkateParkAvailability.placeToBuff("eels"))
        assertEquals(
            SkateParkAvailability.MERRY_GO_ROUND,
            SkateParkAvailability.placeToBuff("merry-go-round"),
        )
        assertEquals("state2buff1", SkateParkRequest.findBuffAction("lutz"))
        assertEquals("state4buff3", SkateParkRequest.findBuffAction("merry-go-round"))
        assertNull(SkateParkRequest.findBuffAction("boardwalk"))
    }

    @Test
    fun hatter_parseLength() {
        assertEquals(4, HatterRequest.parseLength("4"))
        assertEquals(30, HatterRequest.parseLength("30"))
        assertNull(HatterRequest.parseLength(""))
        assertNull(HatterRequest.parseLength("fedora"))
        assertNull(HatterRequest.parseLength("3"))
        assertNotNull(RabbitHoleAvailability.hatDataForLength(22))
        assertEquals("Dances with Tweedles", RabbitHoleAvailability.hatDataForLength(22)?.effect)
    }

    @Test
    fun synthesize_resolveEffect() {
        assertEquals(2165, SweetSynthesisRequest.resolveEffectId("Synthesis: Hot"))
        assertEquals(2165, SweetSynthesisRequest.resolveEffectId("Hot"))
        assertEquals(2175, SweetSynthesisRequest.resolveEffectId("Synthesis: Greed"))
        assertEquals(2166, SweetSynthesisRequest.resolveEffectId("2166"))
        assertNull(SweetSynthesisRequest.resolveEffectId("Synthesis: Bogus"))
        assertEquals(
            "Synthesis: Hot",
            SweetSynthesisRequest.parseEffectQuery("Synthesis: Hot"),
        )
        assertNull(SweetSynthesisRequest.parseEffectQuery(""))
    }
}
