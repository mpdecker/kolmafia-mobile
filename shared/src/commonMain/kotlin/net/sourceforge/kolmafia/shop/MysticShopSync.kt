package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [CrackpotMysticRequest.visitShop] psychosis pixel pref sync. */
object MysticShopSync {

    const val SHOP_ID = "mystic"
    const val MYSTIC_PSYCHOSIS_ITEMS_UNLOCKED = "_mysticPsychosisItemsUnlocked"

    private const val PIXEL_PILL = 5906
    private const val PIXEL_ENERGY_TANK = 5907
    private const val PIXEL_GRAPPLING_HOOK = 6173

    private val PSYCHOSIS_PIXEL_ITEMS = intArrayOf(
        PIXEL_PILL,
        PIXEL_ENERGY_TANK,
        PIXEL_GRAPPLING_HOOK,
    )

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
        val unlocked = PSYCHOSIS_PIXEL_ITEMS.any { itemId ->
            html.contains("""<tr rel="$itemId"""", ignoreCase = true) ||
                when (itemId) {
                    PIXEL_PILL -> html.contains("pixel pill", ignoreCase = true)
                    PIXEL_ENERGY_TANK -> html.contains("pixel energy tank", ignoreCase = true)
                    PIXEL_GRAPPLING_HOOK -> html.contains("pixel grappling hook", ignoreCase = true)
                    else -> false
                }
        }
        if (unlocked) {
            prefs.setBoolean(MYSTIC_PSYCHOSIS_ITEMS_UNLOCKED, true)
        }
    }
}
