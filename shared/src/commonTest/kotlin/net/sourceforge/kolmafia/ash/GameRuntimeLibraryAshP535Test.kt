package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.EquipmentRequest

class GameRuntimeLibraryAshP535Test {

    private class RecordingEquipment : EquipmentRequest(
        HttpClient(MockEngine { respond("ok") }),
        CharacterRequest(HttpClient(MockEngine { respond("ok") })),
    ) {
        val unequippedSlots = mutableListOf<EquipmentSlot>()
        var unequipAllCalls = 0

        override suspend fun unequipSlot(slot: EquipmentSlot): Result<Unit> {
            unequippedSlots += slot
            return Result.success(Unit)
        }

        override suspend fun unequipAll(): Result<Unit> {
            unequipAllCalls++
            return Result.success(Unit)
        }
    }

    @Test
    fun revision_phase538() {
        assertEquals("phase814", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun unequip_nameMatch_unequipsCorrectSlot() {
        val equip = RecordingEquipment()
        val char = KoLCharacter().also {
            it.updateEquipment(EquipmentSlot.HAT, "helmet of the goddess")
            it.updateEquipment(EquipmentSlot.WEAPON, "seal-clubbing club")
        }
        outputLib(
            GameRuntimeLibrary(character = char, equipmentRequest = equip),
            """cli_execute("unequip helmet");""",
        )
        assertEquals(listOf(EquipmentSlot.HAT), equip.unequippedSlots)
        assertEquals(0, equip.unequipAllCalls)
    }

    @Test
    fun unequip_all_callsUnequipAll() {
        val equip = RecordingEquipment()
        outputLib(
            GameRuntimeLibrary(equipmentRequest = equip),
            """cli_execute("unequip all");""",
        )
        assertEquals(1, equip.unequipAllCalls)
        assertTrue(equip.unequippedSlots.isEmpty())
    }

    @Test
    fun unequip_bare_callsUnequipAll() {
        val equip = RecordingEquipment()
        outputLib(
            GameRuntimeLibrary(equipmentRequest = equip),
            """cli_execute("unequip");""",
        )
        assertEquals(1, equip.unequipAllCalls)
    }

    @Test
    fun unequip_unknown_printsClearError() {
        val out = outputLib(
            GameRuntimeLibrary(character = KoLCharacter()),
            """cli_execute("unequip not-a-real-item");""",
        )
        assertTrue(out.contains("Unknown unequip target: not-a-real-item"))
    }

    @Test
    fun help_listsUnequip() {
        val out = outputLib(GameRuntimeLibrary(), """cli_execute("help unequip");""")
        assertTrue(out.lines().any { it.trim() == "unequip" })
    }
}
