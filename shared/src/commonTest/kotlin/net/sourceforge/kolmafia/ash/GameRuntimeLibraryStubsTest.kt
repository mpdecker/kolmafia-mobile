package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals

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
    fun userConfirm_oneArg_returnsTrue() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("true", outputLib(lib, """print(to_string(user_confirm("Continue?")));""").trim())
    }

    @Test
    fun userConfirm_threeArg_returnsDefaultBoolean() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("false", outputLib(lib, """print(to_string(user_confirm("Continue?", 30, false)));""").trim())
    }

    @Test
    fun userPrompt_oneArg_returnsEmptyString() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("", outputLib(lib, """print(user_prompt("Name?"));""").trim())
    }

    @Test
    fun userPrompt_options_returnsFirstAggregateKey() {
        val lib = GameRuntimeLibrary.forTesting()
        val src = """
            string[string] opts;
            opts["a"] = "A";
            opts["b"] = "B";
            print(user_prompt("Pick", opts));
        """
        assertEquals("a", outputLib(lib, src).trim())
    }

    @Test
    fun userPrompt_threeArg_returnsDefaultString() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("default", outputLib(lib, """print(user_prompt("Name?", 30, "default"));""").trim())
    }

    @Test
    fun toModifier_returnsCanonicalName() {
        val lib = GameRuntimeLibrary.forTesting()
        assertEquals("Muscle Percent", outputLib(lib, """print(to_modifier("Muscle Percent"));"""))
    }
}
