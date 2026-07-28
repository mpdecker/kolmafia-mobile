package net.sourceforge.kolmafia.shop

import net.sourceforge.kolmafia.inventory.LimitModeGates
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop Spring Break Beach coinmaster [accessible] gates (Brogurt/Taco/Jimmy). */
object SpringBreakBeachAccessibility {

    fun inaccessibleReason(prefs: Preferences?, limitMode: String = ""): String? {
        if (prefs?.getBoolean("_sleazeAirportToday", false) != true &&
            prefs?.getBoolean("sleazeAirportAlways", false) != true
        ) {
            return "You don't have access to Spring Break Beach"
        }
        if (LimitModeGates.limitZone("Spring Break Beach", limitMode)) {
            return "You cannot currently access Spring Break Beach"
        }
        return null
    }
}
