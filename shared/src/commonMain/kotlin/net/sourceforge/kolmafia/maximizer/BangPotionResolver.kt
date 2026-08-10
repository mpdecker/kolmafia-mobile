package net.sourceforge.kolmafia.maximizer

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop `AdventureResult.resolveBangPotion()` — map generic potion/slime aliases to concrete item IDs. */
object BangPotionResolver {
    fun resolveItemId(itemName: String, preferences: Preferences?): Int? {
        val prefs = preferences ?: return null
        val name = itemName.trim()
        if (name.startsWith("potion of ")) {
            for (itemId in ItemDatabase.FIRST_BANG_POTION..ItemDatabase.LAST_BANG_POTION) {
                val potion = prefs.getString("lastBangPotion$itemId", "")
                if (potion.isNotEmpty() && name.endsWith(potion)) {
                    return itemId
                }
            }
            return null
        }
        if (name.startsWith("vial of slime: ")) {
            for (itemId in ItemDatabase.FIRST_SLIME_VIAL until ItemDatabase.LAST_SLIME_VIAL) {
                val vial = prefs.getString("lastSlimeVial$itemId", "")
                if (vial.isNotEmpty() && name.endsWith(vial)) {
                    return itemId
                }
            }
            return null
        }
        return null
    }
}
