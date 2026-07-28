package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Merge physical inventory counts with coinmaster visit-synced token prefs for validate probes. */
object CoinmasterSyncedTokenCount {

    private const val MR_ACCESSORY = 194
    private const val CHRONER = 7567

    fun accessibleCount(itemId: Int, prefs: Preferences?, physicalCount: Int): Int {
        val synced = syncedPrefCount(itemId, prefs)
        return if (synced == null) physicalCount else maxOf(physicalCount, synced)
    }

    private fun syncedPrefCount(itemId: Int, prefs: Preferences?): Int? =
        when (itemId) {
            MR_ACCESSORY -> prefs?.getInt(MerchTableSync.AVAILABLE_MR_A_PREF, 0)
            CHRONER -> prefs?.getInt(MerchTableSync.AVAILABLE_CHRONERS_PREF, 0)
            Crimbo23ShopSync.ELF_MPC ->
                prefs?.getInt(Crimbo23ShopSync.AVAILABLE_ELF_MPC_PREF, 0)
            Crimbo23ShopSync.PIECE_OF_12 ->
                prefs?.getInt(Crimbo23ShopSync.AVAILABLE_PIECE_OF_12_PREF, 0)
            Crimbo23ShopSync.MACHINE_PARTS ->
                prefs?.getInt(Crimbo23ShopSync.AVAILABLE_MACHINE_PARTS_PREF, 0)
            Crimbo23ShopSync.FLOTSAM ->
                prefs?.getInt(Crimbo23ShopSync.AVAILABLE_FLOTSAM_PREF, 0)
            else -> null
        }
}
