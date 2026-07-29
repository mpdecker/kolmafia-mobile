package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [FiveDPrinterRequest.visitShop] unknown-recipe pref sync. */
object FiveDPrinterShopSync {

    const val SHOP_ID = "5dprinter"

    private val DESC_ITEM_PATTERN = Regex("""descitem\((\d+)\)""")

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
        for (match in DESC_ITEM_PATTERN.findAll(html)) {
            val descId = match.groupValues[1]
            val itemId = ItemDatabase.getByDescId(descId)?.id ?: continue
            if (itemId <= 0 || itemId !in FiveDPrinterAccessibility.UNKNOWN_RECIPE_ITEMS) continue
            val prefKey = "unknownRecipe$itemId"
            if (prefs.getBoolean(prefKey, true)) {
                prefs.setBoolean(prefKey, false)
            }
        }
    }
}
