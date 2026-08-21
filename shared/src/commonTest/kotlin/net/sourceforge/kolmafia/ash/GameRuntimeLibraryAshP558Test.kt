package net.sourceforge.kolmafia.ash

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter

class GameRuntimeLibraryAshP558Test {

    @Test
    fun revision_phase605() {
        assertEquals("phase826", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun stickers_bare_printsSlots() {
        val character = KoLCharacter()
        character.updateEquipment(EquipmentSlot.STICKER1, "scratch 'n' sniff sword sticker")
        val out = outputLib(
            GameRuntimeLibrary(character = character),
            """cli_execute("stickers");""",
        )
        assertTrue(out.contains("Sticker 1:", ignoreCase = true))
        assertTrue(out.contains("sword sticker", ignoreCase = true))
        assertTrue(out.contains("Sticker 2:", ignoreCase = true))
        assertTrue(out.contains("Sticker 3:", ignoreCase = true))
    }
}
