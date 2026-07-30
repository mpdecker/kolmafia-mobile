package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemData
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.StoragePullRules
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.shop.CoinmasterDatabase
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseProbe
import net.sourceforge.kolmafia.shop.CoinmasterVisitInventory
import net.sourceforge.kolmafia.shop.FlowerTradeinAccessibility
import net.sourceforge.kolmafia.shop.FlowerTradeinSync

class GameRuntimeLibraryAshP168Test {

    @AfterTest
    fun cleanup() {
        CoinmasterVisitInventory.resetForTest()
        CoinmasterDatabase.resetForTest()
    }

    @Test
    fun revision_phase186() {
        assertEquals("phase247", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun flowerTradeinValidateBlockedBeforeVisitSync() {
        registerChronerShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)

        assertFalse(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                FlowerTradeinSync.CHRONER,
                net.sourceforge.kolmafia.character.CharacterState(meat = 100_000),
                prefs,
                accessibleCount = { if (it == FlowerTradeinAccessibility.ROSE) 1 else 0 },
            ),
        )
    }

    @Test
    fun flowerTradeinValidateAllowedAfterVisitSyncWithRose() {
        registerChronerShop()
        val prefs = Preferences(MapSettings())
        prefs.setBoolean("autoSatisfyWithCoinmasters", true)
        FlowerTradeinSync.syncFromShopHtml(
            """
                <tr rel="7567">
                <a onClick='javascript:descitem(7567)'><b>Chroner</b></a>
                <span title="rose"><b>1</b></span>
                <form action="shop.php?action=buy&whichshop=flowertradein&whichrow=759">
                </tr>
            """.trimIndent(),
            prefs,
        )

        assertTrue(
            CoinmasterPurchaseProbe.canPurchaseIgnoringMeat(
                FlowerTradeinSync.CHRONER,
                net.sourceforge.kolmafia.character.CharacterState(meat = 100_000),
                prefs,
                accessibleCount = { if (it == FlowerTradeinAccessibility.ROSE) 1 else 0 },
            ),
        )
    }

    @Test
    fun getStorage_updatesBothStorageAndFreepullCaches() {
        val fakeStorage = object : StorageRequest(
            HttpClient(MockEngine { respond("") }),
        ) {
            override suspend fun fetchClassifiedContents(
                characterState: net.sourceforge.kolmafia.character.CharacterState?,
                prefs: Preferences?,
            ): StoragePullRules.StorageContents =
                StoragePullRules.StorageContents(
                    storage = mapOf(7566 to 3),
                    freepulls = mapOf(7566 to 2),
                )
        }
        val db = object : GameDatabase() {
            private val toolbelt = ItemData(
                7566,
                "time-twitching toolbelt",
                "desc",
                "belt.gif",
                ItemPrimaryUse.NONE,
                emptySet(),
                setOf('t', 'd'),
                0,
                null,
            )
            override fun item(id: Int): ItemData? = if (id == 7566) toolbelt else null
            override fun item(name: String): ItemData? =
                if (name.equals("time-twitching toolbelt", ignoreCase = true)) toolbelt else null
        }
        val p = Preferences(MapSettings())
        val lib = GameRuntimeLibrary(storageRequest = fakeStorage, gameDatabase = db, preferences = p)
        assertEquals("1", outputLib(lib, "print(to_string(count(get_storage())));"))
        assertEquals(3, CollectionCache.load(p, Preferences.CACHED_STORAGE)[7566])
        assertEquals(2, CollectionCache.load(p, Preferences.CACHED_FREEPULLS)[7566])
    }

    private fun registerChronerShop() {
        CoinmasterDatabase.loadFromText(
            shopsText = "flowertradein\tThe Central Loathing Floral Mercantile Exchange\n",
            coinText = """
                The Central Loathing Floral Mercantile Exchange	buy	1	Chroner	ROW759
            """.trimIndent(),
        )
    }
}
