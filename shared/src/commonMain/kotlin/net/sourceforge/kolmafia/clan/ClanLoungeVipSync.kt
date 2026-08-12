package net.sourceforge.kolmafia.clan

import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop ClanLoungeRequest shower / swimming-pool response pref sync. */
object ClanLoungeVipSync {

    const val APRIL_SHOWER_PREF = "_aprilShower"
    const val OLYMPIC_SWIMMING_POOL_PREF = "_olympicSwimmingPool"
    const val OLYMPIC_SWIMMING_POOL_ITEM_FOUND_PREF = "_olympicSwimmingPoolItemFound"
    const val BALLPIT_PREF = "_ballpit"

    fun syncShowerFromResponse(html: String, action: String?, prefs: Preferences?) {
        if (prefs == null) return
        when (action?.lowercase()) {
            "shower" -> {
                if (html.contains("already had a shower today")) {
                    prefs.setBoolean(APRIL_SHOWER_PREF, true)
                }
            }
            "takeshower" -> {
                if (html.contains("this is way too hot") ||
                    html.contains("relaxes your muscles") ||
                    html.contains("mind expands") ||
                    html.contains("your goosebumps absorb") ||
                    html.contains("shards of frosty double-ice") ||
                    html.contains("already had a shower today") ||
                    (html.contains("<table><tr><td></td></tr></table>") &&
                        html.contains("aprilshower.gif"))
                ) {
                    prefs.setBoolean(APRIL_SHOWER_PREF, true)
                }
            }
        }
    }

    fun syncSwimFromResponse(html: String, action: String?, prefs: Preferences?) {
        if (prefs == null) return
        when (action?.lowercase()) {
            "swimmingpool" -> {
                if (html.contains("already worked out in the pool today")) {
                    prefs.setBoolean(OLYMPIC_SWIMMING_POOL_PREF, true)
                }
            }
            "goswimming" -> {
                if (html.contains("<table><tr><td></td></tr></table>") &&
                    html.contains("vippool.gif")
                ) {
                    prefs.setBoolean(OLYMPIC_SWIMMING_POOL_PREF, true)
                    return
                }
                if (LAPS_PATTERN.containsMatchIn(html) || SPRINTS_PATTERN.containsMatchIn(html)) {
                    prefs.setBoolean(OLYMPIC_SWIMMING_POOL_PREF, true)
                }
            }
        }
    }

    fun syncBallpitFromResponse(html: String, prefs: Preferences?) {
        if (prefs == null) return
        if (html.contains("play in the ball pit") ||
            html.contains("already played in the ball pit")
        ) {
            prefs.setBoolean(BALLPIT_PREF, true)
        }
    }

    fun syncSwimTreasureFromResponse(html: String, url: String, prefs: Preferences?) {
        if (prefs == null) return
        if (!url.contains("action=treasure", ignoreCase = true)) return
        if (SWIMMING_POOL_TREASURE_PATTERN.containsMatchIn(html)) {
            prefs.setBoolean(OLYMPIC_SWIMMING_POOL_ITEM_FOUND_PREF, true)
        }
    }

    private val LAPS_PATTERN = Regex("""You swam (\d+) laps""", RegexOption.IGNORE_CASE)
    private val SPRINTS_PATTERN =
        Regex("""You did (\d+) submarine sprints""", RegexOption.IGNORE_CASE)
    private val SWIMMING_POOL_TREASURE_PATTERN =
        Regex("""found a .+?!""", RegexOption.IGNORE_CASE)
}
