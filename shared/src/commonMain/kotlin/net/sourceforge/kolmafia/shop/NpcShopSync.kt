package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop NPCPurchaseRequest variable shop HTML sync (bartlebys ephemera + hippy filth clearance). */
object NpcShopSync {

    private val PIRATE_EPHEMERA = Regex("pirate (?:brochure|pamphlet|tract)", RegexOption.IGNORE_CASE)

    private val VARIABLE_SHOPS = setOf("bartlebys", "hippy")

    fun needsSync(shopId: String): Boolean =
        shopId.lowercase() in VARIABLE_SHOPS

    fun syncFromStoreHtml(
        shopId: String,
        html: String,
        prefs: Preferences,
        ascensionNumber: Int,
    ) {
        when (shopId.lowercase()) {
            "bartlebys" -> syncBartlebys(html, prefs, ascensionNumber)
            "hippy" -> syncHippy(html, prefs, ascensionNumber)
        }
    }

    private fun syncBartlebys(html: String, prefs: Preferences, ascensionNumber: Int) {
        val match = PIRATE_EPHEMERA.find(html) ?: return
        prefs.setInt("lastPirateEphemeraReset", ascensionNumber)
        prefs.setString("lastPirateEphemera", match.value)
    }

    private fun syncHippy(html: String, prefs: Preferences, ascensionNumber: Int) {
        val side = when {
            html.contains("peach") &&
                html.contains("pear") &&
                html.contains("plum") -> "hippy"
            html.contains("bowl of rye sprouts") &&
                html.contains("cob of corn") &&
                html.contains("juniper berries") -> "fratboy"
            else -> return
        }
        prefs.setInt("lastFilthClearance", ascensionNumber)
        prefs.setString("currentHippyStore", side)
        prefs.setString("sidequestOrchardCompleted", side)
    }
}
