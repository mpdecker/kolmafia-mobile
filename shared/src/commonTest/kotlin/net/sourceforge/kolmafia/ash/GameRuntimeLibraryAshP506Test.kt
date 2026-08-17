package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.maximizer.MaximizerContinuation

class GameRuntimeLibraryAshP506Test {

    @AfterTest
    fun tearDown() {
        MaximizerContinuation.forceContinue()
    }

    @Test
    fun revision_phase506() {
        assertEquals("phase605", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun if_true_runsContinuation() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("if 1 == 1; echo yes");""")
        assertTrue(out.contains("yes"))
        assertFalse(out.contains("[cli]"))
    }

    @Test
    fun if_false_skipsContinuation() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("if 1 == 0; echo no");""")
        assertFalse(out.contains("no"))
    }

    @Test
    fun if_classIs_matchesCharacterClass() {
        val char = KoLCharacter().apply {
            updateFromApiResponse(CharacterApiResponse(classId = "4"))
        }
        val lib = GameRuntimeLibrary(character = char)
        val out = outputLib(lib, """cli_execute("if class is Sauceror; echo sauce");""")
        assertTrue(out.contains("sauce"))
        val miss = outputLib(lib, """cli_execute("if class is Seal Clubber; echo club");""")
        assertFalse(miss.contains("club"))
    }

    @Test
    fun if_healthPercent_comparesAgainstMaxHp() {
        val char = KoLCharacter().apply {
            updateFromApiResponse(CharacterApiResponse(hp = "80", hpmax = "100"))
        }
        val lib = GameRuntimeLibrary(character = char)
        val out = outputLib(lib, """cli_execute("if health > 50%; echo ok");""")
        assertTrue(out.contains("ok"))
        val miss = outputLib(lib, """cli_execute("if health > 90%; echo high");""")
        assertFalse(miss.contains("high"))
    }

    @Test
    fun if_missingOperator_printsError() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("if bananas; echo x");""")
        assertTrue(out.contains("contains no comparison operator"))
        assertFalse(out.lines().any { it.trim() == "x" })
    }

    @Test
    fun if_emptyCondition_printsError() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("if; echo x");""")
        assertTrue(out.contains("No condition specified"))
        assertFalse(out.lines().any { it.trim() == "x" })
    }
}
