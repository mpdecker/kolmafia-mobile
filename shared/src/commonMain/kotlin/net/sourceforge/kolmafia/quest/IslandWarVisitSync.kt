package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.shop.CoinmasterData
import net.sourceforge.kolmafia.shop.CoinmasterDatabase

/** Desktop [IslandManager.parseBigIsland] / [IslandManager.parsePostwarIsland] / [IslandManager.deduceWinner] visit hooks. */
object IslandWarVisitSync {

    enum class IslandSidequest {
        NONE,
        ARENA,
        JUNKYARD,
        ORCHARD,
        FARM,
        NUNS,
        LIGHTHOUSE,
        CAMP,
    }

    data class IslandVisitContext(
        val hasItemId: (Int) -> Boolean = { false },
        val consumeItem: (Int, Int) -> Unit = { _, _ -> },
        val isWearingWarHippyOutfit: () -> Boolean = { false },
        val ascensionNumber: Int = 0,
        val itemCount: (Int) -> Int = { id -> if (hasItemId(id)) 1 else 0 },
    )

    private const val JAM_BAND_FLYERS = 2404
    private const val ROCK_BAND_FLYERS = 2405
    private const val FILTHWORM_QUEEN_HEART = 2347
    private const val GUNPOWDER = 2403
    private const val MOLYBDENUM_MAGNET = 2497
    private const val MOLYBDENUM_HAMMER = 2498
    private const val MOLYBDENUM_SCREWDRIVER = 2499
    private const val MOLYBDENUM_PLIERS = 2500
    private const val MOLYBDENUM_WRENCH = 2501

    private val MAP_PATTERN = Regex("""bfleft(\d*).*bfright(\d*)""", RegexOption.DOT_MATCHES_ALL)
    private val JUNKYARD_PATTERN = Regex(
        """(?:The last time I saw my|muttering something about a(?: pair of)?) (.*?)(?:, it was|, they were| and) (.*?)[.<]""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val CAMP_PATTERN = Regex("""whichcamp=(\d+)""")
    private val DIMEMASTER_TOKEN_PATTERN = Regex("""You've.*?got ([\d,]+) dime""", RegexOption.DOT_MATCHES_ALL)
    private val QUARTERSMASTER_TOKEN_PATTERN = Regex("""You've.*?got ([\d,]+) quarter""", RegexOption.DOT_MATCHES_ALL)

    private const val DIMEMASTER_TOKEN_TEST = "You don't have any dimes"
    private const val QUARTERSMASTER_TOKEN_TEST = "You don't have any quarters"

    private val JUNKYARD_AREAS = arrayOf(
        "a barrel" to "next to that barrel with something burning in it",
        "a refrigerator" to "near an abandoned refrigerator",
        "some tires" to "over where the old tires are",
        "a car" to "out by that rusted-out car",
    )

    private val MOLYBDENUM_TOOLS = intArrayOf(
        MOLYBDENUM_MAGNET,
        MOLYBDENUM_HAMMER,
        MOLYBDENUM_SCREWDRIVER,
        MOLYBDENUM_PLIERS,
        MOLYBDENUM_WRENCH,
    )

    // Crowther spaded threshold table — desktop IslandManager.IMAGES
    private val IMAGES = intArrayOf(
        0, // Image 0
        3, // Image 1
        9, // Image 2
        17, // Image 3
        28, // Image 4
        40, // Image 5
        52, // Image 6
        64, // Image 7
        80, // Image 8
        96, // Image 9
        114, // Image 10
        132, // Image 11
        152, // Image 12
        172, // Image 13
        192, // Image 14
        224, // Image 15
        258, // Image 16
        294, // Image 17
        332, // Image 18
        372, // Image 19
        414, // Image 20
        458, // Image 21
        506, // Image 22
        556, // Image 23
        606, // Image 24
        658, // Image 25
        711, // Image 26
        766, // Image 27
        822, // Image 28
        880, // Image 29
        939, // Image 30
        999, // Image 31
        1000, // Image 32
    )

    fun parseQuest(url: String): IslandSidequest {
        val lower = url.lowercase()
        return when {
            lower.contains("place=concert") || lower.contains("action=concert") -> IslandSidequest.ARENA
            lower.contains("action=junkman") -> IslandSidequest.JUNKYARD
            lower.contains("action=stand") -> IslandSidequest.ORCHARD
            lower.contains("action=farmer") -> IslandSidequest.FARM
            lower.contains("place=nunnery") -> IslandSidequest.NUNS
            lower.contains("action=pyro") -> IslandSidequest.LIGHTHOUSE
            lower.contains("whichcamp") -> IslandSidequest.CAMP
            else -> IslandSidequest.NONE
        }
    }

    fun applyFromBigIslandVisit(
        url: String? = null,
        html: String,
        preferences: Preferences?,
        context: IslandVisitContext = IslandVisitContext(),
        sessionLogger: SessionLogger? = null,
    ): Boolean {
        val prefs = preferences ?: return false
        var changed = false
        if (prefs.getString("warProgress", "unstarted") != "started") {
            prefs.setString("warProgress", "started")
            changed = true
        }
        if (parseBattlefield(html, prefs)) {
            changed = true
        }
        if (url != null) {
            when (parseQuest(url)) {
                IslandSidequest.ARENA -> if (parseArena(html, prefs, context)) changed = true
                IslandSidequest.JUNKYARD -> if (parseJunkyard(html, prefs, context)) changed = true
                IslandSidequest.FARM -> if (parseFarm(html, prefs)) changed = true
                IslandSidequest.NUNS -> if (parseNunnery(html, prefs)) changed = true
                IslandSidequest.ORCHARD -> if (parseOrchard(html, prefs, context)) changed = true
                IslandSidequest.LIGHTHOUSE -> if (parseLighthouse(html, prefs, context)) changed = true
                IslandSidequest.CAMP -> if (parseCamp(url, html, prefs, context, sessionLogger)) changed = true
                else -> {}
            }
        }
        return changed
    }

    fun applyFromPostwarIslandVisit(
        url: String? = null,
        html: String,
        preferences: Preferences?,
        context: IslandVisitContext = IslandVisitContext(),
    ): Boolean {
        val prefs = preferences ?: return false
        var changed = false
        if (deduceWinner(html, prefs)) {
            changed = true
        }
        if (url != null) {
            when (parseQuest(url)) {
                IslandSidequest.ARENA -> if (parseArena(html, prefs, context)) changed = true
                IslandSidequest.NUNS -> if (parseNunnery(html, prefs)) changed = true
                else -> {}
            }
        }
        return changed
    }

    internal fun deduceWinner(html: String, preferences: Preferences): Boolean {
        val hippiesLost = html.contains("snarfblat=149")
        val fratboysLost = html.contains("snarfblat=150")
        val loser = when {
            !hippiesLost -> "fratboys"
            !fratboysLost -> "hippies"
            else -> "both"
        }
        var changed = false
        if (preferences.getString("sideDefeated", "neither") != loser) {
            preferences.setString("sideDefeated", loser)
            changed = true
        }
        if (preferences.getString("warProgress", "unstarted") != "finished") {
            preferences.setString("warProgress", "finished")
            changed = true
        }
        return changed
    }

    internal fun parseArena(
        html: String,
        preferences: Preferences,
        context: IslandVisitContext,
    ): Boolean {
        return when {
            html.contains("well into the first song") -> {
                setSidequestPref(preferences, "sidequestArenaCompleted", "hippy")
            }
            html.contains("I'll take 'em") -> {
                var changed = setSidequestPref(preferences, "sidequestArenaCompleted", "hippy")
                if (context.hasItemId(JAM_BAND_FLYERS)) {
                    context.consumeItem(JAM_BAND_FLYERS, 1)
                    changed = true
                }
                changed
            }
            html.contains("has already taken the stage") -> {
                setSidequestPref(preferences, "sidequestArenaCompleted", "fratboy")
            }
            html.contains("I'll take them") -> {
                var changed = setSidequestPref(preferences, "sidequestArenaCompleted", "fratboy")
                if (context.hasItemId(ROCK_BAND_FLYERS)) {
                    context.consumeItem(ROCK_BAND_FLYERS, 1)
                    changed = true
                }
                changed
            }
            html.contains("The stage at the Mysterious Island Arena is empty") -> {
                setSidequestPref(preferences, "sidequestArenaCompleted", "none")
            }
            else -> false
        }
    }

    internal fun parseFarm(html: String, preferences: Preferences): Boolean {
        return when {
            html.contains("growing soybeans") || html.contains("blocks of megatofu") ->
                setSidequestPref(preferences, "sidequestFarmCompleted", "hippy")
            html.contains("growing hops") || html.contains("bottles of McMillicancuddy") ->
                setSidequestPref(preferences, "sidequestFarmCompleted", "fratboy")
            else -> false
        }
    }

    internal fun parseNunnery(html: String, preferences: Preferences): Boolean {
        var changed = false
        when {
            html.contains("tend to your wounds") ->
                changed = setSidequestPref(preferences, "sidequestNunsCompleted", "hippy") || changed
            html.contains("refreshing massage") ->
                changed = setSidequestPref(preferences, "sidequestNunsCompleted", "fratboy") || changed
            html.contains("world-weary traveler") ->
                changed = setSidequestPref(preferences, "sidequestNunsCompleted", "none") || changed
        }
        when {
            html.contains("The Sisters tend to your wounds") ||
                html.contains("The Sisters give you an invigorating massage") -> {
                val next = preferences.getInt("nunsVisits", 0) + 1
                preferences.setInt("nunsVisits", next)
                changed = true
            }
            html.contains("all of the Sisters are busy right now") -> {
                if (preferences.getInt("nunsVisits", 0) != 99) {
                    preferences.setInt("nunsVisits", 99)
                    changed = true
                }
            }
        }
        return changed
    }

    internal fun parseJunkyard(
        html: String,
        preferences: Preferences,
        context: IslandVisitContext,
    ): Boolean {
        val currentLocation = preferences.getString("currentJunkyardLocation", "")
        var tool = preferences.getString("currentJunkyardTool", "")
        var location = currentLocation
        var done = false

        val match = JUNKYARD_PATTERN.find(html)
        if (match != null) {
            val rawTool = match.groupValues[1].trim()
            tool = normalizeJunkyardTool(rawTool)
            location = canonicalizeJunkyardLocation(match.groupValues[2].trim())
        } else if (html.contains("I made this while you were off getting my tools")) {
            tool = ""
            location = ""
            done = true
        }

        var changed = false
        if (location != currentLocation) {
            preferences.setString("currentJunkyardTool", tool)
            preferences.setString("currentJunkyardLocation", location)
            changed = true
        }

        if (!done) {
            return changed
        }

        for (itemId in MOLYBDENUM_TOOLS) {
            context.consumeItem(itemId, 1)
        }
        changed = true

        when {
            html.contains("spark plug earring") ||
                html.contains("woven baling wire bracelets") ||
                html.contains("gearbox necklace") ->
                changed = setSidequestPref(preferences, "sidequestJunkyardCompleted", "hippy") || changed
            html.contains("chain necklace") ||
                html.contains("sawblade shield") ||
                html.contains("wrench bracelet") ->
                changed = setSidequestPref(preferences, "sidequestJunkyardCompleted", "fratboy") || changed
        }
        return changed
    }

    private fun normalizeJunkyardTool(rawTool: String): String {
        val suffix = if (rawTool == "wrench") "crescent wrench" else rawTool
        return "molybdenum $suffix"
    }

    private fun canonicalizeJunkyardLocation(location: String): String {
        for ((short, canonical) in JUNKYARD_AREAS) {
            if (location == short) return canonical
        }
        return location
    }

    internal fun parseOrchard(
        html: String,
        preferences: Preferences,
        context: IslandVisitContext,
    ): Boolean {
        if (!html.contains("tyranny of nature")) {
            return false
        }

        var changed = false
        if (context.hasItemId(FILTHWORM_QUEEN_HEART)) {
            context.consumeItem(FILTHWORM_QUEEN_HEART, 1)
            changed = true
        }

        val side = if (context.isWearingWarHippyOutfit()) "hippy" else "fratboy"
        changed = setSidequestPref(preferences, "sidequestOrchardCompleted", side) || changed
        if (preferences.getInt("lastFilthClearance", -1) != context.ascensionNumber) {
            preferences.setInt("lastFilthClearance", context.ascensionNumber)
            changed = true
        }
        if (preferences.getString("currentHippyStore", "none") != side) {
            preferences.setString("currentHippyStore", side)
            changed = true
        }
        ConcoctionDatabase.refreshConcoctionsNowFromLastContext()
        return true
    }

    internal fun parseLighthouse(
        html: String,
        preferences: Preferences,
        context: IslandVisitContext,
    ): Boolean {
        if (!html.contains("My bombs for you")) {
            return false
        }

        val side = if (context.isWearingWarHippyOutfit()) "hippy" else "fratboy"
        setSidequestPref(preferences, "sidequestLighthouseCompleted", side)
        context.consumeItem(GUNPOWDER, 5)
        return true
    }

    internal fun findCampMaster(url: String): CoinmasterData? {
        val camp = CAMP_PATTERN.find(url)?.groupValues?.getOrNull(1) ?: return null
        return when (camp) {
            "1" -> CoinmasterDatabase.findByNickname("dimemaster")
            "2" -> CoinmasterDatabase.findByNickname("quartersmaster")
            else -> null
        }
    }

    internal fun parseCamp(
        url: String,
        html: String,
        preferences: Preferences,
        context: IslandVisitContext = IslandVisitContext(),
        sessionLogger: SessionLogger? = null,
    ): Boolean = IslandWarCampSync.parseCampResponse(url, html, preferences, context, sessionLogger)

    internal fun parseCampTokenBalance(
        coinmaster: CoinmasterData,
        html: String,
        preferences: Preferences,
    ): Boolean {
        val property = coinmaster.property ?: return false
        val (tokenPattern, tokenTest) = campTokenConfig(coinmaster) ?: return false

        val check = if (tokenTest != null) {
            val found = html.contains(tokenTest)
            false == found
        } else {
            true
        }

        val rawBalance = if (!check) {
            "0"
        } else {
            tokenPattern.find(html)?.groupValues?.getOrNull(1) ?: return false
        }

        val balance = parseTokenBalance(rawBalance)
        if (preferences.getInt(property, 0) == balance) return false
        preferences.setInt(property, balance)
        return true
    }

    private fun campTokenConfig(coinmaster: CoinmasterData): Pair<Regex, String?>? =
        when (coinmaster.nickname) {
            "dimemaster" -> DIMEMASTER_TOKEN_PATTERN to DIMEMASTER_TOKEN_TEST
            "quartersmaster" -> QUARTERSMASTER_TOKEN_PATTERN to QUARTERSMASTER_TOKEN_TEST
            else -> null
        }

    private fun parseTokenBalance(raw: String): Int {
        val normalized = when (raw.trim()) {
            "no" -> "0"
            "one" -> "1"
            "" -> "1"
            else -> raw.replace(",", "")
        }
        return normalized.toIntOrNull() ?: 0
    }

    internal fun parseBattlefield(html: String, preferences: Preferences): Boolean {
        val match = MAP_PATTERN.find(html) ?: return false
        val fratboyImage = match.groupValues[1].toIntOrNull() ?: 0
        val hippyImage = match.groupValues[2].toIntOrNull() ?: 0

        var changed = false
        imageRange(fratboyImage)?.let { (min, max) ->
            changed = clampCounter(preferences, "fratboysDefeated", min, max) || changed
        }
        imageRange(hippyImage)?.let { (min, max) ->
            changed = clampCounter(preferences, "hippiesDefeated", min, max) || changed
        }
        return changed
    }

    internal fun imageRange(image: Int): Pair<Int, Int>? {
        if (image !in 0..32) return null
        val min = IMAGES[image]
        val max = if (min == 1000) 1000 else IMAGES[image + 1] - 1
        return min to max
    }

    private fun setSidequestPref(
        preferences: Preferences,
        prefKey: String,
        value: String,
    ): Boolean {
        if (preferences.getString(prefKey, "none") == value) return false
        preferences.setString(prefKey, value)
        return true
    }

    private fun clampCounter(
        preferences: Preferences,
        prefKey: String,
        min: Int,
        max: Int,
    ): Boolean {
        val current = preferences.getInt(prefKey, 0)
        return when {
            current < min -> {
                preferences.setInt(prefKey, min)
                true
            }
            current > max -> {
                preferences.setInt(prefKey, max)
                true
            }
            else -> false
        }
    }
}
