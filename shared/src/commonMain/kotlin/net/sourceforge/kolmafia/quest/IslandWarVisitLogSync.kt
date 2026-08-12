package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [IslandRequest.registerRequest] bigisland.php/postwarisland.php visit session-log hooks. */
object IslandWarVisitLogSync {

    const val PREF_LAST_CAMP_VISITED = "_lastCampVisited"

    private const val GUNPOWDER = 2403

    private const val BATTLEFIELD_FRAT = "The Battlefield (Frat Uniform)"
    private const val BATTLEFIELD_HIPPY = "The Battlefield (Hippy Uniform)"

    private val ACTION_PATTERN = Regex("""action=([^&]+)""")
    private val PLACE_PATTERN = Regex("""place=([^&]+)""")
    private val OPTION_PATTERN = Regex("""[?&]option=(\d+)""")

    fun register(
        url: String,
        html: String,
        preferences: Preferences,
        context: IslandWarVisitSync.IslandVisitContext,
        sessionLogger: SessionLogger?,
    ): Boolean {
        val isBigIsland = url.contains("bigisland.php", ignoreCase = true)
        val isPostwarIsland = url.contains("postwarisland.php", ignoreCase = true)
        if (!isBigIsland && !isPostwarIsland) return false

        if (isBigIsland) {
            val campMaster = IslandWarVisitSync.findCampMaster(url)
            if (campMaster != null) {
                preferences.setString(PREF_LAST_CAMP_VISITED, campMaster.nickname)
                return true
            }

            val action = getAction(url)
            if (action == "bossfight") {
                return handleBossfight(preferences, sessionLogger)
            }
        }

        val action = getAction(url)

        if (action == null) {
            val place = getPlace(url)
            if (place == "concert") {
                sessionLogger?.appendRawLine("Visiting the Mysterious Island Arena")
            }
            return true
        }

        val message = when (action) {
            "concert" -> {
                val option = OPTION_PATTERN.find(url)?.groupValues?.getOrNull(1) ?: return true
                "concert $option"
            }
            "junkman" -> "Visiting Yossarian"
            "stand" -> "Visiting The Organic Produce Stand"
            "farmer" -> "Visiting Farmer McMillicancuddy"
            "nuns" -> "Visiting Our Lady of Perpetual Indecision "
            "pyro" -> {
                val count = if (context.hasItemId(GUNPOWDER)) 1 else 0
                "Visiting the lighthouse keeper with $count barrel${if (count == 1) "" else "s"} of gunpowder."
            }
            else -> return false
        }

        sessionLogger?.appendRawLine(message)
        return true
    }

    private fun handleBossfight(
        preferences: Preferences,
        sessionLogger: SessionLogger?,
    ): Boolean {
        val lastCamp = preferences.getString(PREF_LAST_CAMP_VISITED, "")
        val (headquarters, battlefield) = when (lastCamp) {
            "dimemaster" -> "Hippy Camp" to BATTLEFIELD_FRAT
            "quartersmaster" -> "Frat House" to BATTLEFIELD_HIPPY
            else -> "Headquarters" to BATTLEFIELD_HIPPY
        }
        preferences.setString(Preferences.LAST_LOCATION, battlefield)
        sessionLogger?.appendRawLine(headquarters)
        return true
    }

    internal fun getAction(url: String): String? =
        ACTION_PATTERN.find(url)?.groupValues?.getOrNull(1)

    internal fun getPlace(url: String): String? =
        PLACE_PATTERN.find(url)?.groupValues?.getOrNull(1)
}
