package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop Chroner shop [accessible] gates keyed by shop nickname / whichshop id. */
object TimeTowerAccessibility {

    private val INACCESSIBLE_MESSAGES = mapOf(
        "twitch_alliedhq" to "You can't get to the Allied HQ",
        "shakeshop" to "You can't get to Ye Newe Souvenir Shoppe",
        "shoeshop" to "You can't get to the Shoe Repair Shop",
        "twitchsoup" to "You can't get to the Primordial Soup Kitchen",
        "nina" to "You can't get to Ni\u00f1a Store",
        "caveshop" to "You can't get to the Neandermall",
        "conmerch" to "You can't get to the KoL Con 13 Merch Table",
        "applestore" to "You can't get to The Applecalypse Store",
    )

    fun inaccessibleReason(shopId: String, prefs: Preferences?): String? {
        if (prefs?.getBoolean(TimeTowerSync.PREF, false) == true) return null
        return INACCESSIBLE_MESSAGES[shopId.lowercase()]
    }
}
