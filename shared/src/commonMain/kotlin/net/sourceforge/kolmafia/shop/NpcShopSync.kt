package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop NPCPurchaseRequest shop pref sync (AshP147+). */
object NpcShopSync {

    private const val BLART = 10790
    private const val RAINPROOF_BARREL_CAULK = 10794
    private const val PUMP_GREASE = 10795

    private val SHOP_ID_PATTERN = Regex("""whichshop=([^&]+)""", RegexOption.IGNORE_CASE)
    private val STORE_ID_PATTERN = Regex("""whichstore=([^&]+)""", RegexOption.IGNORE_CASE)

    private val PIRATE_EPHEMERA_PATTERN =
        Regex("pirate (?:brochure|pamphlet|tract)", RegexOption.IGNORE_CASE)

    fun needsSync(storeKey: String): Boolean =
        when (storeKey.lowercase()) {
            "wildfire", "bartlebys", "hippy", "fwshop", "mayoclinic", "hiddentavern" -> true
            else -> false
        }

    fun applyShopVisit(
        html: String,
        url: String?,
        prefs: Preferences?,
        ascensionNumber: Int,
    ) {
        if (prefs == null) return
        if (url?.contains("ajax=1", ignoreCase = true) == true) return
        val shopId = extractShopOrStoreId(url) ?: return
        syncFromStoreHtml(shopId, html, prefs, ascensionNumber, url)
    }

    fun syncFromStoreHtml(
        storeKey: String,
        html: String,
        prefs: Preferences,
        ascensionNumber: Int,
        url: String? = null,
    ) {
        when (storeKey.lowercase()) {
            "wildfire" -> syncWildfirePrefs(html, prefs)
            "bartlebys" -> syncBartlebys(html, prefs, ascensionNumber)
            "hippy" -> syncHippy(html, prefs, ascensionNumber)
            "fwshop" -> syncFwshop(html, prefs)
            "mayoclinic" -> syncMayoclinic(html, url, prefs)
            "hiddentavern" -> syncHiddenTavern(prefs, ascensionNumber)
        }
    }

    private fun syncHiddenTavern(prefs: Preferences, ascensionNumber: Int) {
        if (prefs.getInt("hiddenTavernUnlock", -1) != ascensionNumber) {
            prefs.setInt("hiddenTavernUnlock", ascensionNumber)
        }
    }

    private fun syncBartlebys(html: String, prefs: Preferences, ascensionNumber: Int) {
        val match = PIRATE_EPHEMERA_PATTERN.find(html) ?: return
        prefs.setInt("lastPirateEphemeraReset", ascensionNumber)
        prefs.setString("lastPirateEphemera", match.value)
    }

    private fun syncHippy(html: String, prefs: Preferences, ascensionNumber: Int) {
        var side = "none"
        if (html.contains("peach", ignoreCase = true) &&
            html.contains("pear", ignoreCase = true) &&
            html.contains("plum", ignoreCase = true)
        ) {
            prefs.setInt("lastFilthClearance", ascensionNumber)
            side = "hippy"
        } else if (html.contains("bowl of rye sprouts", ignoreCase = true) &&
            html.contains("cob of corn", ignoreCase = true) &&
            html.contains("juniper berries", ignoreCase = true)
        ) {
            prefs.setInt("lastFilthClearance", ascensionNumber)
            side = "fratboy"
        }
        prefs.setString("currentHippyStore", side)
        prefs.setString("sidequestOrchardCompleted", side)
        if (html.contains("Oh, hey, boss!  Welcome back!")) {
            prefs.setBoolean("_hippyMeatCollected", true)
        }
    }

    private fun syncFwshop(html: String, prefs: Preferences) {
        if (!html.contains("<b>Combat Explosives")) return
        prefs.setBoolean("_fireworksShop", true)
        prefs.setBoolean("_fireworksShopHatBought", !html.contains("<b>Dangerous Hats"))
        prefs.setBoolean("_fireworksShopEquipmentBought", !html.contains("<b>Explosive Equipment"))
    }

    private fun syncMayoclinic(html: String, url: String?, prefs: Preferences) {
        if (!html.contains("Mayo", ignoreCase = true)) return
        if (url?.contains("ajax=1", ignoreCase = true) == true) return
        if (url?.contains("buyitem", ignoreCase = true) != true) {
            when {
                html.contains("miracle whip", ignoreCase = true) -> {
                    prefs.setBoolean("_mayoDeviceRented", false)
                    prefs.setBoolean("itemBoughtPerAscension8266", false)
                }
                html.contains("mayo lance", ignoreCase = true) -> {
                    prefs.setBoolean("_mayoDeviceRented", false)
                    prefs.setBoolean("itemBoughtPerAscension8266", true)
                }
                else -> {
                    prefs.setBoolean("_mayoDeviceRented", true)
                    prefs.setBoolean("itemBoughtPerAscension8266", true)
                }
            }
        }
        prefs.setBoolean("_mayoTankSoaked", !html.contains("Soak in the Mayo Tank"))
    }

    fun applyWildfireVisit(html: String, url: String?, prefs: Preferences?) {
        if (prefs == null) return
        val shopId = extractShopOrStoreId(url) ?: return
        if (!shopId.equals("wildfire", ignoreCase = true)) return
        if (url?.contains("ajax=1", ignoreCase = true) == true) return
        syncWildfirePrefs(html, prefs)
    }

    fun applyWildfirePurchase(html: String, url: String?, itemId: Int, prefs: Preferences?) {
        if (prefs == null) return
        val shopId = extractShopOrStoreId(url)
        if (shopId != null && !shopId.equals("wildfire", ignoreCase = true)) return
        if (!html.contains("You acquire an item")) return

        when (itemId) {
            BLART -> prefs.setBoolean("itemBoughtPerAscension10790", true)
            RAINPROOF_BARREL_CAULK -> prefs.setBoolean("itemBoughtPerAscension10794", true)
            PUMP_GREASE -> prefs.setBoolean("itemBoughtPerAscension10795", true)
        }
    }

    private fun syncWildfirePrefs(html: String, prefs: Preferences) {
        prefs.setBoolean("itemBoughtPerAscension10790", !html.contains("""<tr rel="10790">"""))
        prefs.setBoolean("itemBoughtPerAscension10794", !html.contains("""<tr rel="10794">"""))
        prefs.setBoolean("itemBoughtPerAscension10795", !html.contains("""<tr rel="10795">"""))
    }

    private fun extractShopOrStoreId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        SHOP_ID_PATTERN.find(url)?.groupValues?.getOrNull(1)?.let { return it }
        return STORE_ID_PATTERN.find(url)?.groupValues?.getOrNull(1)
    }
}
