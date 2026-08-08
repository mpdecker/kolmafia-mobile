package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.ash.GameRuntimeLibrary
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterAccessibility
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.shop.FlowerTradeinAccessibility
import net.sourceforge.kolmafia.shop.ItemStack
import net.sourceforge.kolmafia.shop.ShopRow
import net.sourceforge.kolmafia.shop.TinkeringBenchPurchasedItem

class GameRuntimeLibraryAshP166Test {

    @Test
    fun revision_phase185() {
        assertEquals("phase320", GameRuntimeLibrary.REVISION)
    }

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun storageAmount_sumsCachedStorageAndFreepulls() {
        val p = Preferences(MapSettings())
        CollectionCache.save(p, Preferences.CACHED_STORAGE, mapOf(99 to 3))
        CollectionCache.save(p, Preferences.CACHED_FREEPULLS, mapOf(99 to 2))
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            private val item = ItemData(
                99,
                "haggard item",
                "desc",
                "hag.gif",
                ItemPrimaryUse.NONE,
                emptySet(),
                setOf('t', 'd'),
                0,
                null,
            )
            override fun item(id: Int): ItemData? = if (id == 99) item else null
            override fun item(name: String): ItemData? =
                if (name.equals("haggard item", ignoreCase = true)) item else null
        }
        val lib = GameRuntimeLibrary(preferences = p, gameDatabase = db)
        assertEquals("5", outputLib(lib, """print(to_string(storage_amount(to_item("haggard item"))));"""))
    }

    @Test
    fun flowerTradeinBlockedWithoutFlowers() {
        val master = CoinmasterData(
            masterName = "The Central Loathing Floral Mercantile Exchange",
            nickname = "flowertradein",
            token = "Chroner",
            shopId = "flowertradein",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        val reason = CoinmasterAccessibility.inaccessibleReason(
            master,
            CharacterState(),
            accessibleCount = { 0 },
        )
        assertEquals("You have no roses or tulips", reason)
    }

    @Test
    fun flowerTradeinAllowedWithRose() {
        val master = CoinmasterData(
            masterName = "The Central Loathing Floral Mercantile Exchange",
            nickname = "flowertradein",
            token = "Chroner",
            shopId = "flowertradein",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )
        val accessible = CoinmasterAccessibility.isAccessible(
            master,
            CharacterState(),
            accessibleCount = { if (it == FlowerTradeinAccessibility.ROSE) 1 else 0 },
        )
        assertTrue(accessible)
    }

    @Test
    fun tinkeringBenchPurchasedItem_appliesForTinkerMaster() {
        registerItem(11549, "smashed scientific equipment")
        registerItem(11550, "biphasic molecular oculus")
        val db = object : net.sourceforge.kolmafia.data.GameDatabase() {
            override fun item(id: Int): ItemData? = ItemDatabase.getById(id)
            override fun item(name: String): ItemData? = ItemDatabase.getByName(name)
        }
        val master = CoinmasterData(
            masterName = "Tinkering Bench",
            nickname = "wereprofessor_tinker",
            token = "smashed scientific equipment",
            shopId = "wereprofessor_tinker",
            buyItems = listOf(
                ShopRow(
                    rowId = 1467,
                    item = ItemStack(11550, 1),
                    costs = listOf(ItemStack(11549, 1)),
                ),
            ),
            sellItems = emptyList(),
        )
        TinkeringBenchPurchasedItem.apply(master, 11550, db)
        TinkeringBenchPurchasedItem.apply(
            CoinmasterData(
                masterName = "Internet Meme Shop",
                nickname = "bacon",
                token = "BACON",
                shopId = "bacon",
                buyItems = emptyList(),
                sellItems = emptyList(),
            ),
            11550,
            db,
        )
    }

    @Test
    fun flowerTradeinValidateBlockedWithoutAccessibleFlower() {
        registerItem(8668, "rose")
        CoinmasterDatabase.loadFromText(
            shopsText = "flowertradein\tThe Central Loathing Floral Mercantile Exchange\n",
            coinText = "The Central Loathing Floral Mercantile Exchange\tbuy\t2\trose\tROW759\n",
        )
        val p = Preferences(MapSettings())
        p.setBoolean("autoSatisfyWithCoinmasters", true)
        val state = CharacterState(meat = 100_000)
        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                8668,
                state,
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    private fun registerItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t'),
                autosellPrice = 0,
                plural = null,
            ),
        )
    }
}
