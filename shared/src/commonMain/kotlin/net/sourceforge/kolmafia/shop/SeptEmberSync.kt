package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [SeptEmberCenserRequest.visitShop] balance + pref sync. */
object SeptEmberSync {

    const val BALANCE_CHECKED_PREF = "_septEmberBalanceChecked"
    const val AVAILABLE_EMBERS_PREF = "availableSeptEmbers"
    const val SEPTEMBER_CENSER = 11642
    const val SHOP_PATH = "shop.php?whichshop=september"

    private val TOKEN_PATTERN = Regex("""<b>You have ([\d,]+) Ember""")

    fun syncFromShopHtml(html: String, prefs: Preferences) {
        if (prefs.getBoolean(BALANCE_CHECKED_PREF, false)) return
        TOKEN_PATTERN.find(html)?.let { match ->
            val embers = match.groupValues[1].replace(",", "").toIntOrNull() ?: 0
            prefs.setInt(AVAILABLE_EMBERS_PREF, embers)
        }
        prefs.setBoolean(BALANCE_CHECKED_PREF, true)
    }

    /** Desktop [SeptEmberCenserRequest.checkBalance] proactive shop visit on login/refresh. */
    fun checkBalance(
        prefs: Preferences,
        accessibleCount: (Int) -> Int,
        isKingdomOfExploathing: Boolean,
        onVisit: () -> Unit,
    ) {
        if (prefs.getBoolean(BALANCE_CHECKED_PREF, false)) return
        if (accessibleCount(SEPTEMBER_CENSER) <= 0) return
        if (isKingdomOfExploathing) return
        onVisit()
    }
}
