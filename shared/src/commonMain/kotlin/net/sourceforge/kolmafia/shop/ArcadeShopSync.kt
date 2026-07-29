package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [ArcadeRequest.visitShop] locked-item pref sync. */
object ArcadeShopSync {

    const val SHOP_ID = "arcade"

    private val UNLOCKABLE_ITEMS = intArrayOf(
        4637, 4638, 4639, 4646, 4647,
    )

    private val ITEM_ROW_PATTERN = Regex("""<tr rel="(\d+)"""")

    fun applyVisitShop(
        html: String,
        url: String?,
        prefs: Preferences?,
        sessionLogger: SessionLogger?,
        state: CharacterState?,
    ) {
        if (prefs == null) return
        syncFromShopHtml(html, prefs)
    }

    fun syncFromShopHtml(html: String, prefs: Preferences) {
        for (match in ITEM_ROW_PATTERN.findAll(html)) {
            val id = match.groupValues[1].toIntOrNull() ?: continue
            if (id in UNLOCKABLE_ITEMS) {
                prefs.setBoolean("lockedItem$id", false)
            }
        }
    }
}
