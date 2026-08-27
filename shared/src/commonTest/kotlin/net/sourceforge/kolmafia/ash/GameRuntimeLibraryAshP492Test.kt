package net.sourceforge.kolmafia.ash

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterApiResponse
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse

class GameRuntimeLibraryAshP492Test {

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
    }

    private fun lines(out: String): List<String> =
        out.lines().map { it.trim() }.filter { it.isNotEmpty() }

    private fun equippedLib(
        equipment: Map<EquipmentSlot, String> = emptyMap(),
        familiarName: String = "",
        familiarWeight: String = "0",
        enthronedName: String = "",
        bjornedName: String = "",
        hats: List<String> = emptyList(),
        path: String = "",
    ): GameRuntimeLibrary {
        val char = KoLCharacter()
        char.updateFromApiResponse(
            CharacterApiResponse(
                path = path,
                familiarname = familiarName,
                familiarweight = familiarWeight,
                enthronedname = enthronedName,
                bjornedname = bjornedName,
                hats = hats,
            ),
        )
        for ((slot, name) in equipment) {
            char.updateEquipment(slot, name)
        }
        return GameRuntimeLibrary(character = char)
    }

    @Test
    fun revision_phase492() {
        assertEquals("phase3050", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun equip_bareListsSlots() {
        val out = outputLib(
            equippedLib(
                equipment = mapOf(
                    EquipmentSlot.HAT to "helmet turtle",
                    EquipmentSlot.WEAPON to "seal-clubbing club",
                ),
                familiarName = "Mosquito",
                familiarWeight = "5",
            ),
            """cli_execute("equip");""",
        )
        val listed = lines(out)
        assertTrue(listed.contains("Hat: helmet turtle"))
        assertTrue(listed.contains("Weapon: seal-clubbing club"))
        assertTrue(listed.contains("Off-hand: none"))
        assertTrue(listed.contains("Shirt: none"))
        assertTrue(listed.contains("Pants: none"))
        assertTrue(listed.contains("Acc. 1: none"))
        assertTrue(listed.contains("Acc. 2: none"))
        assertTrue(listed.contains("Acc. 3: none"))
        assertTrue(listed.contains("Pet: Mosquito (5 lbs)"))
        assertTrue(listed.contains("Item: none"))
        assertFalse(listed.any { it.startsWith("Back:") })
    }

    @Test
    fun equipList_filtersByLeftover() {
        val out = outputLib(
            equippedLib(mapOf(EquipmentSlot.HAT to "helmet turtle", EquipmentSlot.WEAPON to "seal-clubbing club")),
            """cli_execute("equip list helmet");""",
        )
        val listed = lines(out)
        assertTrue(listed.contains("Hat: helmet turtle"))
        assertFalse(listed.any { it.contains("seal-clubbing") })
    }

    @Test
    fun wear_bareAlsoLists() {
        val out = outputLib(
            equippedLib(mapOf(EquipmentSlot.HAT to "helmet turtle")),
            """cli_execute("wear");""",
        )
        assertTrue(lines(out).contains("Hat: helmet turtle"))
    }

    @Test
    fun crown_printsCarryingEnthroned() {
        val out = outputLib(
            equippedLib(
                equipment = mapOf(EquipmentSlot.HAT to "Crown of Thrones"),
                enthronedName = "Mini-Hipster",
            ),
            """cli_execute("equip");""",
        )
        val listed = lines(out)
        assertTrue(listed.contains("Hat: Crown of Thrones"))
        assertTrue(listed.contains("Carrying: Mini-Hipster"))
    }

    @Test
    fun bjorn_printsBackAndCarrying() {
        val out = outputLib(
            equippedLib(
                equipment = mapOf(EquipmentSlot.CONTAINER to "Buddy Bjorn"),
                bjornedName = "Bloovian Groose",
            ),
            """cli_execute("equip");""",
        )
        val listed = lines(out)
        assertTrue(listed.contains("Back: Buddy Bjorn"))
        assertTrue(listed.contains("Carrying: Bloovian Groose"))
    }

    @Test
    fun stickers_printWhenAnyEquipped() {
        val out = outputLib(
            equippedLib(mapOf(EquipmentSlot.STICKER1 to "scratch 'n' sniff unicorn sticker")),
            """cli_execute("equip");""",
        )
        val listed = lines(out)
        assertTrue(listed.contains("Sticker 1: scratch 'n' sniff unicorn sticker"))
        assertTrue(listed.contains("Sticker 2: none"))
        assertTrue(listed.contains("Sticker 3: none"))
    }

    @Test
    fun hatTrick_printsEachHat() {
        ItemDatabase.registerForTest(
            ItemData(
                id = 9,
                name = "helmet turtle",
                descId = "d9",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        ItemDatabase.registerForTest(
            ItemData(
                id = 4,
                name = "seal-skull helmet",
                descId = "d4",
                image = "img",
                primaryUse = ItemPrimaryUse.NONE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 1,
                plural = null,
            ),
        )
        val out = outputLib(
            equippedLib(hats = listOf("9", "4"), path = "Hat Trick"),
            """cli_execute("equip");""",
        )
        val listed = lines(out)
        assertTrue(listed.contains("Hat: helmet turtle"))
        assertTrue(listed.contains("Hat: seal-skull helmet"))
    }
}
