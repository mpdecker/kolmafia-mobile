package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.banish.Banisher

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
        """(?:flees? in terror|banish(?:ed)? from|gone somewhere else|fle(?:e[sd]?|d) the (?:area|field))""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Ordered list of (distinctive substring → Banisher). Checked only when BANISH_PATTERN fires.
     * First match wins. Sourced from desktop FightRequest.java.
     */
    private val BANISHER_PATTERNS: List<Pair<String, Banisher>> = listOf(
        "throw the smokebomb at your feet"                    to Banisher.SNOKEBOMB,
        "press the secret switch"                             to Banisher.KGB_TRANQUILIZER_DART,
        "Well, I never"                                       to Banisher.MAFIA_MIDDLEFINGER_RING,
        "They run off"                                        to Banisher.THROW_LATTE_ON_OPPONENT,
        "short distance into the future"                      to Banisher.REFLEX_HAMMER,
        "walk away and decide not to see this creature again" to Banisher.FEEL_HATRED,
        "before flying out of sight"                          to Banisher.SPRING_LOADED_FRONT_BUMPER,
        "tide of beans"                                       to Banisher.BEANCANNON,
        "residual hot jelly heat"                             to Banisher.BREATHE_OUT,
        "into the ball return system"                         to Banisher.BOWL_A_CURVEBALL,
        "won't be seeing"                                     to Banisher.PANTSGIVING,
        "nowhere to be seen"                                  to Banisher.LOUDER_THAN_BOMB,
        "busy getting the cheese off"                         to Banisher.STUFFED_YAM_STINKBOMB,
        "unfurls outward in a blast"                          to Banisher.ANCHOR_BOMB,
        "toss the ice house"                                  to Banisher.ICE_HOUSE,
        "Your nanites remember the molecular structure"       to Banisher.SYSTEM_SWEEP,
        "You give a tremendous shout"                         to Banisher.BANISHING_SHOUT,
        "The Force"                                           to Banisher.SABER_FORCE,
        "champagne"                                           to Banisher.DIVINE_CHAMPAGNE_POPPER,
        "chatterbox"                                          to Banisher.CHATTERBOXING,
        "stinky cheese"                                       to Banisher.STINKY_CHEESE_EYE,
        "smoke grenade"                                       to Banisher.SMOKE_GRENADE,
        "walk away from the explosion"                        to Banisher.WALK_AWAY_FROM_EXPLOSION,
        "split pea soup"                                      to Banisher.SPLIT_PEA_SOUP,
        "tennis ball"                                         to Banisher.TENNIS_BALL,
        "cocktail napkin"                                     to Banisher.COCKTAIL_NAPKIN,
        "vivala"                                              to Banisher.V_FOR_VIVALA_MASK,
        "indigo taffy"                                        to Banisher.PULLED_INDIGO_TAFFY,
        "thunder clap"                                        to Banisher.THUNDER_CLAP,
        "dirty stinkbomb"                                     to Banisher.DIRTY_STINKBOMB,
        "peppermint bomb"                                     to Banisher.PEPPERMINT_BOMB,
        "throw the bell away"                                 to Banisher.HAROLDS_BELL,
        "skull explodes into a million"                       to Banisher.CRYSTAL_SKULL,
        "EEEEEEEEEEEEEEEEEEEEEEEK"                            to Banisher.CLASSY_MONKEY,
        "push away your opponent"                             to Banisher.BE_A_MIND_MASTER,
        "You burned that foe so hard, you won't"              to Banisher.THROWIN_EMBER,
        "deliver an epic punch"                               to Banisher.PUNCH_OUT_YOUR_FOE,
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
        val banisher = if (banished) {
            BANISHER_PATTERNS.firstOrNull { (text, _) -> html.contains(text) }?.second
                ?: Banisher.UNKNOWN
        } else Banisher.UNKNOWN
        return AdventureResult.Combat(monster, won, items, meat, stats,
            banished = banished, banisher = banisher)
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
