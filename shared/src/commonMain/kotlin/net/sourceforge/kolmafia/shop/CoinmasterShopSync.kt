package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.data.GameDatabase

/** Desktop coinmaster purchasedItem pref sync (AshP147+). */
object CoinmasterShopSync {

    private const val COSMIC_SIX_PACK = 6237

    private const val VIRAL_VIDEO = 9017
    private const val PLUS_ONE = 9020
    private const val GALLON_OF_MILK = 9021
    private const val PRINT_SCREEN = 9022
    private const val DAILY_DUNGEON_MALWARE = 9024
    private const val MINI_KIWI_INTOXICATING_SPIRITS = 11602

    private const val CHEAP_TOASTER = 637
    private const val TALES_OF_DREAD = 6423
    private const val BRASS_DREAD_FLASK = 6428
    private const val SILVER_DREAD_FLASK = 6429

    fun applyPurchasedItem(
        master: CoinmasterData,
        itemId: Int,
        prefs: Preferences?,
        gameDatabase: GameDatabase? = null,
    ) {
        if (prefs == null) return
        when (master.nickname.lowercase()) {
            "bacon" -> when (itemId) {
                VIRAL_VIDEO -> prefs.setBoolean("_internetViralVideoBought", true)
                PLUS_ONE -> prefs.setBoolean("_internetPlusOneBought", true)
                GALLON_OF_MILK -> prefs.setBoolean("_internetGallonOfMilkBought", true)
                PRINT_SCREEN -> prefs.setBoolean("_internetPrintScreenButtonBought", true)
                DAILY_DUNGEON_MALWARE -> prefs.setBoolean("_internetDailyDungeonMalwareBought", true)
            }
            "kiwi" -> {
                if (itemId == MINI_KIWI_INTOXICATING_SPIRITS) {
                    prefs.setBoolean("_miniKiwiIntoxicatingSpiritsBought", true)
                }
            }
            "shore" -> {
                if (itemId == CHEAP_TOASTER) {
                    prefs.setBoolean("itemBoughtPerAscension637", true)
                }
            }
            "dv" -> when (itemId) {
                TALES_OF_DREAD -> prefs.setBoolean("itemBoughtPerCharacter6423", true)
                BRASS_DREAD_FLASK -> prefs.setBoolean("itemBoughtPerCharacter6428", true)
                SILVER_DREAD_FLASK -> prefs.setBoolean("itemBoughtPerCharacter6429", true)
            }
            "jarl" -> {
                if (itemId == COSMIC_SIX_PACK) {
                    prefs.setBoolean("_cosmicSixPackConjured", true)
                }
            }
            "wereprofessor_tinker" -> TinkeringBenchPurchasedItem.apply(master, itemId, gameDatabase)
        }
    }
}
