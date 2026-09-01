package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ModifierDatabase

class GameRuntimeLibraryAshP493Test {

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
    }

    private fun lines(out: String): List<String> =
        out.lines().map { it.trim() }.filter { it.isNotEmpty() }

    private fun modifiersLib(): GameRuntimeLibrary {
        ModifierDatabase.resetForTest()
        ModifierDatabase.injectForTest(
            "Item",
            "mod hat",
            "Monster Level: +10, Combat Rate: +5, Initiative: +20, Experience (Muscle): +3, Meat Drop: +15, Item Drop: +25",
        )
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(classId = "1"))
        char.updateEquipment(EquipmentSlot.HAT, "mod hat")
        return GameRuntimeLibrary(character = char)
    }

    @Test
    fun revision_phase493() {
        assertEquals("phase4190", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun modifiers_printsDesktopDump() {
        val listed = lines(outputLib(modifiersLib(), """cli_execute("modifiers");"""))
        assertTrue(listed.contains("ML: 10"))
        assertTrue(listed.contains("Enc: 5.0%"))
        assertTrue(listed.contains("Init: 20.0%"))
        assertTrue(listed.contains("Exp: 3.0"))
        assertTrue(listed.contains("Meat: 15.0%"))
        assertTrue(listed.contains("Item: 25.0%"))
    }

    @Test
    fun modifiers_filtersByLeftover() {
        val listed = lines(outputLib(modifiersLib(), """cli_execute("modifiers Init");"""))
        assertTrue(listed.contains("Init: 20.0%"))
        assertFalse(listed.any { it.startsWith("ML:") })
        assertFalse(listed.any { it.startsWith("Enc:") })
    }
}
