package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * High-traffic IoTM / familiar fight HTML pref writers (Phases 1266–1285),
 * ported from desktop [FightRequest] familiar/equipment handlers.
 */
object FightIotmSync {

    const val FAMILIAR_NANORHINO = 167
    const val FAMILIAR_MELODRAMEDARY = 279
    const val FAMILIAR_COOKBOOKBAT = 288

    private val SWEAT_LESS = Regex("""You get (\d+)% less Sweaty""")
    private val SWEAT_MORE = Regex("""You get (\d+)% Sweatier""")
    private val NANORHINO_BUFF = Regex("""title="Nano(?:brawny|brainy|ballsy)""")
    private val NANORHINO_CHARGE1 = Regex("""(\d+)% charge""")
    private val NANORHINO_CHARGE2 = Regex("""charge to (\d+)%""")
    private val SPIT_FULL = Regex("""\((\d+)% full\)""")
    private val DRONE_ACTIVATION = Regex("""(\d+) more drones are still circling around""")
    private val DECEASED_TREE = Regex("""Your crimbo tree has ([\d,]+) needles left""")

    private const val COOKBOOKBAT_INGREDIENTS =
        "\"As I recall,  you could use these,\""

    private val SCRAPBOOK_PHRASES = listOf(
        "for your scrapbook",
        "definitely going in the scrapbook",
        "for the scrapbook",
        "for the ol' scrapbook",
    )

    fun apply(
        html: String,
        preferences: Preferences?,
        familiarId: Int = 0,
        monsterName: String = "",
        locationName: String = "",
        won: Boolean = false,
        adventureId: String = "",
    ): Boolean {
        if (preferences == null || html.isBlank()) return false
        var changed = false
        changed = applySweat(html, preferences) || changed
        changed = applyScrapbook(html, preferences) || changed
        changed = applyGooseDrones(html, preferences) || changed
        changed = applyLovebugs(html, preferences, adventureId) || changed
        changed = applyBagOTricks(html, preferences) || changed
        changed = applyGarbageTree(html, preferences) || changed
        changed = applyMayoWasp(html, preferences, monsterName) || changed
        when (familiarId) {
            FAMILIAR_NANORHINO ->
                changed = applyNanorhino(html, preferences) || changed
            FAMILIAR_COOKBOOKBAT ->
                changed = applyCookbookbat(html, preferences, locationName) || changed
            FAMILIAR_MELODRAMEDARY ->
                changed = applyMelodramedary(html, preferences) || changed
        }
        // Cosmic bowling ball return timer ticks each fight start (desktop FightRequest)
        if (!won) {
            // no-op: return combats decremented on fight start separately
        }
        return changed
    }

    /** Desktop decrement of cosmicBowlingBallReturnCombats on fight start. */
    fun noteFightStart(preferences: Preferences?) {
        preferences ?: return
        val cur = preferences.getInt("cosmicBowlingBallReturnCombats", -1)
        if (cur > -1) {
            preferences.setInt("cosmicBowlingBallReturnCombats", (cur - 1).coerceAtLeast(-1))
        }
    }

    fun applySweat(html: String, preferences: Preferences): Boolean {
        var changed = false
        SWEAT_LESS.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { n ->
            preferences.setInt("sweat", (preferences.getInt("sweat", 0) - n).coerceAtLeast(0))
            changed = true
        }
        SWEAT_MORE.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { n ->
            preferences.setInt("sweat", preferences.getInt("sweat", 0) + n)
            changed = true
        }
        return changed
    }

    fun applyScrapbook(html: String, preferences: Preferences): Boolean {
        if (html.contains("You take out your scrapbook and start showing photos") ||
            html.contains("waving your scrapbook")
        ) {
            preferences.setInt(
                "scrapbookCharges",
                (preferences.getInt("scrapbookCharges", 0) - 100).coerceAtLeast(0),
            )
            return true
        }
        if (SCRAPBOOK_PHRASES.any { html.contains(it) }) {
            preferences.setInt(
                "scrapbookCharges",
                preferences.getInt("scrapbookCharges", 0) + 1,
            )
            return true
        }
        return false
    }

    fun applyGooseDrones(html: String, preferences: Preferences): Boolean {
        if (!html.contains("matter duplicating drones")) return false
        when {
            html.contains("That was the last drone") -> {
                preferences.setInt("gooseDronesRemaining", 0)
                return true
            }
            html.contains("1 more drone is still circling around") -> {
                preferences.setInt("gooseDronesRemaining", 1)
                return true
            }
            else -> {
                val n = DRONE_ACTIVATION.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
                    ?: return false
                preferences.setInt("gooseDronesRemaining", n)
                return true
            }
        }
    }

    /** Delegates to [FightLovebugSync] full catalog (Phases 1491–1505). */
    fun applyLovebugs(
        html: String,
        preferences: Preferences,
        adventureId: String = "",
    ): Boolean = FightLovebugSync.apply(html, preferences, adventureId)

    fun applyBagOTricks(html: String, preferences: Preferences): Boolean {
        if (html.contains("You reach into the bag and pull out ")) {
            preferences.setInt(
                "_bagOTricksBuffs",
                preferences.getInt("_bagOTricksBuffs", 0) + 1,
            )
            preferences.setInt("bagOTricksCharges", 0)
            return true
        }
        if (!html.contains("The Bag o' Tricks")) return false
        when {
            html.contains("suddenly feels a little heavier") -> {
                preferences.setInt("bagOTricksCharges", 1)
                return true
            }
            html.contains("begins to wriggle around in your hand") -> {
                preferences.setInt("bagOTricksCharges", 2)
                return true
            }
            html.contains("begins squirming around more urgently") ||
                html.contains("continues to wriggle around in your hand") -> {
                preferences.setInt("bagOTricksCharges", 3)
                return true
            }
        }
        return false
    }

    fun applyGarbageTree(html: String, preferences: Preferences): Boolean {
        if (html.contains("Your crimbo tree is now 100% naked")) {
            preferences.setInt("garbageTreeCharge", 0)
            return true
        }
        DECEASED_TREE.find(html)?.groupValues?.getOrNull(1)?.replace(",", "")?.toIntOrNull()?.let {
            preferences.setInt("garbageTreeCharge", it)
            return true
        }
        return false
    }

    fun applyMayoWasp(html: String, preferences: Preferences, monsterName: String): Boolean {
        if (!monsterName.equals("mayonnaise wasp", ignoreCase = true)) return false
        if (!html.contains("The mayo wasp sniffs at you for a minute")) return false
        preferences.setBoolean("_mayoWaspEggDeposited", true)
        return true
    }

    fun applyNanorhino(html: String, preferences: Preferences): Boolean {
        val current = preferences.getInt("_nanorhinoCharge", 0)
        if (NANORHINO_BUFF.containsMatchIn(html)) {
            preferences.setInt("_nanorhinoCharge", 0)
            return true
        }
        NANORHINO_CHARGE2.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            preferences.setInt("_nanorhinoCharge", it.coerceIn(0, 100))
            return true
        }
        NANORHINO_CHARGE1.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
            if (it != current) {
                preferences.setInt("_nanorhinoCharge", it.coerceIn(0, 100))
                return true
            }
        }
        return false
    }

    fun applyCookbookbat(
        html: String,
        preferences: Preferences,
        locationName: String,
    ): Boolean {
        var changed = false
        if (!html.contains(COOKBOOKBAT_INGREDIENTS)) {
            val next = (preferences.getInt("cookbookbatIngredientsCharge", 0) + 1).coerceAtMost(11)
            preferences.setInt("cookbookbatIngredientsCharge", next)
            changed = true
        }
        val questLoc = preferences.getString("_cookbookbatQuestLastLocation", "")
        val inSuggested = locationName.isNotEmpty() &&
            questLoc.isNotEmpty() &&
            questLoc.equals(locationName, ignoreCase = true)
        val until = preferences.getInt("_cookbookbatCombatsUntilNewQuest", 0)
        if (until > 0) {
            preferences.setInt(
                "_cookbookbatCombatsUntilNewQuest",
                (until - 1).coerceAtLeast(if (inSuggested) 1 else 0),
            )
            changed = true
        }
        return changed
    }

    fun applyMelodramedary(html: String, preferences: Preferences): Boolean {
        when {
            html.contains("spits a tremendous globule of saliva at your foe") ||
                html.contains("obligingly -- ") -> {
                preferences.setInt("camelSpit", 0)
                return true
            }
            html.contains("is starting to make audible sloshing noises") -> {
                preferences.setInt("camelSpit", 100)
                return true
            }
            html.contains("sucking the liquid") -> {
                SPIT_FULL.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
                    preferences.setInt("camelSpit", it)
                    return true
                }
            }
        }
        return false
    }
}
