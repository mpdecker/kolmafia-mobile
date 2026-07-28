package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

class EquipmentDatabaseDerivePulverizationTest {

    @AfterTest
    fun tearDown() {
        EquipmentDatabase.resetForTest()
        ItemDatabase.resetForTest()
        ModifierDatabase.resetForTest()
        NpcStoreDatabase.resetForTest()
        StandardRewardDatabase.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun isPulverizable_equipmentTypesOnly() = runBlocking {
        loadBundledData()
        assertTrue(EquipmentDatabase.isPulverizable(BISHOPS_MITRE))
        assertTrue(EquipmentDatabase.isPulverizable(POTTERY_HAT))
        assertFalse(EquipmentDatabase.isPulverizable(47))
    }

    @Test
    fun derive_bishopsMitre_multiElementYield1P() = runBlocking {
        loadBundledData()
        assertEquals(BISHOPS_MITRE_EXPECTED, EquipmentDatabase.derivePulverization(BISHOPS_MITRE))
    }

    @Test
    fun derive_potteryHat_hotResistanceAndDamageYield2P() = runBlocking {
        loadBundledData()
        assertEquals(POTTERY_HAT_EXPECTED, EquipmentDatabase.derivePulverization(POTTERY_HAT))
    }

    @Test
    fun getPulverization_lazyCachesDerivedValue() = runBlocking {
        loadBundledData()
        val first = EquipmentDatabase.getPulverization(POTTERY_HAT)
        val second = EquipmentDatabase.getPulverization(POTTERY_HAT)
        assertEquals(POTTERY_HAT_EXPECTED, first)
        assertEquals(first, second)
    }

    @Test
    fun getPulverization_standardRewardExplicitMapWins() = runBlocking {
        loadBundledData()
        assertEquals(MOSS_MULCH, EquipmentDatabase.getPulverization(MOSS_MACE))
    }

    @Test
    fun derive_giftOnlyItem_returnsUselessPowder() {
        val itemId = 99001
        ItemDatabase.registerForTest(
            ItemData(
                id = itemId,
                name = "gift-only hat",
                descId = "gift_hat",
                image = "hat.gif",
                primaryUse = ItemPrimaryUse.HAT,
                secondaryUses = emptySet(),
                access = setOf('g'),
                autosellPrice = 0,
                plural = null,
            ),
        )
        EquipmentDatabase.registerForTest(
            itemId,
            EquipmentData("gift-only hat", power = 50, statRequirement = null, hands = 0, itemType = null),
        )
        assertEquals(EquipmentDatabase.USELESS_POWDER, EquipmentDatabase.derivePulverization(itemId))
    }

    @Test
    fun derive_npcStoreItem_returnsUselessPowder() {
        val itemId = 99002
        ItemDatabase.registerForTest(
            ItemData(
                id = itemId,
                name = "npc shop hat",
                descId = "npc_hat",
                image = "hat.gif",
                primaryUse = ItemPrimaryUse.HAT,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 0,
                plural = null,
            ),
        )
        EquipmentDatabase.registerForTest(
            itemId,
            EquipmentData("npc shop hat", power = 50, statRequirement = null, hands = 0, itemType = null),
        )
        NpcStoreDatabase.loadFromText(
            """
            Test Shop	testshop	npc shop hat	100
            """.trimIndent(),
        )
        assertEquals(EquipmentDatabase.USELESS_POWDER, EquipmentDatabase.derivePulverization(itemId))
    }

    private suspend fun loadBundledData() {
        GameDatabase().load()
    }

    private companion object {
        const val BISHOPS_MITRE = 11911
        const val POTTERY_HAT = 4682
        const val MOSS_MACE = 11504
        const val MOSS_MULCH = 11510

        const val BISHOPS_MITRE_EXPECTED = 0x8003F002.toInt()
        const val POTTERY_HAT_EXPECTED = 0x80013004.toInt()
    }
}
