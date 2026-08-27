package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.data.EquipmentData
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class GameRuntimeLibraryAshP121Test {

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        EquipmentDatabase.resetForTest()
    }

    @Test
    fun revision_phase170() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun isTradeable_and_isGiftable() {
        registerItem(4001, "giftable item", access = setOf('t', 'g', 'd'))
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(is_tradeable(to_item("giftable item")));""").trim())
        assertEquals("true", outputLib(lib, """print(is_giftable(to_item("giftable item")));""").trim())
    }

    @Test
    fun isDiscardable_and_isDisplayable() {
        registerItem(4002, "plain item", access = setOf('t', 'd'))
        val lib = GameRuntimeLibrary()
        assertEquals("true", outputLib(lib, """print(is_discardable(to_item("plain item")));""").trim())
        assertEquals("true", outputLib(lib, """print(is_displayable(to_item("plain item")));""").trim())
    }

    @Test
    fun toPlural_usesDatabasePlural() {
        registerItem(4003, "seal tooth", access = setOf('t', 'd'), plural = "seal teeth")
        val lib = GameRuntimeLibrary()
        assertEquals("seal teeth", outputLib(lib, """print(to_plural(to_item("seal tooth")));""").trim())
    }

    @Test
    fun getPower_weaponHands_itemType_weaponType() {
        registerItem(4004, "25-meat staff", ItemPrimaryUse.WEAPON, setOf('t', 'd'))
        EquipmentDatabase.registerForTest(
            itemId = 4004,
            equipment = EquipmentData(
                name = "25-meat staff",
                power = 70,
                statRequirement = "Mus: 20",
                hands = 2,
                itemType = "staff",
            ),
        )
        val lib = GameRuntimeLibrary()
        assertEquals("70", outputLib(lib, """print(get_power(to_item("25-meat staff")));""").trim())
        assertEquals("2", outputLib(lib, """print(weapon_hands(to_item("25-meat staff")));""").trim())
        assertEquals("staff", outputLib(lib, """print(item_type(to_item("25-meat staff")));""").trim())
        assertEquals("Muscle", outputLib(lib, """print(weapon_type(to_item("25-meat staff")));""").trim())
    }

    private fun registerItem(
        id: Int,
        name: String,
        primaryUse: ItemPrimaryUse = ItemPrimaryUse.USABLE,
        access: Set<Char>,
        plural: String? = null,
    ) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = primaryUse,
                secondaryUses = emptySet(),
                access = access,
                autosellPrice = 10,
                plural = plural,
            ),
        )
    }
}
