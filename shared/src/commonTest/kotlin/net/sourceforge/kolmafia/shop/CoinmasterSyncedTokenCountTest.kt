package net.sourceforge.kolmafia.shop

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import net.sourceforge.kolmafia.preferences.Preferences

class CoinmasterSyncedTokenCountTest {

    @Test
    fun usesMerchTableChronerPrefWhenHigherThanPhysical() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(MerchTableSync.AVAILABLE_CHRONERS_PREF, 2000)
        assertEquals(
            2000,
            CoinmasterSyncedTokenCount.accessibleCount(7567, prefs, physicalCount = 0),
        )
    }

    @Test
    fun usesPhysicalCountWhenHigherThanMerchTablePref() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(MerchTableSync.AVAILABLE_CHRONERS_PREF, 5)
        assertEquals(
            100,
            CoinmasterSyncedTokenCount.accessibleCount(7567, prefs, physicalCount = 100),
        )
    }

    @Test
    fun usesCrimbo23ElfMpcPref() {
        val prefs = Preferences(MapSettings())
        prefs.setInt(Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, 12)
        assertEquals(
            12,
            CoinmasterSyncedTokenCount.accessibleCount(Crimbo23ShopSync.ELF_MPC, prefs, 0),
        )
    }

    @Test
    fun passesThroughUnmappedItemIds() {
        val prefs = Preferences(MapSettings())
        assertEquals(7, CoinmasterSyncedTokenCount.accessibleCount(999, prefs, 7))
    }
}
