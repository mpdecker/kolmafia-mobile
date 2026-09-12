package net.sourceforge.kolmafia.shop

/**
 * Desktop inventory-token coinmaster [accessible] gates
 * (Toxic Chemistry / Fishbonery / Guzzlr / Warbear / Shower Thoughts / FDKOL).
 */
object InventoryTokenShopAccessibility {

    const val TOXIC_GLOBULE = 8218
    const val FRESHWATER_FISHBONE = 7651
    const val GUZZLRBUCK = 10535
    const val WARBEAR_BLACK_BOX = 7035
    const val GLOB_OF_WET_PAPER = 11885
    const val FDKOL_COMMENDATION = 5707

    fun inaccessibleReason(nickname: String, accessibleCount: (Int) -> Int): String? =
        when (nickname.lowercase()) {
            "toxic", "toxicchemistry" ->
                if (accessibleCount(TOXIC_GLOBULE) <= 0) {
                    "You do not have a toxic globule in inventory"
                } else {
                    null
                }
            "fishbones", "fishbonery" ->
                if (accessibleCount(FRESHWATER_FISHBONE) <= 0) {
                    "You do not have a freshwater fishbone in inventory"
                } else {
                    null
                }
            "guzzlr" ->
                if (accessibleCount(GUZZLRBUCK) <= 0) {
                    "You have no Guzzlrbucks to spend"
                } else {
                    null
                }
            "warbear", "warbearbox" ->
                if (accessibleCount(WARBEAR_BLACK_BOX) <= 0) {
                    "You don't have a warbear black box"
                } else {
                    null
                }
            "showerthoughts" ->
                if (accessibleCount(GLOB_OF_WET_PAPER) <= 0) {
                    "You do not have a glob of wet paper in inventory"
                } else {
                    null
                }
            "fdkol" ->
                if (accessibleCount(FDKOL_COMMENDATION) <= 0) {
                    "You do not have an FDKOL commendation in inventory"
                } else {
                    null
                }
            else -> null
        }
}
