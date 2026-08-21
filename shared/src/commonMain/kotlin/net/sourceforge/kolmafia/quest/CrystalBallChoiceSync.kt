package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [CrystalBallManager.parsePonder] via choice 1462 visit.
 */
object CrystalBallChoiceSync {

    const val CHOICE_ID = 1462

    const val PREDICTIONS_PREF = "crystalBallPredictions"

    private val POSSIBLE_PREDICTION = Regex(
        """<li>\s+(?:an?|the|some)? ?(.*? in .*?)</li>""",
        RegexOption.IGNORE_CASE,
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        currentRun: Int = 0,
        findLocation: (String) -> String? = { AdventureDatabase.getByName(it)?.locationName },
        findMonster: (String) -> String? = { MonsterDatabase.getByName(it)?.name },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val old = parsePref(preferences.getString(PREDICTIONS_PREF, ""))
        val next = linkedMapOf<String, Prediction>()

        POSSIBLE_PREDICTION.findAll(html).forEach { match ->
            val group = match.groupValues[1]
            val resolved = resolveMonsterLocation(group, findLocation, findMonster) ?: return@forEach
            val (monsterName, locationName) = resolved
            val prior = old[locationName]
            val turn = if (prior != null && prior.monster.equals(monsterName, ignoreCase = true)) {
                prior.turnCount
            } else {
                currentRun
            }
            next[locationName] = Prediction(turn, locationName, monsterName)
        }

        preferences.setString(
            PREDICTIONS_PREF,
            next.values.sortedWith(compareBy({ it.turnCount }, { it.location }))
                .joinToString("|") { it.toPref() },
        )
        return true
    }

    private fun resolveMonsterLocation(
        group: String,
        findLocation: (String) -> String?,
        findMonster: (String) -> String?,
    ): Pair<String, String>? {
        var index = group.indexOf(" in ")
        while (index >= 0) {
            val monsterName = group.substring(0, index)
            val locationName = group.substring(index + 4)
            index = group.indexOf(" in ", index + 4)
            val location = findLocation(locationName) ?: continue
            val monster = findMonster(monsterName) ?: continue
            return monster to location
        }
        return null
    }

    private fun parsePref(raw: String): Map<String, Prediction> {
        if (raw.isBlank()) return emptyMap()
        val map = mutableMapOf<String, Prediction>()
        for (part in raw.split("|")) {
            val bits = part.split(":", limit = 3)
            if (bits.size < 3) continue
            val turn = bits[0].toIntOrNull() ?: continue
            map[bits[1]] = Prediction(turn, bits[1], bits[2])
        }
        return map
    }

    private data class Prediction(val turnCount: Int, val location: String, val monster: String) {
        fun toPref(): String = "$turnCount:$location:$monster"
    }
}
