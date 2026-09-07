package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [net.sourceforge.kolmafia.request.RichardRequest] clan_hobopolis gym. */
object RichardRequest {
    const val MYSTICALITY = 1
    const val MOXIE = 2
    const val MUSCLE = 3

    fun getAdventuresUsed(url: String): Int {
        if (!url.contains("clan_hobopolis.php", ignoreCase = true)) return 0
        if (!url.contains("preaction=spendturns", ignoreCase = true)) return 0
        return Regex("""numturns=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("clan_hobopolis.php", ignoreCase = true) ||
            !url.contains("place=3") ||
            !url.contains("preaction=spendturns")
        ) {
            return false
        }
        val gym = when {
            url.contains("whichservice=1") -> "Help Richard make bandages (Mysticality)"
            url.contains("whichservice=2") -> "Help Richard make drinks (Moxie)"
            url.contains("whichservice=3") -> "Help Richard make sandwiches (Muscle)"
            else -> "Help Richard"
        }
        sessionLogger?.appendRawLine(gym)
        return true
    }
}
