package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Phases 5759–5770 — thin HTTP residual non-shop registerRequest hubs
 * (Behavioral Deepen XXXI). Arena, clan hall, and raffle visit session-log lines.
 */

object ClanHallRequestHub {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.startsWith("clan_hall.php", ignoreCase = true)) return false
        sessionLogger?.appendRawLine("Visiting Clan Hall")
        return true
    }
}

object ArenaVisitRequestHub {
    /** Claim bare arena.php visits that CakeArenaRequest.registerRequest doesn't log. */
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.startsWith("arena.php", ignoreCase = true)) return false
        if (url.contains("action=go", ignoreCase = true)) return false // handled by CakeArenaRequest
        sessionLogger?.appendRawLine("Visiting the Cake-Shaped Arena")
        return true
    }
}

object RaffleVisitRequestHub {
    /** Desktop RaffleRequest.registerRequest — session-log raffle ticket purchases. */
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.startsWith("raffle.php", ignoreCase = true)) return false
        // Raffle buy is logged by RequestLogger.registerLongTail; silently claim visit here.
        return true
    }
}
