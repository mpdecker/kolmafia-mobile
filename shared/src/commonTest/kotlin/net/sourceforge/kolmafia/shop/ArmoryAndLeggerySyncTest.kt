package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.data.StandardRewardDatabase
import net.sourceforge.kolmafia.preferences.Preferences

class ArmoryAndLeggerySyncTest {

    @AfterTest
    fun cleanup() {
        CoinmasterDatabase.resetForTest()
        StandardRewardDatabase.resetForTest()
    }

    @Test
    fun visitSyncLearnsUnknownRowAndPulverizedCurrency() {
        registerItems()
        StandardRewardDatabase.loadFromText(
            """
                11528	2025	hard	SC	UNKNOWN	petrified wood war pike
            """.trimIndent(),
            """
                11526	2025	norm	crepe paper pared cuttings
            """.trimIndent(),
        )
        val prefs = Preferences(MapSettings())
        ArmoryAndLeggerySync.syncFromShopHtml(
            """
                <tr rel="11528">
                <a onClick='javascript:descitem(11528)'><b>petrified wood war pike</b></a>
                <span title="petrified wood waste parts"><b>1</b></span>
                <form action="shop.php?action=buy&whichshop=armory&whichrow=1700">
                </tr>
            """.trimIndent(),
            prefs,
            force = true,
        )

        val reward = StandardRewardDatabase.findStandardReward(11528)
        assertNotNull(reward)
        assertEquals("1700", reward.row)
        assertNotNull(StandardRewardDatabase.findStandardPulverized(PETRIFIED_WOOD))
        assertTrue(CoinmasterDatabase.findBuyRowForItem(11528) != null)
    }

    private fun registerItems() {
        registerTestItem(11528, "petrified wood war pike")
        registerTestItem(PETRIFIED_WOOD, "petrified wood waste parts")
    }

    private fun registerTestItem(id: Int, name: String) {
        ItemDatabase.registerForTest(
            ItemData(
                id = id,
                name = name,
                descId = "d$id",
                image = "img",
                primaryUse = ItemPrimaryUse.USABLE,
                secondaryUses = emptySet(),
                access = setOf('t', 'd'),
                autosellPrice = 1,
                plural = null,
            ),
        )
    }

    companion object {
        private const val PETRIFIED_WOOD = 11534
    }
}
