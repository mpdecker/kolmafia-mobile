package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shop.NpcShopSync

/** Desktop [net.sourceforge.kolmafia.request.NPCPurchaseRequest] store.php. */
object NPCPurchaseRequest {
    fun parseResponse(url: String, html: String, preferences: Preferences?, ascensionNumber: Int) {
        if (!url.contains("store.php", ignoreCase = true)) return
        val store = Regex("""whichstore=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1) ?: return
        if (preferences != null) {
            NpcShopSync.syncFromStoreHtml(store, html, preferences, ascensionNumber, url)
        }
    }

    fun registerRequest(url: String): Boolean = url.contains("store.php", ignoreCase = true)
}
