package net.sourceforge.kolmafia.data

import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

class GameDatabaseLoadTest {

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
    fun load_prefetchesPulverizationForUnlistedEquipment() = runBlocking {
        GameDatabase().load()
        val pulver = EquipmentDatabase.getPulverization(BISHOPS_MITRE)
        assertNotEquals(-1, pulver)
        assertEquals(BISHOPS_MITRE_EXPECTED, pulver)
    }

    @Test
    fun load_standardRewardPulverizationStillExplicit() = runBlocking {
        GameDatabase().load()
        assertEquals(MOSS_MULCH, EquipmentDatabase.getPulverization(MOSS_MACE))
    }

    private companion object {
        const val BISHOPS_MITRE = 11911
        const val MOSS_MACE = 11504
        const val MOSS_MULCH = 11510
        const val BISHOPS_MITRE_EXPECTED = 0x8003F002.toInt()
    }
}
