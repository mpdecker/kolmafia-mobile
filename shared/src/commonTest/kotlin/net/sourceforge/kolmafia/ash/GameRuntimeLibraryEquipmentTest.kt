package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import kotlin.test.Test
import kotlin.test.assertEquals

class GameRuntimeLibraryEquipmentTest {

    private fun libWithHat(itemName: String): GameRuntimeLibrary {
        val char = KoLCharacter()
        char.updateEquipment(EquipmentSlot.HAT, itemName)
        return GameRuntimeLibrary(character = char)
    }

    private fun libWithDb(): GameRuntimeLibrary = runBlocking {
        val db = GameDatabase()
        db.load()
        GameRuntimeLibrary(gameDatabase = db)
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
        assertEquals("hat",
            outputLib(GameRuntimeLibrary.forTesting(), """print(to_slot("Hat"));"""))
    }

    @Test
    fun slotToItem_aliasForEquippedItem() {
        val lib = libWithHat("spooky scarecrow")
        assertEquals("spooky scarecrow",
            outputLib(lib, """print(slot_to_item(to_slot("Hat")));"""))
    }

    @Test
    fun toSlot_fromItem_resolvesEquipmentSlot() = runBlocking {
        val lib = libWithDb()
        assertEquals("hat", outputLib(lib, """print(to_slot(to_item("ravioli hat")));""").trim())
        assertEquals("weapon", outputLib(lib, """print(to_slot(to_item("sweet ninja sword")));""").trim())
        assertEquals("", outputLib(lib, """print(to_slot(to_item("meat paste")));""").trim())
    }
}
