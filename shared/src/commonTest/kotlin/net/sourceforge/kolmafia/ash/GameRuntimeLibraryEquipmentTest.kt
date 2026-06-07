package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryEquipmentTest {

    private fun libWithHat(itemName: String): GameRuntimeLibrary {
        val char = KoLCharacter()
        char.updateEquipment(EquipmentSlot.HAT, itemName)
        return GameRuntimeLibrary(character = char)
    }

    @Test
    fun equippedItem_returnsItemName() {
        val lib = libWithHat("spooky scarecrow")
        assertEquals("spooky scarecrow",
            outputLib(lib, """print(equipped_item(to_slot("Hat")));"""))
    }

    @Test
    fun equippedItem_noneForEmptySlot() {
        assertEquals("none",
            outputLib(GameRuntimeLibrary.forTesting(), """print(equipped_item(to_slot("Hat")));"""))
    }

    @Test
    fun haveEquipped_trueWhenWearing() {
        val lib = libWithHat("spooky scarecrow")
        assertEquals("true",
            outputLib(lib, """print(to_string(have_equipped(to_item("spooky scarecrow"))));"""))
    }

    @Test
    fun haveEquipped_falseWhenNotWearing() {
        assertEquals("false",
            outputLib(GameRuntimeLibrary.forTesting(),
                """print(to_string(have_equipped(to_item("spooky scarecrow"))));"""))
    }

    @Test
    fun toSlot_roundTripsName() {
        assertEquals("Hat",
            outputLib(GameRuntimeLibrary.forTesting(), """print(to_slot("Hat"));"""))
    }

    @Test
    fun slotToItem_aliasForEquippedItem() {
        val lib = libWithHat("spooky scarecrow")
        assertEquals("spooky scarecrow",
            outputLib(lib, """print(slot_to_item(to_slot("Hat")));"""))
    }
}
