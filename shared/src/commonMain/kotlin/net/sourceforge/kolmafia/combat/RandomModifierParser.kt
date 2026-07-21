package net.sourceforge.kolmafia.combat

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDefinition

object RandomModifierParser {

    data class ParseResult(
        val strippedName: String,
        val modifiers: List<String>,
    )

    private val MONSTER_ID = Regex("""<!-- MONSTERID: (\d+) -->""")

    private val EXTRA_MODIFIERS = setOf("powerPixel", "shrunk", "mimeo")

    /** Desktop [MonsterData.crazyModifierMapping] → OCRS token to modifier name. */
    private val crazySummerModifiers: Map<String, String> = mapOf(
        "annoying" to "annoying",
        "artisanal" to "artisanal",
        "askew" to "askew",
        "blinking" to "phase-shifting",
        "blue" to "ice-cold",
        "blurry" to "blurry",
        "bouncing" to "bouncing",
        "broke" to "broke",
        "clingy" to "clingy",
        "cloned" to "cloned",
        "cloud" to "cloud-based",
        "clowny" to "clowning",
        "crimbo" to "yuletide",
        "curse" to "cursed",
        "disguised" to "disguised",
        "drunk" to "drunk",
        "electric" to "electrified",
        "flies" to "filthy",
        "flip" to "Australian",
        "floating" to "floating",
        "fragile" to "fragile",
        "fratty" to "fratty",
        "frozen" to "frozen",
        "generous" to "generous",
        "ghostly" to "ghostly",
        "gray" to "spooky",
        "green" to "stinky",
        "haunted" to "haunted",
        "hilarious" to "hilarious",
        "hopping" to "hopping-mad",
        "hot" to "red-hot",
        "huge" to "huge",
        "invisible" to "invisible",
        "jitter" to "jittery",
        "lazy" to "lazy",
        "leet" to "1337",
        "mirror" to "left-handed",
        "narcissistic" to "narcissistic",
        "obscene" to "obscene",
        "optimal" to "optimal",
        "patriotic" to "American",
        "pixellated" to "pixellated",
        "pulse" to "throbbing",
        "purple" to "sleazy",
        "quacking" to "quacking",
        "rainbow" to "tie-dyed",
        "red" to "red-hot",
        "rotate" to "twirling",
        "shakes" to "shaky",
        "short" to "short",
        "shy" to "shy",
        "skinny" to "skinny",
        "sparkling" to "solid gold",
        "spinning" to "cartwheeling",
        "stingy" to "stingy",
        "swearing" to "foul-mouthed",
        "ticking" to "ticking",
        "tiny" to "tiny",
        "turgid" to "turgid",
        "unlucky" to "unlucky",
        "unstoppable" to "unstoppable",
        "untouchable" to "untouchable",
        "wet" to "wet",
        "wobble" to "dancin'",
        "xray" to "negaverse",
        "yellow" to "cowardly",
        "zoom" to "restless",
    )

    fun parseMonsterId(responseText: String): Int? =
        MONSTER_ID.find(responseText)?.groupValues?.get(1)?.toIntOrNull()

    /**
     * Port of desktop [AdventureRequest.handleRandomModifiers].
     * Returns stripped canonical name and resolved modifier list.
     */
    fun parseRandomModifiers(monsterName: String, responseText: String): ParseResult {
        if (!responseText.contains("var ocrs")) {
            return ParseResult(monsterName, emptyList())
        }

        val ocrsTokens = extractOcrsTokens(responseText)
        if (ocrsTokens.isEmpty()) {
            return ParseResult(monsterName, emptyList())
        }

        var name = monsterName
        var trimmed = ""
        when {
            name.startsWith("The ", ignoreCase = true) -> {
                trimmed = name.substring(0, 4)
                name = name.substring(4)
            }
            name.startsWith("a ") -> {
                trimmed = name.substring(0, 2)
                name = name.substring(2)
            }
        }

        val modifiers = mutableListOf<String>()
        val count = ocrsTokens.lastIndex
        for (j in ocrsTokens.indices) {
            val token = ocrsTokens[j]
            if (token == "drippy") continue

            if (token in EXTRA_MODIFIERS) {
                modifiers.add(token)
                continue
            }

            val mapped = crazySummerModifiers[token]
            if (mapped == null) {
                modifiers.add(token)
                continue
            }

            modifiers.add(mapped)
            val remove = mapped + if (j == count) " " else ", "
            name = singleStringDelete(name, remove)
        }

        return ParseResult(trimmed + name.trim(), modifiers)
    }

    fun resolveTemplate(
        strippedName: String,
        responseText: String,
        gameDatabase: GameDatabase?,
    ): MonsterDefinition? {
        if (gameDatabase == null) return null
        parseMonsterId(responseText)?.let { id ->
            gameDatabase.monster(id)?.let { return it }
        }
        return gameDatabase.monster(strippedName)
    }

    private fun extractOcrsTokens(responseText: String): List<String> {
        val ocrsStart = responseText.indexOf("var ocrs")
        if (ocrsStart == -1) return emptyList()
        val end = responseText.indexOf(';', ocrsStart).let { if (it == -1) responseText.length else it }
        val scriptText = responseText.substring(ocrsStart, end)
        val segments = scriptText.split("\"")
        if (segments.size < 3) return emptyList()
        val tokens = mutableListOf<String>()
        for (i in 1 until segments.lastIndex) {
            val segment = segments[i].trim()
            if (segment.isEmpty() || segment == "," || segment.contains(":")) continue
            tokens.add(segment)
        }
        return tokens
    }

    private fun singleStringDelete(original: String, search: String): String {
        val index = original.indexOf(search)
        if (index == -1) return original
        return original.removeRange(index, index + search.length)
    }
}
