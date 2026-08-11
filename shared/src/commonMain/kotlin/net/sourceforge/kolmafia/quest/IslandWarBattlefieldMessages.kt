package net.sourceforge.kolmafia.quest

/** Desktop [IslandManager] battlefield victory message tables. */
object IslandWarBattlefieldMessages {

    val HIPPY_MESSAGES: List<List<String>> = listOf(
        listOf("M.C. Escher", "protesting the war", "out of commission", "pacifist beliefs", "garotting", "soapy water", "his own nasty breath", "smoke and dust", "sugar water", "seems to be enjoying it", "astral projection", "knock another hippy into the void", "blasting a nearby hippy"),
        listOf("three hippies", "vodka and pain", "cheap aftershave", "lob a sake bomb"),
        listOf("skunky beer", "brushing burning coals", "subsequent exhale", "hot kabobs"),
        listOf("naked human body", "personal airship", "slightly envious", "guess the outcome"),
        listOf("Absolutely nothing", "glass sculpture", "one more six-pack"),
        listOf("planks of wood", "SWAT vans", "wooden barrel"),
    )

    val FRAT_MESSAGES: List<List<String>> = listOf(
        listOf("karmic retribution", "herbal brownies", "homeopathic healing", "much lighter fluid", "meaty goodness", "Ultimate Frisbee", "alcohol poisoning", "entranced by the sun", "Marxist spell", "smash the can", "bulb of garlic", "knock another frat boy into the void", "blasting a nearby frat boy"),
        listOf("three attacking", "three frat boys", "three-way", "stock market crash"),
        listOf("gopher hole", "runaway Mobile Sweat Lodge", "red-hot coals.", "purple squirrels"),
        listOf("platoon of", "funk of hippies"),
        listOf("regiment"),
        listOf("scream like schoolgirls", "en masse", "mobile homes"),
    )

    fun battlefieldDelta(responseText: String, messages: List<List<String>>): Int {
        var test = 2
        for (tier in messages) {
            if (tier.any { responseText.contains(it) }) {
                return test
            }
            test *= 2
        }
        return 1
    }

    private val AREA_UNLOCK = intArrayOf(64, 192, 458)

    private val HIPPY_AREA_UNLOCK = arrayOf("Lighthouse", "Junkyard", "Arena")

    private val FRATBOY_AREA_UNLOCK = arrayOf("Orchard", "Nunnery", "Farm")

    private val HERO_UNLOCK = intArrayOf(501, 601, 701, 801, 901)

    private val HIPPY_HERO = arrayOf(
        "Slow Talkin' Elliot",
        "Neil",
        "Zim Merman",
        "the C.A.R.N.I.V.O.R.E. Operative",
        "the Glass of Orange Juice",
    )

    private val FRATBOY_HERO = arrayOf(
        "the Next-Generation Frat Boy",
        "Monty Basingstoke-Pratt, IV",
        "Brutus, the toga-clad lout",
        "Danglin' Chad",
        "the War Frat Streaker",
    )

    fun victoryMessage(
        defeatingFratSide: Boolean,
        last: Int,
        current: Int,
        isKingdomOfExploathing: Boolean,
    ): String {
        val delta = current - last
        val side = if (defeatingFratSide) {
            if (delta == 1) "frat boy" else "frat boys"
        } else {
            if (delta == 1) "hippy" else "hippies"
        }
        val total = if (isKingdomOfExploathing) 333 else 1000
        return "$delta $side defeated; $current down, ${total - current} left."
    }

    fun areaMessage(
        defeatingFratSide: Boolean,
        last: Int,
        current: Int,
        isKingdomOfExploathing: Boolean,
    ): String? {
        if (isKingdomOfExploathing) return null
        val areas = if (defeatingFratSide) HIPPY_AREA_UNLOCK else FRATBOY_AREA_UNLOCK
        for (i in AREA_UNLOCK.indices) {
            val threshold = AREA_UNLOCK[i]
            if (last < threshold && current >= threshold) {
                return "The ${areas[i]} is now accessible in this uniform!"
            }
        }
        return null
    }

    fun heroMessage(
        defeatingFratSide: Boolean,
        last: Int,
        current: Int,
        isKingdomOfExploathing: Boolean,
    ): String? {
        if (isKingdomOfExploathing) return null
        val heroes = if (defeatingFratSide) FRATBOY_HERO else HIPPY_HERO
        for (i in HERO_UNLOCK.indices) {
            val threshold = HERO_UNLOCK[i]
            if (last < threshold && current >= threshold) {
                return "Keep your eyes open for ${heroes[i]}!"
            }
        }
        return null
    }

    fun finishWarMessage(loser: String): String = when (loser) {
        "fratboys" -> "War finished: fratboys defeated"
        "hippies" -> "War finished: hippies defeated"
        "both" -> "War finished: both sides defeated"
        else -> "War finished: $loser defeated"
    }
}
