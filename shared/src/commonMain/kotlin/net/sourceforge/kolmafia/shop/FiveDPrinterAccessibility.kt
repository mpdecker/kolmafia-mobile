package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop FiveDPrinterRequest accessible / canBuyItem gates. */
object FiveDPrinterAccessibility {

    const val FIVE_D_PRINTER = 7750

    val UNKNOWN_RECIPE_ITEMS = intArrayOf(
        7752, // XIBLAXIAN_XENO_GOGGLES
        7753, // XIBLAXIAN_STEALTH_COWL
        7754, // XIBLAXIAN_STEALTH_TROUSERS
        7755, // XIBLAXIAN_STEALTH_VEST
        7756, // XIBLAXIAN_ULTRABURRITO
        7757, // XIBLAXIAN_SPACE_WHISKEY
        7758, // XIBLAXIAN_RESIDENCE_CUBE
    )

    fun isShopAccessible(accessibleCount: (Int) -> Int): Boolean =
        accessibleCount(FIVE_D_PRINTER) > 0

    fun isItemAvailable(itemId: Int, prefs: Preferences?): Boolean {
        if (itemId !in UNKNOWN_RECIPE_ITEMS) return true
        return prefs?.getBoolean("unknownRecipe$itemId", true) != true
    }
}
