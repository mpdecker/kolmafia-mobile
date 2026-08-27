package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [FightRequest.processFamiliarAction] high-traffic hub (Phases 1506–1520):
 * commerce ghost, patriotic eagle, cookbookbat quest strings, bellydancer pickpockets.
 *
 * Nanorhino / Melodramedary / cookbookbat timer remain in [FightIotmSync] to avoid
 * double-counting charges.
 */
object FightFamiliarMessageSync {

    const val FAMILIAR_GHOST_COMMERCE = 282
    const val FAMILIAR_PATRIOTIC_EAGLE = 293
    const val FAMILIAR_COOKBOOKBAT = 288

    private val GHOST_QUEST = listOf(
        Regex("""Better get an? (.*?) while there's still some left!""", RegexOption.IGNORE_CASE),
        Regex("""[Bb]uy an? (.*?) before they all sell out""", RegexOption.IGNORE_CASE),
        Regex("""Don't forget to buy an? (.*?)!""", RegexOption.IGNORE_CASE),
        Regex("""Did you buy an? (.*?) yet\?""", RegexOption.IGNORE_CASE),
        Regex("""Hey pal, you should buy an? (.*?)!""", RegexOption.IGNORE_CASE),
        Regex("""Quick, buy an? (.*?)!""", RegexOption.IGNORE_CASE),
        Regex("""Buy an? (.*?)!""", RegexOption.IGNORE_CASE),
    )

    private val GHOST_COMPLETE = listOf(
        Regex("""Nice, you bought an? (.*?)!""", RegexOption.IGNORE_CASE),
        Regex("""Oh,? good, you got an? (.*?)(?: before they sold out)?!""", RegexOption.IGNORE_CASE),
    )

    private val COOKBOOKBAT_QUEST = listOf(
        Regex(
            """"As I recall, (?<ingredient>.*?) was common in (?<location>.*?), back in my day\. +Perhaps if you kill +(?:an?|the|some)? (?<monster>.*?), you'll find one\."""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """"If memory serves, (?<ingredient>.*?) was very popular in (?<location>.*?), during my time\. +Perhaps if you find +(?:an?|the|some)? (?<monster>.*?), you'll collect one,"""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """"My recollection is that (?<ingredient>.*?) was often collected from +(?:an?|the|some)? (?<monster>.*?)\. +If I recall correctly, you can hunt them in (?<location>.*?)\."""",
            RegexOption.IGNORE_CASE,
        ),
    )

    private val COOKBOOKBAT_REMINDER = listOf(
        Regex(
            """"If I recall, I suggested that you look for +(?:an?|the|some)? (?<monster>.*?)\."""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """"If my ancient memory serves, I suggested looking in (?<location>.*?)\."""",
            RegexOption.IGNORE_CASE,
        ),
        Regex(
            """"According to my memories, +(?:an?|the|some)? (?<monster>.*?) at (?<location>.*?) may have what you're looking for\."""",
            RegexOption.IGNORE_CASE,
        ),
    )

    private const val COOKBOOKBAT_INGREDIENTS = "\"As I recall,  you could use these,\""

    /** Desktop fight-start charge for commerce ghost. */
    fun noteFightStart(preferences: Preferences?, familiarId: Int) {
        preferences ?: return
        if (familiarId == FAMILIAR_GHOST_COMMERCE) {
            preferences.setInt(
                "commerceGhostCombats",
                preferences.getInt("commerceGhostCombats", 0) + 1,
            )
        }
    }

    fun apply(
        html: String,
        preferences: Preferences?,
        familiarId: Int = 0,
        familiarImage: String = "",
    ): Boolean {
        if (preferences == null || html.isBlank()) return false
        var changed = false
        changed = applyBellydancer(html, preferences) || changed
        when {
            familiarId == FAMILIAR_GHOST_COMMERCE ||
                familiarImage.contains("cghost_commerce", ignoreCase = true) ->
                changed = applyGhostOfCommerce(html, preferences) || changed
            familiarId == FAMILIAR_PATRIOTIC_EAGLE ||
                familiarImage.contains("pateagle", ignoreCase = true) ->
                changed = applyPatrioticEagle(html, preferences) || changed
            familiarId == FAMILIAR_COOKBOOKBAT ||
                familiarImage.contains("bbat_fam", ignoreCase = true) ->
                changed = applyCookbookbatQuest(html, preferences) || changed
        }
        return changed
    }

    fun applyGhostOfCommerce(html: String, preferences: Preferences): Boolean {
        for (p in GHOST_QUEST) {
            p.find(html)?.groupValues?.getOrNull(1)?.let { item ->
                preferences.setString("commerceGhostItem", item.trim())
                preferences.setInt("commerceGhostCombats", 10)
                return true
            }
        }
        for (p in GHOST_COMPLETE) {
            if (p.containsMatchIn(html)) {
                preferences.setString("commerceGhostItem", "")
                preferences.setInt("commerceGhostCombats", 0)
                return true
            }
        }
        return false
    }

    fun applyPatrioticEagle(html: String, preferences: Preferences): Boolean {
        var changed = false
        if (html.contains("throat is still too raw") ||
            html.contains("screech and starts coughing")
        ) {
            preferences.setInt(
                "screechCombats",
                (preferences.getInt("screechCombats", 0) - 1).coerceAtLeast(0),
            )
            changed = true
        }
        if (html.contains("I'm ready to screech") ||
            html.contains("screech and then somehow smiles")
        ) {
            preferences.setInt("screechCombats", 0)
            changed = true
        }
        return changed
    }

    fun applyCookbookbatQuest(html: String, preferences: Preferences): Boolean {
        var changed = false
        if (html.contains(COOKBOOKBAT_INGREDIENTS)) {
            preferences.setInt("cookbookbatIngredientsCharge", 0)
            changed = true
        }
        for (p in COOKBOOKBAT_QUEST) {
            p.find(html)?.let { m ->
                m.groups["ingredient"]?.value?.let {
                    preferences.setString("_cookbookbatQuestIngredient", it.trim())
                }
                m.groups["location"]?.value?.let {
                    preferences.setString("_cookbookbatQuestLastLocation", it.trim())
                }
                m.groups["monster"]?.value?.let {
                    preferences.setString("_cookbookbatQuestMonster", it.trim())
                }
                preferences.setInt("_cookbookbatCombatsUntilNewQuest", 6)
                return true
            }
        }
        for (p in COOKBOOKBAT_REMINDER) {
            p.find(html)?.let { m ->
                m.groups["monster"]?.value?.let {
                    preferences.setString("_cookbookbatQuestMonster", it.trim())
                }
                m.groups["location"]?.value?.let {
                    preferences.setString("_cookbookbatQuestLastLocation", it.trim())
                }
                return true
            }
        }
        if (html.contains("looks smug as you follow their instructions.") ||
            html.contains("As I recall, this is where I told you to look.")
        ) {
            preferences.setString("_cookbookbatQuestMonster", "")
            preferences.setString("_cookbookbatQuestIngredient", "")
            return true
        }
        return changed
    }

    fun applyBellydancer(html: String, preferences: Preferences): Boolean {
        if (html.contains("'s dancing, your foe doesn't notice that she's going through") ||
            html.contains(
                "'s veils flutter across your opponent's field of view, obscuring the sight of",
            ) ||
            html.contains("dances lithely around your opponent, distracting")
        ) {
            preferences.setInt(
                "_bellydancerPickpockets",
                preferences.getInt("_bellydancerPickpockets", 0) + 1,
            )
            return true
        }
        return false
    }
}
