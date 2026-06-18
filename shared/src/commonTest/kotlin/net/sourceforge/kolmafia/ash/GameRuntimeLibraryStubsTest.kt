package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameRuntimeLibraryStubsTest {

    @Test
    fun pvpAttack_returnsFalse() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false", outputLib(lib, """print(to_string(pvp_attack("rival")));"""))
    }

    @Test
    fun rankedFam_returnsFalse() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false", outputLib(lib, "print(to_string(ranked_fam()));"))
    }

    @Test
    fun userConfirm_throwsScriptException() {
        val lib = GameRuntimeLibrary.forTesting()
        val failed = runCatching {
            outputLib(lib, """user_confirm("Continue?");""")
        }.isFailure
        assertTrue(failed)
    }

    @Test
    fun toModifier_returnsCanonicalName() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("Muscle Percent", outputLib(lib, """print(to_modifier("Muscle Percent"));"""))
    }
}
