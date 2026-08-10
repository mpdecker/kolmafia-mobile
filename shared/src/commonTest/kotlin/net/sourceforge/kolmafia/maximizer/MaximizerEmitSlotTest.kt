package net.sourceforge.kolmafia.maximizer

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionData
import net.sourceforge.kolmafia.data.ConcoctionIngredient
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.preferences.Preferences

class MaximizerEmitSlotTest {

    private val stubDb = object : GameDatabase() {
        override fun item(id: Int): ItemData? = ItemDatabase.getById(id)
        override fun item(name: String): ItemData? = ItemDatabase.getByName(name)
    }

    @AfterTest
    fun cleanup() {
        ItemDatabase.resetForTest()
        ConcoctionDatabase.resetForTest()
    }

    @Test
    fun emit_keepUnchangedSlot() {
        registerEquipment(7001, "kept hat")
        val charState = CharacterState(
            equipment = mapOf(EquipmentSlot.HAT to "kept hat"),
        )
        val boosts = emitForSlot(
            slot = EquipmentSlot.HAT,
            itemName = "kept hat",
            charState = charState,
        )
        assertEquals(1, boosts.size)
        assertEquals("keep HAT: kept hat", boosts.single().text)
        assertEquals("", boosts.single().cmd)
    }

    @Test
    fun emit_closetOnlyItemUsesUnclosetChain() {
        registerEquipment(7002, "closet hat")
        val boosts = emitForSlot(
            slot = EquipmentSlot.HAT,
            itemName = "closet hat",
            inventoryCount = { 0 },
            closetContents = mapOf(7002 to 1),
        )
        assertEquals(1, boosts.size)
        val boost = boosts.single()
        assertTrue(boost.text.contains("uncloset & equip HAT closet hat", ignoreCase = true), boost.text)
        assertTrue(boost.cmd.startsWith("closet take 1 \u00B67002;equip HAT \u00B67002"), boost.cmd)
    }

    @Test
    fun emit_storagePullUsesPullChain() {
        registerEquipment(7003, "stored hat")
        val boosts = emitForSlot(
            slot = EquipmentSlot.HAT,
            itemName = "stored hat",
            inventoryCount = { 0 },
            storageContents = mapOf(7003 to 1),
        )
        assertEquals(1, boosts.size)
        val boost = boosts.single()
        assertTrue(boost.text.contains("pull & equip HAT stored hat", ignoreCase = true), boost.text)
        assertTrue(boost.cmd.startsWith("pull \u00B67003;equip HAT \u00B67003"), boost.cmd)
    }

    @Test
    fun emit_creatableUsesMakeChainWithAdventureSuffix() {
        registerEquipment(7010, "craft hat")
        registerItem(7011, "leaf", ItemPrimaryUse.USABLE)
        ConcoctionDatabase.injectForTest(
            ConcoctionData(
                result = "craft hat",
                resultQuantity = 1,
                methods = setOf("COMBINE"),
                ingredients = listOf(ConcoctionIngredient("leaf", 1)),
            ),
        )
        val boosts = emitForSlot(
            slot = EquipmentSlot.HAT,
            itemName = "craft hat",
            spec = MaximizeSpec(
                primary = DoubleModifier.MYS,
                evaluator = Evaluator("mys"),
                allowCreatable = true,
            ),
            inventoryCount = { id -> if (id == 7011) 5 else 0 },
            charState = CharacterState(adventuresLeft = 10),
        )
        assertEquals(1, boosts.size)
        val boost = boosts.single()
        assertTrue(boost.text.contains("make & equip HAT craft hat", ignoreCase = true), boost.text)
        assertTrue(boost.cmd.startsWith("make \u00B67010;equip HAT \u00B67010"), boost.cmd)
    }

    @Test
    fun emit_garbageChampagneZeroChargeUsesFoldPrefix() {
        registerEquipment(MaximizerGarbageAuto.BROKEN_CHAMPAGNE_ID, "broken champagne bottle", ItemPrimaryUse.OFFHAND)
        val prefs = Preferences(MapSettings()).apply {
            setInt("garbageChampagneCharge", 0)
            setBoolean("_garbageItemChanged", false)
        }
        val boosts = emitForSlot(
            slot = EquipmentSlot.OFFHAND,
            itemName = "broken champagne bottle",
            spec = MaximizeSpec(DoubleModifier.ITEMDROP, Evaluator("item")),
            inventoryCount = { id ->
                if (id == MaximizerGarbageAuto.BROKEN_CHAMPAGNE_ID) 1 else 0
            },
            preferences = prefs,
            charState = CharacterState(
                equipment = mapOf(EquipmentSlot.OFFHAND to "other offhand"),
            ),
        )
        assertEquals(1, boosts.size)
        val boost = boosts.single()
        assertTrue(boost.text.contains("fold & equip OFFHAND broken champagne bottle", ignoreCase = true), boost.text)
        assertTrue(boost.cmd.contains("fold \u00B6${MaximizerGarbageAuto.BROKEN_CHAMPAGNE_ID}"), boost.cmd)
    }

    @Test
    fun emit_priceLevelAllSkipsExpensiveTradeableWithClosetCopy() {
        registerEquipment(7020, "mall hat")
        val mallPrices = MallPriceManager()
        mallPrices.cachePrice(7020, price = 50_000L, quantity = 1, shopId = 1)
        val boosts = emitForSlot(
            slot = EquipmentSlot.HAT,
            itemName = "mall hat",
            inventoryCount = { 0 },
            closetContents = mapOf(7020 to 1),
            spec = MaximizeSpec(
                primary = DoubleModifier.MYS,
                evaluator = Evaluator("mys"),
                maxPrice = 1000,
            ),
            mallPriceManager = mallPrices,
            priceLevel = MaximizerPriceLevel.ALL,
        )
        assertTrue(boosts.isEmpty())
    }

    private fun emitForSlot(
        slot: EquipmentSlot,
        itemName: String,
        spec: MaximizeSpec = MaximizeSpec(DoubleModifier.MYS, Evaluator("mys")),
        charState: CharacterState = CharacterState(),
        inventoryCount: (Int) -> Int = { id ->
            val itemId = ItemDatabase.getByName(itemName)?.id
            if (itemId != null && id == itemId) 1 else 0
        },
        closetContents: Map<Int, Int> = emptyMap(),
        storageContents: Map<Int, Int> = emptyMap(),
        preferences: Preferences? = null,
        mallPriceManager: MallPriceManager? = null,
        priceLevel: MaximizerPriceLevel = MaximizerPriceLevel.DONT_CHECK,
    ): List<MaximizerBoost> {
        val itemId = ItemDatabase.getByName(itemName)?.id ?: error("missing item $itemName")
        val scoreBefore = 0.0
        val scoreAfter = 10.0
        return MaximizerEmitSlot.buildBoosts(
            MaximizerEmitSlot.Context(
                plan = MaximizerEmitSlot.Plan(
                    goal = "mys",
                    spec = spec,
                    scoreBefore = scoreBefore,
                    scoreAfter = scoreAfter,
                    bestPerSlot = mapOf(slot to (itemName to scoreAfter)),
                ),
                charState = charState,
                inventory = MaximizerEmitSlot.InventorySnapshot(
                    closetContents = closetContents,
                    storageContents = storageContents,
                ),
                inventoryCount = inventoryCount,
                gameDatabase = stubDb,
                preferences = preferences,
                mallPriceManager = mallPriceManager,
                priceLevel = priceLevel,
                equipScope = MaximizerEquipScope.SPECULATE,
            ),
        ).filter { it.slot == slot }
    }

    private fun registerEquipment(id: Int, name: String, primaryUse: ItemPrimaryUse = ItemPrimaryUse.HAT) {
        registerItem(id, name, primaryUse)
    }

    private fun registerItem(id: Int, name: String, primaryUse: ItemPrimaryUse) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = primaryUse,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }
}
