package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [net.sourceforge.kolmafia.session.CrystalBallManager] — fight-HTML prediction
 * catalog, ponder parse, expiry, and zone/monster helpers.
 */
object CrystalBallManager {
    const val PREDICTIONS_PREF = "crystalBallPredictions"

    data class Prediction(val turnCount: Int, val location: String, val monster: String) {
        fun toPref(): String = "$turnCount:$location:$monster"
        companion object {
            fun parse(raw: String): Prediction? {
                val bits = raw.split(":", limit = 3)
                if (bits.size < 3) return null
                val turn = bits[0].toIntOrNull() ?: return null
                return Prediction(turn, bits[1], bits[2])
            }
        }
    }

    private val predictions = linkedMapOf<String, Prediction>()

    private val FIGHT_PATTERNS = listOf(
        Regex("your next fight will be against <b>an? (.*?)</b>", RegexOption.IGNORE_CASE),
        Regex("next monster in this (?:zone is going to|area will) be <b>an? (.*?)</b>", RegexOption.IGNORE_CASE),
        Regex("Look out, there's <b>an? (.*?)</b> right around the next corner", RegexOption.IGNORE_CASE),
        Regex("There's a little you fighting a little <b>(.*?)</b>", RegexOption.IGNORE_CASE),
        Regex("How do you feel about fighting <b>an? (.*?)</b>\\? Coz that's", RegexOption.IGNORE_CASE),
        Regex("the next monster in this area will be <b>an? (.*?)</b>", RegexOption.IGNORE_CASE),
        Regex("and see a tiny you fighting a tiny <b>(.*?)</b> in a tiny", RegexOption.IGNORE_CASE),
        Regex("it looks like there's <b>an? (.*?)</b> prowling around", RegexOption.IGNORE_CASE),
        Regex("and see yourself running into <b>an? (.*?)</b> soon", RegexOption.IGNORE_CASE),
        Regex("showing you an image of yourself fighting <b>an? (.*?)</b>", RegexOption.IGNORE_CASE),
        Regex("if you stick around here you're going to run into <b>an? (.*?)</b>", RegexOption.IGNORE_CASE),
    )

    private val POSSIBLE_PREDICTION = Regex(
        """<li>\s+(?:an?|the|some)? ?(.*? in .*?)</li>""",
        RegexOption.IGNORE_CASE,
    )

    fun clear(preferences: Preferences? = null) {
        predictions.clear()
        writePref(preferences)
    }

    fun reset(preferences: Preferences?) {
        predictions.clear()
        val raw = preferences?.getString(PREDICTIONS_PREF, "").orEmpty()
        if (raw.isBlank()) return
        for (part in raw.split("|")) {
            val pred = Prediction.parse(part) ?: continue
            predictions[pred.location] = pred
        }
    }

    fun getPredictions(): Map<String, Prediction> = predictions.toMap()

    fun parseCrystalBall(
        html: String,
        locationName: String?,
        currentRun: Int,
        preferences: Preferences?,
        findMonster: (String) -> String? = { MonsterDatabase.getByName(it)?.name },
    ): Boolean {
        reset(preferences)
        val predicted = parseCrystalBallMonster(html) ?: return false
        val monsterName = findMonster(predicted) ?: return false
        val location = locationName?.takeIf { it.isNotBlank() } ?: return false
        val existing = predictions[location]
        if (existing != null && existing.monster.equals(monsterName, ignoreCase = true)) {
            return false
        }
        predictions[location] = Prediction(currentRun, location, monsterName)
        writePref(preferences)
        return true
    }

    fun parsePonder(
        html: String,
        preferences: Preferences?,
        currentRun: Int = 0,
        findLocation: (String) -> String? = { AdventureDatabase.getByName(it)?.locationName },
        findMonster: (String) -> String? = { MonsterDatabase.getByName(it)?.name },
    ): Boolean {
        reset(preferences)
        val old = predictions.toMap()
        predictions.clear()
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
            predictions[locationName] = Prediction(turn, locationName, monsterName)
        }
        writePref(preferences)
        return true
    }

    fun updateCrystalBallPredictions(
        lastAdventureName: String?,
        currentRun: Int,
        preferences: Preferences?,
    ): Boolean {
        if (predictions.isEmpty() || lastAdventureName == null) return false
        val before = predictions.size
        predictions.values.removeAll { prediction ->
            !prediction.location.equals(lastAdventureName, ignoreCase = true) &&
                prediction.turnCount + 2 <= currentRun
        }
        if (predictions.size == before) return false
        writePref(preferences)
        return true
    }

    fun isCrystalBallZone(zone: String, equipped: Boolean): Boolean {
        if (!equipped) return false
        return predictions.values.any { it.location.equals(zone, ignoreCase = true) }
    }

    fun isCrystalBallMonster(monster: String, zone: String, equipped: Boolean): Boolean {
        if (!equipped) return false
        return predictions.values.any { pred ->
            pred.monster.equals(monster, ignoreCase = true) &&
                pred.location.equals(zone, ignoreCase = true)
        }
    }

    fun isEquipped(familiarItemName: String?): Boolean {
        val name = familiarItemName.orEmpty()
        return name.contains("miniature crystal ball", ignoreCase = true) ||
            name.contains("replica miniature crystal ball", ignoreCase = true)
    }

    fun orbItemId(): Int = ItemPool.MINIATURE_CRYSTAL_BALL
    fun replicaOrbItemId(): Int = ItemPool.REPLICA_MINIATURE_CRYSTAL_BALL

    internal fun parseCrystalBallMonster(html: String): String? {
        for (pattern in FIGHT_PATTERNS) {
            pattern.find(html)?.groupValues?.getOrNull(1)?.let { return it }
        }
        return null
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

    private fun writePref(preferences: Preferences?) {
        preferences?.setString(
            PREDICTIONS_PREF,
            predictions.values.sortedWith(compareBy({ it.turnCount }, { it.location }))
                .joinToString("|") { it.toPref() },
        )
    }
}
