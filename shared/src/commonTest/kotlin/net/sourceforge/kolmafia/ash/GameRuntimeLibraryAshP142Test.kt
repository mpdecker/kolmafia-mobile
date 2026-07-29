package net.sourceforge.kolmafia.ash

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterPurchaseAccessibility

class GameRuntimeLibraryAshP142Test {

    private fun prefs(): Preferences = Preferences(MapSettings())

    private fun replicaMaster(): CoinmasterData =
        CoinmasterData(
            masterName = "Replica Mr. Store",
            nickname = "mrreplica",
            token = "replica Mr. Accessory",
            shopId = "mrreplica",
            buyItems = emptyList(),
            sellItems = emptyList(),
        )

    @Test
    fun revision_phase176() {
        assertEquals("phase220", GameRuntimeLibrary.REVISION)
    }

    @Test
    fun coinmasterPurchase_replicaCurrentYearAvailable() {
        val p = prefs()
        p.setInt("currentReplicaStoreYear", 2023)
        assertTrue(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                replicaMaster(),
                11325,
                CharacterState(challengePath = AscensionPath.LEGACY_OF_LOATHING.apiName),
                p,
                accessibleCount = { 0 },
            ),
        )
    }

    @Test
    fun coinmasterPurchase_crimbo20ButtonOneTime() {
        assertTrue(
            CoinmasterPurchaseAccessibility.canPurchaseItem(
                CoinmasterData(
                    masterName = "Elf Food Drive",
                    nickname = "crimbo20food",
                    token = "donated food",
                    shopId = "crimbo20food",
                    buyItems = emptyList(),
                    sellItems = emptyList(),
                ),
                10691,
                CharacterState(),
                prefs(),
                accessibleCount = { 0 },
            ),
        )
    }
}
