package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Parses campground telescope HTML and syncs `telescope*` / `nsChallenge*` prefs.
 * Mirrors desktop [TelescopeRequest.parseResponse] and [SorceressLairManager.parseChallenge].
 */
object TelescopeSync {

    private val beecorePattern = Regex("""see (.*?) surrounding it\.""")
    private val patterns = listOf(
        Regex("""second group of people (.*?)\."""),
        Regex("""third group of (.*?)\."""),
        Regex("""reveal some (.*?)\."""),
        Regex("""entrance you see (.*?)\."""),
        Regex("""see a pipe (.*?)\."""),
    )

    private val crowd2Data = listOf(
        Triple(
            "standing around flexing their muscles and using grip exercisers",
            "Strongest Adventurer",
            "Muscle",
        ),
        Triple(
            "sitting around playing chess and solving complicated-looking logic puzzles",
            "Smartest Adventurer",
            "Mysticality",
        ),
        Triple("all wearing sunglasses and dancing", "Smoothest Adventurer", "Moxie"),
    )

    private val crowd3Data = listOf(
        Triple("people, all of whom appear to be on fire", "Hottest Adventurer", "hot"),
        Triple("people, clustered around a group of igloos", "Coldest Adventurer", "cold"),
        Triple("people, surrounded by a cloud of eldritch mist", "Spookiest Adventurer", "spooky"),
        Triple("people, surrounded by garbage and clouds of flies", "Stinkiest Adventurer", "stench"),
        Triple("greasy-looking people furtively skulking around", "Sleaziest Adventurer", "sleaze"),
    )

    private val mazeTrap1Data = listOf(
        Triple("smoldering bushes on the outskirts of a hedge maze", "Hot Damage", "hot"),
        Triple("frost-rimed bushes on the outskirts of a hedge maze", "Cold Damage", "cold"),
        Triple("creepy-looking black bushes on the outskirts of a hedge maze", "Spooky Damage", "spooky"),
        Triple("nasty-looking, dripping green bushes on the outskirts of a hedge maze", "Stench Damage", "stench"),
        Triple("purplish, greasy-looking hedges", "Sleaze Damage", "sleaze"),
    )

    private val mazeTrap2Data = listOf(
        Triple("smoke rising from deeper within the maze", "Hot Damage", "hot"),
        Triple("wintry mists rising from deeper within the maze", "Cold Damage", "cold"),
        Triple("a miasma of eldritch vapors rising from deeper within the maze", "Spooky Damage", "spooky"),
        Triple("a cloud of green gas hovering over the maze", "Stench Damage", "stench"),
        Triple("a greasy purple cloud hanging over the center of the maze", "Sleaze Damage", "sleaze"),
    )

    private val mazeTrap3Data = listOf(
        Triple("with lava slowly oozing out of it", "Hot Damage", "hot"),
        Triple("occasionally disgorging a bunch of ice cubes", "Cold Damage", "cold"),
        Triple("surrounded by creepy black mist", "Spooky Damage", "spooky"),
        Triple("disgorging a really surprising amount of sewage", "Stench Damage", "stench"),
        Triple("that occasionally vomits out a greasy ball of hair", "Sleaze Damage", "sleaze"),
    )

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        character: CharacterState? = null,
        inBeecore: Boolean = false,
        inBugcore: Boolean = false,
    ) {
        if (!url.contains("campground.php", ignoreCase = true)) return
        if (preferences == null) return

        if (url.contains("action=telescopehigh", ignoreCase = true)) {
            preferences.setBoolean("telescopeLookedHigh", true)
            return
        }
        if (!url.contains("action=telescopelow", ignoreCase = true)) return

        preferences.setInt("lastTelescopeReset", character?.ascensionNumber ?: 0)
        for (index in 1..7) preferences.setString("telescope$index", "")
        for (index in 1..5) preferences.setString("nsChallenge$index", "none")

        if (inBugcore) return

        var upgrades = 0
        for (patternIndex in patterns.indices) {
            val pattern = if (patternIndex == 0 && inBeecore) beecorePattern else patterns[patternIndex]
            val match = pattern.find(html) ?: break
            upgrades++
            val test = match.groupValues[1]
            preferences.setString("telescope$upgrades", test)
            preferences.setString("nsChallenge$upgrades", "none")
            challengeData(upgrades)?.firstOrNull { it.first == test }?.third?.let { resolved ->
                preferences.setString("nsChallenge$upgrades", resolved)
            }
        }

        val previousUpgrades = preferences.getInt("telescopeUpgrades", 0)
        val resolvedUpgrades = if (upgrades == 5 && previousUpgrades > upgrades) previousUpgrades else upgrades
        preferences.setInt("telescopeUpgrades", resolvedUpgrades)
    }

    private fun challengeData(challenge: Int): List<Triple<String, String, String>>? = when (challenge) {
        1 -> crowd2Data
        2 -> crowd3Data
        3 -> mazeTrap1Data
        4 -> mazeTrap2Data
        5 -> mazeTrap3Data
        else -> null
    }
}
