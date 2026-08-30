package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Residual desktop SorceressLairManager hub. Existing tower, contest, telescope,
 * and hedge-maze parsers remain the authoritative implementations.
 */
object SorceressLairSync {
    const val WAND_OF_NAGAMAR = 626
    const val CONFIDENCE = "Confidence!"
    const val HEDGE_MAZE = "The Hedge Maze"

    private val SILENT_CHOICES = (1005..1013).toSet() + setOf(1015, 1020, 1021, 1022)

    fun parseTowerResponse(
        action: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        setKingLiberated: () -> Unit = {},
    ): Boolean {
        if (action == "ns_11_prism" &&
            html.contains("King Ralph the XI stands before you in all his regal glory")
        ) {
            questDatabase?.setProgress(Quest.FINAL, QuestDatabase.FINISHED)
            preferences?.setBoolean("kingLiberated", true)
            setKingLiberated()
            return true
        }
        return TowerSync.parseTower(html, questDatabase, preferences)
    }

    fun enterSorceressFight(effectManager: EffectManager?): Boolean =
        effectManager?.retainEffects { it.name.equals(CONFIDENCE, ignoreCase = true) } ?: false

    fun visitChoice(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        questDatabase: QuestDatabase?,
        setLastAdventure: (String) -> Unit = {},
    ): Boolean = when (choiceId) {
        1003 -> ContestBoothSync.parseContestBooth(0, html, preferences, questDatabase)
        in 1005..1013 -> {
            setLastAdventure(HEDGE_MAZE)
            val trap = ContestBoothSync.parseMazeTrap(choiceId, html, preferences)
            ContestBoothSync.visitHedgeMazeChoice(choiceId, preferences, questDatabase) || trap
        }
        else -> false
    }

    fun registerRequest(
        url: String,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
        adventureCount: Int = preferences?.getInt("turnsPlayed", 0) ?: 0,
        onEnterSorceressFight: () -> Unit = {},
    ): Boolean {
        if (!url.substringAfterLast('/').startsWith("place.php")) return false
        if (!queryParam(url, "whichplace").equals("nstower", ignoreCase = true)) return false
        val action = queryParam(url, "action") ?: return true
        if (action == "ns_10_sorcfight") {
            onEnterSorceressFight()
            return false
        }
        if (action == "ns_03_hedgemaze") return true
        val message = when (action) {
            "ns_01_contestbooth" -> "[$adventureCount] Tower: Contest Booth"
            "ns_02_coronation" -> "[$adventureCount] Tower: Closing Ceremony"
            "ns_11_prism" -> "[$adventureCount] Freeing King Ralph"
            else -> return false
        }
        sessionLogger?.appendRawLine(message)
        return true
    }

    fun registerChoice(
        choiceId: Int,
        option: Int,
        preferences: Preferences?,
        sessionLogger: SessionLogger?,
    ): Boolean {
        if (choiceId in SILENT_CHOICES) return true
        if (choiceId != 1003) return false
        val message = when (option) {
            in 1..3 -> {
                val challenge = option - 1
                val test = if (challenge == 0) "" else preferences?.getString("nsChallenge$challenge", "").orEmpty()
                "Registering for the ${TelescopeSync.getChallengeDescription(challenge, test)} Contest"
            }
            4 -> "Claiming your prize"
            5 -> "Looking at the Moon"
            else -> return true
        }
        sessionLogger?.appendRawLine(message)
        return true
    }

    fun needsNagamar(action: String?, inBeecore: Boolean, hasWand: Boolean): Boolean =
        action == "ns_10_sorcfight" && !inBeecore && !hasWand

    fun action(url: String): String? = queryParam(url, "action")

    private fun queryParam(url: String, name: String): String? {
        val query = url.substringAfter('?', "")
        return query.split('&').firstNotNullOfOrNull { part ->
            val key = part.substringBefore('=')
            if (key.equals(name, ignoreCase = true)) part.substringAfter('=', "") else null
        }
    }
}
