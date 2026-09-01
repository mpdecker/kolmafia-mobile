package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryAshP766Test {

    @Test
    fun revision_phase848() {
        assertEquals("phase4010", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun pvpTrack_liveAshRegistered() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false", outputLib(lib, """print(to_string(pvp_attack("someone")));""").trim())
        assertEquals("false", outputLib(lib, "print(to_string(ranked_fam()));").trim())
        assertEquals("0", outputLib(lib, "print(pvp_attacks_left());").trim())
    }
}
