package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleAirportChange] elemental airport today-flags + spooky bunker unlocks.
 */
object AirportSync {

    const val SPRING_BEACH_TICKET = 7467
    const val SHAWARMA_KEYCARD = 7792
    const val BOTTLE_OPENER_KEYCARD = 7793
    const val ARMORY_KEYCARD = 7794

    private val BLOCKED = listOf(
        "You don't know where that is.",
        "That isn't a place you can go.",
    )

    private val SNARF = Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE)

    private data class Element(
        val alwaysPref: String,
        val todayPref: String,
        val placeToken: String,
        val adventureIds: Set<Int>,
    )

    private val ELEMENTS = listOf(
        Element("coldAirportAlways", "_coldAirportToday", "airport_cold", setOf(455, 456, 457)),
        Element("hotAirportAlways", "_hotAirportToday", "airport_hot", setOf(448, 449, 450, 451)),
        Element("sleazeAirportAlways", "_sleazeAirportToday", "airport_sleaze", setOf(402, 403, 404)),
        Element("spookyAirportAlways", "_spookyAirportToday", "airport_spooky", setOf(415, 416, 417)),
        Element("stenchAirportAlways", "_stenchAirportToday", "airport_stench", setOf(442, 443, 444, 445)),
    )

    fun syncFromVisit(
        html: String,
        url: String?,
        prefs: Preferences,
        consumeItem: (Int) -> Unit = {},
    ): Boolean {
        if (url.isNullOrBlank()) return false
        var changed = false
        val area = SNARF.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull()
        for (el in ELEMENTS) {
            if (prefs.getBoolean(el.alwaysPref, false)) continue
            val direct =
                (area != null && area in el.adventureIds) ||
                    url.contains("whichplace=${el.placeToken}", ignoreCase = true)
            if (direct) {
                if (!BLOCKED.any { html.contains(it) }) {
                    prefs.setBoolean(el.todayPref, true)
                    changed = true
                }
            } else if (url.contains("whichplace=airport", ignoreCase = true) &&
                html.contains("whichplace=${el.placeToken}", ignoreCase = true)
            ) {
                prefs.setBoolean(el.todayPref, true)
                changed = true
            }
        }
        if (url.contains("whichplace=airport_spooky_bunker", ignoreCase = true)) {
            changed = applyBunker(url, html, prefs, consumeItem) || changed
        }
        return changed
    }

    fun syncFromSpringBeachTicketUse(html: String, prefs: Preferences): Boolean {
        if (html.contains("already have access", ignoreCase = true)) return false
        prefs.setBoolean("_sleazeAirportToday", true)
        return true
    }

    fun applyBunker(
        url: String,
        html: String,
        prefs: Preferences,
        consumeItem: (Int) -> Unit = {},
    ): Boolean {
        var changed = false
        when {
            html.contains("action=si_shop1locked") -> {
                prefs.setBoolean("SHAWARMAInitiativeUnlocked", false)
                changed = true
            }
            html.contains("whichshop=si_shop1") -> {
                prefs.setBoolean("SHAWARMAInitiativeUnlocked", true)
                changed = true
            }
        }
        when {
            html.contains("action=si_shop2locked") -> {
                prefs.setBoolean("canteenUnlocked", false)
                changed = true
            }
            html.contains("whichshop=si_shop2") -> {
                prefs.setBoolean("canteenUnlocked", true)
                changed = true
            }
        }
        when {
            html.contains("action=si_shop3locked") -> {
                prefs.setBoolean("armoryUnlocked", false)
                changed = true
            }
            html.contains("whichshop=si_shop3") -> {
                prefs.setBoolean("armoryUnlocked", true)
                changed = true
            }
        }
        if (html.contains("insert the keycard and the door slides open")) {
            when {
                url.contains("action=si_shop1locked", ignoreCase = true) -> {
                    prefs.setBoolean("SHAWARMAInitiativeUnlocked", true)
                    consumeItem(SHAWARMA_KEYCARD)
                    changed = true
                }
                url.contains("action=si_shop2locked", ignoreCase = true) -> {
                    prefs.setBoolean("canteenUnlocked", true)
                    consumeItem(BOTTLE_OPENER_KEYCARD)
                    changed = true
                }
                url.contains("action=si_shop3locked", ignoreCase = true) -> {
                    prefs.setBoolean("armoryUnlocked", true)
                    consumeItem(ARMORY_KEYCARD)
                    changed = true
                }
            }
        }
        return changed
    }
}
