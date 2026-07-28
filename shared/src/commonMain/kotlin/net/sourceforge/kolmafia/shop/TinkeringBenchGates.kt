package net.sourceforge.kolmafia.shop

/** Desktop [TinkeringBenchRequest.TinkeredItem] one-of-each upgrade tree gates. */
object TinkeringBenchGates {

    private enum class TinkeredItem(val itemId: Int, val nextItemId: Int = 0) {
        NONE(0),
        BIPHASIC_OCULUS(11550, 11551),
        TRIPHASIC_OCULUS(11551),
        MAGNETRON_PISTOL(11559),
        MOTION_SENSOR(11558),
        BELT_POUCH(11555, 11556),
        FANNYPACK(11556, 11557),
        UTILITY_BELT(11557),
        HIGH_TENSION(11552, 11553),
        ULTRA_HIGH_TENSION(11553, 11554),
        IRRESPONSIBLE_TENSION(11554),
        ;

        fun haveItem(accessibleCount: (Int) -> Int): Boolean =
            itemId > 0 && accessibleCount(itemId) > 0

        fun haveCreatedItem(accessibleCount: (Int) -> Int): Boolean {
            if (haveItem(accessibleCount)) return true
            if (nextItemId == 0) return false
            return find(nextItemId).haveCreatedItem(accessibleCount)
        }

        fun canMake(accessibleCount: (Int) -> Int): Boolean =
            !haveCreatedItem(accessibleCount)

        companion object {
            fun find(itemId: Int): TinkeredItem =
                entries.firstOrNull { it.itemId == itemId } ?: NONE
        }
    }

    fun canMakeItem(itemId: Int, accessibleCount: (Int) -> Int): Boolean =
        TinkeredItem.find(itemId).canMake(accessibleCount)
}
