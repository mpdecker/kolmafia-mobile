package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import kotlin.test.Test
import kotlin.test.assertEquals

/** Minimal GameDatabase stub that returns controlled items and NPC prices without loading from disk. */
private class StubPricingDatabase(
    private val fakeItemName: String? = null,
    private val fakeAutosellPrice: Int = 0,
    private val fakeNpcPrice: Int = 0,
) : GameDatabase() {

    private val fakeItem: ItemData? = fakeItemName?.let {
        ItemData(
            id = 1,
            name = it,
            descId = "desc",
            image = "item.gif",
            primaryUse = ItemPrimaryUse.NONE,
            secondaryUses = emptySet(),
            access = setOf('t', 'd'),
            autosellPrice = fakeAutosellPrice,
            plural = null
        )
    }

    override fun item(name: String): ItemData? =
        if (name.equals(fakeItemName, ignoreCase = true)) fakeItem else null

    override fun npcPrice(itemName: String): Int =
        if (itemName.equals(fakeItemName, ignoreCase = true)) fakeNpcPrice else 0
}

class GameRuntimeLibraryPricingTest {

    @Test
    fun autosellPrice_knownItem_returnsCorrectPrice() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubPricingDatabase(
                fakeItemName = "seal tooth",
                fakeAutosellPrice = 75,
                fakeNpcPrice = 0
            )
        )
        assertEquals("75",
            outputLib(lib, """print(to_string(autosell_price(to_item("seal tooth"))));"""))
    }

    @Test
    fun autosellPrice_unknownItem_returnsZero() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubPricingDatabase(
                fakeItemName = "seal tooth",
                fakeAutosellPrice = 75,
                fakeNpcPrice = 0
            )
        )
        assertEquals("0",
            outputLib(lib, """print(to_string(autosell_price(to_item("no such item"))));"""))
    }

    @Test
    fun npcPrice_knownItem_returnsCorrectPrice() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubPricingDatabase(
                fakeItemName = "anti-anti-antidote",
                fakeAutosellPrice = 5,
                fakeNpcPrice = 42
            )
        )
        assertEquals("42",
            outputLib(lib, """print(to_string(npc_price(to_item("anti-anti-antidote"))));"""))
    }

    @Test
    fun npcPrice_itemNotSoldByNpcs_returnsZero() {
        val lib = GameRuntimeLibrary(
            gameDatabase = StubPricingDatabase(
                fakeItemName = "anti-anti-antidote",
                fakeAutosellPrice = 5,
                fakeNpcPrice = 42
            )
        )
        assertEquals("0",
            outputLib(lib, """print(to_string(npc_price(to_item("some other item"))));"""))
    }
}
