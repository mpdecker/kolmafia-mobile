package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.request.IslandRequest.parseResponse] action branch hooks. */
object IslandWarActionResponseSync {

    const val PREF_CONCERT_VISITED = "concertVisited"
    const val PREF_FARMER_ITEMS_COLLECTED = "_farmerItemsCollected"

    private const val GUNPOWDER = 2403

    fun parseActionResponse(
        url: String,
        html: String,
        preferences: Preferences,
        context: IslandWarVisitSync.IslandVisitContext,
    ): Boolean {
        val action = IslandWarVisitLogSync.getAction(url) ?: return false
        return when (action) {
            "concert" -> parseConcert(html, preferences)
            "farmer" -> parseFarmer(html, preferences)
            "pyro" -> parsePyro(html, context)
            else -> false
        }
    }

    private fun parseConcert(html: String, preferences: Preferences): Boolean {
        if (!html.contains("You acquire an effect") &&
            !html.contains("pretty much tapped out") &&
            !html.contains("You're all rocked out")
        ) {
            return false
        }
        if (preferences.getBoolean(PREF_CONCERT_VISITED, false)) {
            return false
        }
        preferences.setBoolean(PREF_CONCERT_VISITED, true)
        return true
    }

    private fun parseFarmer(html: String, preferences: Preferences): Boolean {
        if (!html.contains("Ach, here ye are") &&
            !html.contains("already got yer stuff today")
        ) {
            return false
        }
        if (preferences.getBoolean(PREF_FARMER_ITEMS_COLLECTED, false)) {
            return false
        }
        preferences.setBoolean(PREF_FARMER_ITEMS_COLLECTED, true)
        return true
    }

    private fun parsePyro(
        html: String,
        context: IslandWarVisitSync.IslandVisitContext,
    ): Boolean {
        if (!html.contains("eyes light up")) {
            return false
        }
        val count = context.itemCount(GUNPOWDER)
        if (count <= 0) {
            return false
        }
        context.consumeItem(GUNPOWDER, count)
        return true
    }
}
