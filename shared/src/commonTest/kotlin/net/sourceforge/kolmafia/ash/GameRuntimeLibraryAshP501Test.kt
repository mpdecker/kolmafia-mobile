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

class GameRuntimeLibraryAshP501Test {

    @AfterTest
    fun tearDown() {
        ModifierDatabase.resetForTest()
    }

    private fun modrefLib(): GameRuntimeLibrary {
        ModifierDatabase.resetForTest()
        ModifierDatabase.injectForTest(
            "Item",
            "mod hat",
            "Monster Level: +10, Combat Rate: +5",
        )
        val char = KoLCharacter()
        char.updateFromApiResponse(CharacterApiResponse(classId = "1"))
        char.updateEquipment(EquipmentSlot.HAT, "mod hat")
        return GameRuntimeLibrary(character = char)
    }

    @Test
    fun revision_phase501() {
        assertEquals("phase743", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun modref_monster_containsMlLine() {
        val out = outputLib(modrefLib(), """cli_execute("modref Monster");""")
        assertTrue(out.lines().any { it.contains("Monster Level:") && it.contains("10") })
    }

    @Test
    fun modref_leftover_filters() {
        val out = outputLib(modrefLib(), """cli_execute("modref Monster");""")
        assertTrue(out.lines().any { it.contains("Monster Level:") })
        assertFalse(out.lines().any { it.contains("Combat Rate:") })
    }
}
