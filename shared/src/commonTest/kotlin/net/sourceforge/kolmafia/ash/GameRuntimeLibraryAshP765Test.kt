package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryAshP765Test {

    @Test
    fun revision_phase765() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun pvp_attack_and_ranked_fam_stillRegistered() {
        val lib = GameRuntimeLibrary.forTesting()
        // Without HTTP these return false, but they must resolve (not missing functions).
        assertEquals("false", outputLib(lib, """print(to_string(pvp_attack("rival")));""").trim())
        assertEquals("false", outputLib(lib, "print(to_string(ranked_fam()));").trim())
    }

    @Test
    fun ashFunctionInventory_stillMeetsFloor() {
        val scope = AshScope()
        GameRuntimeLibrary.forTesting().registerAll(scope)
        val count = scope.debugFunctionCount()
        assertTrue(count >= 890, "Expected ≥890 overloads after PvP stub teardown, got $count")
    }
}
