package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop elemental-airport coinmaster [accessible] gates
 * (Disco GiftCo / Wal-Mart / Dinsey / SI shops / Elemental Duty Free).
 */
object AirportShopAccessibility {

    fun hotInaccessible(prefs: Preferences?, limitMode: String = ""): String? {
        if (!hasAirport(prefs, "hotAirportAlways", "_hotAirportToday")) {
            return "You don't have access to That 70s Volcano"
        }
        if (LimitModeGates.limitZone("That 70s Volcano", limitMode)) {
            return "You cannot currently access That 70s Volcano"
        }
        return null
    }

    fun coldInaccessible(prefs: Preferences?, limitMode: String = ""): String? {
        if (!hasAirport(prefs, "coldAirportAlways", "_coldAirportToday")) {
            return "You don't have access to The Glaciest"
        }
        if (LimitModeGates.limitZone("The Glaciest", limitMode)) {
            return "You cannot currently access The Glaciest"
        }
        return null
    }

    fun spookyInaccessible(prefs: Preferences?, limitMode: String = ""): String? {
        if (!hasAirport(prefs, "spookyAirportAlways", "_spookyAirportToday")) {
            return "You don't have access to Conspiracy Island"
        }
        if (LimitModeGates.limitZone("Conspiracy Island", limitMode)) {
            return "You cannot currently access Conspiracy Island"
        }
        return null
    }

    fun stenchInaccessible(prefs: Preferences?, limitMode: String = ""): String? {
        if (!hasAirport(prefs, "stenchAirportAlways", "_stenchAirportToday")) {
            return "You don't have access to Dinseylandfill"
        }
        if (LimitModeGates.limitZone("Dinseylandfill", limitMode)) {
            return "You cannot currently access Dinseylandfill"
        }
        return null
    }

    /** True when any elemental airport always/today pref is set (incl. sleaze / SBB). */
    fun anyAirport(prefs: Preferences?): Boolean =
        hasAirport(prefs, "coldAirportAlways", "_coldAirportToday") ||
            hasAirport(prefs, "hotAirportAlways", "_hotAirportToday") ||
            hasAirport(prefs, "spookyAirportAlways", "_spookyAirportToday") ||
            hasAirport(prefs, "stenchAirportAlways", "_stenchAirportToday") ||
            hasAirport(prefs, "sleazeAirportAlways", "_sleazeAirportToday")

    /** Desktop [AirportRequest.accessible] — Elemental Duty Free. */
    fun dutyFreeInaccessible(prefs: Preferences?): String? =
        if (anyAirport(prefs)) null else "You cannot access the Elemental Airport"

    fun shawarmaInaccessible(prefs: Preferences?, limitMode: String = ""): String? =
        spookyInaccessible(prefs, limitMode)
            ?: if (prefs?.getBoolean("SHAWARMAInitiativeUnlocked", false) != true) {
                "SHAWARMA Initiative is locked"
            } else {
                null
            }

    fun canteenInaccessible(prefs: Preferences?, limitMode: String = ""): String? =
        spookyInaccessible(prefs, limitMode)
            ?: if (prefs?.getBoolean("canteenUnlocked", false) != true) {
                "The Canteen is locked"
            } else {
                null
            }

    fun spacegateArmoryInaccessible(prefs: Preferences?, limitMode: String = ""): String? =
        spookyInaccessible(prefs, limitMode)
            ?: if (prefs?.getBoolean("armoryUnlocked", false) != true) {
                "The Armory is locked"
            } else {
                null
            }

    private fun hasAirport(prefs: Preferences?, always: String, today: String): Boolean =
        prefs?.getBoolean(always, false) == true || prefs?.getBoolean(today, false) == true
}
