package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop town place unlock writers: [QuestManager.handleTownRightChange],
 * [QuestManager.handleTownWrongChange], [QuestManager.handleTownMarketChange],
 * [QuestManager.handleSpeakeasyChange], eldritch flags from [QuestManager.handleTownChange].
 */
object TownUnlockSync {

    private val speakeasyNamePattern = Regex(
        """div id=town_speakeasyname.*?title="(.*?)"""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun applyFromTownRight(
        url: String?,
        html: String,
        preferences: Preferences?,
        inBadMoon: Boolean = false,
    ): Boolean {
        if (preferences == null) return false
        if (url != null && !url.contains("whichplace=town_right", ignoreCase = true)) return false
        var changed = false
        val hasAction = url.orEmpty().contains("action", ignoreCase = true)
        if (!hasAction && !inBadMoon) {
            if (html.contains("Voting Booth") && !preferences.getBoolean("voteAlways", false)) {
                preferences.setBoolean("_voteToday", true)
                changed = true
            }
        }
        if (html.contains("Horsery")) {
            preferences.setBoolean("horseryAvailable", true)
            changed = true
        }
        if (html.contains("Telegraph Office") &&
            !preferences.getBoolean("telegraphOfficeAvailable", false)
        ) {
            preferences.setBoolean("_telegraphOfficeToday", true)
            changed = true
        }
        if (html.contains("Madness Bakery")) {
            preferences.setBoolean("madnessBakeryAvailable", true)
            changed = true
        }
        return changed
    }

    fun applyFromTownWrong(
        url: String?,
        html: String,
        preferences: Preferences?,
        inBadMoon: Boolean = false,
    ): Boolean {
        if (preferences == null) return false
        if (url != null && !url.contains("whichplace=town_wrong", ignoreCase = true)) return false
        val hasAction = url.orEmpty().contains("action", ignoreCase = true)
        if (hasAction || inBadMoon) return false
        var changed = false
        preferences.setBoolean("hasDetectiveSchool", html.contains("Precinct"))
        changed = true
        if (html.contains("The Neverending Party") &&
            !preferences.getBoolean("neverendingPartyAlways", false) &&
            !preferences.getBoolean("replicaNeverendingPartyAlways", false)
        ) {
            preferences.setBoolean("_neverendingPartyToday", true)
        }
        if (preferences.getInt("_neverendingPartyFreeTurns", 0) < 10 &&
            html.contains("The Neverending Party (1)")
        ) {
            preferences.setInt("_neverendingPartyFreeTurns", 10)
        }
        if (html.contains("Boxing Daycare") && !preferences.getBoolean("daycareOpen", false)) {
            preferences.setBoolean("_daycareToday", true)
        }
        if (html.contains("Tunnel of L.O.V.E.") &&
            !preferences.getBoolean("loveTunnelAvailable", false)
        ) {
            preferences.setBoolean("_loveTunnelToday", true)
        }
        if (html.contains("Speakeasy")) {
            speakeasyNamePattern.find(html)?.groupValues?.getOrNull(1)?.let {
                preferences.setString("speakeasyName", it)
            }
            preferences.setBoolean("ownsSpeakeasy", true)
        }
        if (html.contains("Overgrown Lot")) {
            preferences.setBoolean("overgrownLotAvailable", true)
        }
        return changed
    }

    fun applyFromTownMarket(
        url: String?,
        html: String,
        preferences: Preferences?,
        inBadMoon: Boolean = false,
    ): Boolean {
        if (preferences == null) return false
        if (url != null && !url.contains("whichplace=town_market", ignoreCase = true)) return false
        val hasAction = url.orEmpty().contains("action", ignoreCase = true)
        if (hasAction || inBadMoon) return false
        if (!html.contains("The Skeleton Store")) return false
        preferences.setBoolean("skeletonStoreAvailable", true)
        return true
    }

    fun applyFromSpeakeasy(url: String?, html: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        if (url != null &&
            !url.contains("whichplace=speakeasy", ignoreCase = true) &&
            !url.contains("speakeasy", ignoreCase = true)
        ) {
            return false
        }
        if (html.contains("olivers_nocost")) return false
        preferences.setInt("_speakeasyFreeFights", 3)
        return true
    }

    fun applyFromTown(url: String?, html: String, preferences: Preferences?): Boolean {
        if (preferences == null) return false
        if (url != null &&
            !url.contains("whichplace=town", ignoreCase = true)
        ) {
            // Exact town (not town_right/wrong/market) — allow if whichplace=town& or ends with =town
            return false
        }
        // Avoid double-firing on town_right / town_wrong / town_market
        val place = url.orEmpty()
        if (place.contains("town_right", ignoreCase = true) ||
            place.contains("town_wrong", ignoreCase = true) ||
            place.contains("town_market", ignoreCase = true)
        ) {
            return false
        }
        var changed = false
        val fissure = html.contains("town_eincursion")
        if (preferences.getBoolean("eldritchFissureAvailable", false) != fissure) {
            preferences.setBoolean("eldritchFissureAvailable", fissure)
            changed = true
        }
        val horror = html.contains("town_eicfight2")
        if (preferences.getBoolean("eldritchHorrorAvailable", false) != horror) {
            preferences.setBoolean("eldritchHorrorAvailable", horror)
            changed = true
        }
        return changed
    }
}
