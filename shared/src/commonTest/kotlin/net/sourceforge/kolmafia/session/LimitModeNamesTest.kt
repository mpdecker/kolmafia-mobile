package net.sourceforge.kolmafia.session

import kotlin.test.Test
import kotlin.test.assertEquals

class LimitModeNamesTest {

    @Test
    fun ashName_noneReturnsEmpty() {
        assertEquals("", LimitModeNames.ashName(""))
        assertEquals("", LimitModeNames.ashName("none"))
    }

    @Test
    fun ashName_canonicalAliases() {
        assertEquals("spelunky", LimitModeNames.ashName("spelunk"))
        assertEquals("spelunky", LimitModeNames.ashName("Spelunky"))
        assertEquals("edunder", LimitModeNames.ashName("ed"))
        assertEquals("cockroach", LimitModeNames.ashName("roach"))
    }

    @Test
    fun ashName_knownModesPassThrough() {
        assertEquals("batman", LimitModeNames.ashName("batman"))
        assertEquals("bird", LimitModeNames.ashName("bird"))
        assertEquals("mole", LimitModeNames.ashName("mole"))
        assertEquals("astral", LimitModeNames.ashName("astral"))
    }

    @Test
    fun ashName_unrecognizedReturnsUnknown() {
        assertEquals("unknown", LimitModeNames.ashName("avatar"))
        assertEquals("unknown", LimitModeNames.ashName("not-a-mode"))
    }
}
