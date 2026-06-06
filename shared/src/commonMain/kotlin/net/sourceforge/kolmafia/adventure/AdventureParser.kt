package net.sourceforge.kolmafia.adventure

object AdventureParser {
    private val ITEM_GAINED = Regex("""You acquire an item:\s*<b>(.*?)</b>""")
    private val MEAT_GAINED = Regex("""You gain ([\d,]+) Meat""")
    private val STAT_GAINED = Regex("""You gain ([\d,]+) (\w+) \(\d+ exp\)""")
    private val WIN_PATTERN = Regex("""You win the fight""")
    private val CHOICE_ID = Regex("""name="whichchoice"\s*value="(\d+)"""")
    private val CHOICE_OPTION = Regex("""option=(\d+)">(.*?)</a>""")
    private val MONSTER_NAME = Regex("""<span id='monname'>(.*?)</span>""")
    private val ENCOUNTER_NAME = Regex("""<b>([^<]{3,60})</b>""")
    private val BANISH_PATTERN = Regex(
        """(?:flees? in terror|banished? from|gone somewhere else|flees? the (?:area|field)|flee[sd]? the (?:area|field))""",
        RegexOption.IGNORE_CASE
    )

    fun parseAdventureResponse(html: String, finalUrl: String): AdventureResult = when {
        finalUrl.contains("fight.php") || html.contains("You're fighting") -> parseCombatStart(html)
        finalUrl.contains("choice.php") || html.contains("whichchoice") -> parseChoice(html)
        else -> parseNonCombat(html)
    }

    fun parseFightResult(html: String): AdventureResult.Combat {
        val won = WIN_PATTERN.containsMatchIn(html)
        val monster = MONSTER_NAME.find(html)?.groupValues?.get(1) ?: "Unknown"
        val items = ITEM_GAINED.findAll(html).map { it.groupValues[1].trim() }.toList()
        val meat = parseMeat(html)
        val stats = parseStats(html)
        val banished = BANISH_PATTERN.containsMatchIn(html)
        return AdventureResult.Combat(monster, won, items, meat, stats, banished = banished)
    }

    private fun parseCombatStart(html: String): AdventureResult.Combat {
        val monster = MONSTER_NAME.find(html)?.groupValues?.get(1) ?: "Unknown"
        return AdventureResult.Combat(monster, won = false)
    }

    private fun parseChoice(html: String): AdventureResult.Choice {
        val choiceId = CHOICE_ID.find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        val options = CHOICE_OPTION.findAll(html).map { it.groupValues[2].trim() }.toList()
        return AdventureResult.Choice(choiceId, "Choice Adventure", options, responseText = html)
    }

    private fun parseNonCombat(html: String): AdventureResult.NonCombat {
        val name = ENCOUNTER_NAME.find(html)?.groupValues?.get(1) ?: "Encounter"
        val items = ITEM_GAINED.findAll(html).map { it.groupValues[1].trim() }.toList()
        val meat = parseMeat(html)
        return AdventureResult.NonCombat(name, html, items, meat)
    }

    private fun parseMeat(html: String): Int =
        MEAT_GAINED.find(html)?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull() ?: 0

    private fun parseStats(html: String): Map<String, Int> =
        STAT_GAINED.findAll(html).associate { m ->
            val value = m.groupValues[1].replace(",", "").toIntOrNull() ?: 0
            m.groupValues[2] to value
        }
}
