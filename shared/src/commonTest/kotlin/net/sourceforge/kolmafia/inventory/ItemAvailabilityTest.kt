package net.sourceforge.kolmafia.inventory

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.DynamicItemModifierSync
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

class ItemAvailabilityTest {

    @AfterTest
    fun tearDown() {
        ItemDatabase.resetForTest()
        NpcStoreDatabase.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    private fun prefs(configure: Preferences.() -> Unit = {}): Preferences =
        Preferences(MapSettings()).also(configure)

    private fun stubDb(vararg items: ItemData): GameDatabase =
        object : GameDatabase() {
            override fun item(name: String): ItemData? =
                items.firstOrNull { it.name.equals(name, ignoreCase = true) }
        }

    private fun testItem(
        id: Int,
        name: String,
        tradeable: Boolean = false,
    ): ItemData {
        val item = ItemData(
            id = id,
            name = name,
            descId = "desc$id",
            image = "test.gif",
            primaryUse = ItemPrimaryUse.ACCESSORY,
            secondaryUses = emptySet(),
            access = if (tradeable) setOf('t') else emptySet(),
            autosellPrice = 0,
            plural = null,
        )
        ItemDatabase.registerForTest(item)
        return item
    }

    private fun emptyContext() = DynamicItemModifierSync.CheckContext(
        inventoryItemIds = emptySet(),
        equippedItemNames = emptySet(),
        activeEffectNames = emptySet(),
    )

    @Test
    fun itemInInventory_availableRegardlessOfPrefs() {
        val item = testItem(1, "test ring")
        val db = stubDb(item)
        val p = prefs()
        val context = emptyContext().copy(inventoryItemIds = setOf(1))
        assertTrue(ItemAvailability.itemAvailable(1, "test ring", context, p, db))
    }

    @Test
    fun tradeableItem_mallPrefOn_availableViaMall() {
        val item = testItem(2, "mall ring", tradeable = true)
        val db = stubDb(item)
        val p = prefs { setBoolean("autoSatisfyWithMall", true) }
        assertTrue(ItemAvailability.itemAvailable(2, "mall ring", emptyContext(), p, db))
    }

    @Test
    fun tradeableItem_mallPrefOff_notAvailableViaMall() {
        val item = testItem(2, "mall ring", tradeable = true)
        val db = stubDb(item)
        val p = prefs { setBoolean("autoSatisfyWithMall", false) }
        assertFalse(ItemAvailability.itemAvailable(2, "mall ring", emptyContext(), p, db))
    }

    @Test
    fun npcStoreItem_npcPrefOn_available() {
        val item = testItem(3, "ye olde golde frontes")
        val db = stubDb(item)
        NpcStoreDatabase.loadFromText(
            """
            2
            Shadowy Store	guildstore1	ye olde golde frontes	1500	ROW522
            """.trimIndent(),
        )
        val p = prefs { setBoolean("autoSatisfyWithNPCs", true) }
        assertTrue(ItemAvailability.itemAvailable(3, "ye olde golde frontes", emptyContext(), p, db))
    }

    @Test
    fun coinmasterBuyRow_coinmasterPrefOn_available() {
        val item = testItem(3485, "bounty-hunting rifle")
        val db = stubDb(item)
        CoinmasterDatabase.loadFromText(
            """
            hunter	Bounty Hunter Hunter	COIN
            """.trimIndent(),
            """
            Bounty Hunter Hunter	buy	15	bounty-hunting rifle
            """.trimIndent(),
        )
        val p = prefs { setBoolean("autoSatisfyWithCoinmasters", true) }
        assertTrue(ItemAvailability.itemAvailable(3485, "bounty-hunting rifle", emptyContext(), p, db))
    }

    @Test
    fun spelunkyLimitMode_blocksMallEvenWithPref() {
        val item = testItem(2, "mall ring", tradeable = true)
        val db = stubDb(item)
        val p = prefs { setBoolean("autoSatisfyWithMall", true) }
        val context = emptyContext().copy(limitMode = "spelunky")
        assertFalse(ItemAvailability.itemAvailable(2, "mall ring", context, p, db))
    }

    @Test
    fun closetItem_closetPrefOn_available() {
        val item = testItem(4, "closet ring")
        val db = stubDb(item)
        val p = prefs { setBoolean("autoSatisfyWithCloset", true) }
        val context = emptyContext().copy(closetItemIds = setOf(4))
        assertTrue(ItemAvailability.itemAvailable(4, "closet ring", context, p, db))
    }

    @Test
    fun closetItem_edLimitMode_blocksClosetEvenWithPref() {
        val item = testItem(4, "closet ring")
        val db = stubDb(item)
        val p = prefs { setBoolean("autoSatisfyWithCloset", true) }
        val context = emptyContext().copy(closetItemIds = setOf(4), limitMode = "edunder")
        assertFalse(ItemAvailability.itemAvailable(4, "closet ring", context, p, db))
    }

    @Test
    fun storageItem_storagePrefOn_available() {
        val item = testItem(5, "stored ring")
        val db = stubDb(item)
        val p = prefs { setBoolean("autoSatisfyWithStorage", true) }
        val context = emptyContext().copy(storageItemIds = setOf(5), canInteract = true)
        assertTrue(ItemAvailability.itemAvailable(5, "stored ring", context, p, db))
    }

    @Test
    fun stashItem_stashPrefOnAndHasClan_available() {
        val item = testItem(6, "stash ring")
        val db = stubDb(item)
        val p = prefs { setBoolean("autoSatisfyWithStash", true) }
        val context = emptyContext().copy(stashItemIds = setOf(6), hasClan = true, canInteract = true)
        assertTrue(ItemAvailability.itemAvailable(6, "stash ring", context, p, db))
    }

    @Test
    fun elevenLeafClover_coinmasterPrefOn_zeroHermitStock_notAvailable() {
        val item = testItem(HermitRequest.ELEVEN_LEAF_CLOVER_ID, "11-leaf clover")
        val db = stubDb(item)
        CoinmasterDatabase.loadFromText(
            """
            hermit	Hermit	COIN
            """.trimIndent(),
            """
            Hermit	buy	3	11-leaf clover
            """.trimIndent(),
        )
        val p = prefs { setBoolean("autoSatisfyWithCoinmasters", true) }
        val context = emptyContext().copy(hermitCloverCount = 0)
        assertFalse(
            ItemAvailability.itemAvailable(
                HermitRequest.ELEVEN_LEAF_CLOVER_ID,
                "11-leaf clover",
                context,
                p,
                db,
            ),
        )
    }

    @Test
    fun elevenLeafClover_coinmasterPrefOn_withHermitStock_available() {
        val item = testItem(HermitRequest.ELEVEN_LEAF_CLOVER_ID, "11-leaf clover")
        val db = stubDb(item)
        CoinmasterDatabase.loadFromText(
            """
            hermit	Hermit	COIN
            """.trimIndent(),
            """
            Hermit	buy	3	11-leaf clover
            """.trimIndent(),
        )
        val p = prefs { setBoolean("autoSatisfyWithCoinmasters", true) }
        val context = emptyContext().copy(hermitCloverCount = 1)
        assertTrue(
            ItemAvailability.itemAvailable(
                HermitRequest.ELEVEN_LEAF_CLOVER_ID,
                "11-leaf clover",
                context,
                p,
                db,
            ),
        )
    }
}
