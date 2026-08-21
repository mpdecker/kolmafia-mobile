package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [LeprecondoManager.visit] for choice 1556.
 * Pref sync only — no rearrange CLI / automation.
 */
object LeprecondoChoiceSync {

    const val CHOICE_ID = 1556

    private val INSTALLED_FURNITURE = Regex(
        """<img id="i(\d)" alt="(.*?) in (?:top|bottom) (?:left|right)"""",
    )
    private val REARRANGEMENTS = Regex(
        """You can rearrange the furnishings (\d) more""",
        RegexOption.IGNORE_CASE,
    )
    private val DISCOVERY_SELECT = Regex(
        """<select id="r1" name="r1">(.*?)</select>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val DISCOVERY_OPTION = Regex(
        """<option(?: selected)? value='(\d+)'""",
        RegexOption.IGNORE_CASE,
    )

    /** Desktop [LeprecondoManager.Furniture] name → id for visit parse. */
    private val FURNITURE_IDS = mapOf(
        "buckets of concrete" to 1,
        "thrift store oil painting" to 2,
        "boxes of old comic books" to 3,
        "second-hand hot plate" to 4,
        "beer cooler" to 5,
        "free mattress" to 6,
        "gigantic chess set" to 7,
        "UltraDance karaoke machine" to 8,
        "cupcake treadmill" to 9,
        "beer pong table" to 10,
        "padded weight bench" to 11,
        "internet-connected laptop" to 12,
        "sous vide laboratory" to 13,
        "programmable blender" to 14,
        "sensory deprivation tank" to 15,
        "fruit-smashing robot" to 16,
        "ManCave™ sports bar set" to 17,
        "ManCave&trade; sports bar set" to 17,
        "couch and flatscreen" to 18,
        "kegerator" to 19,
        "fine upholstered dining table set" to 20,
        "whiskeybed" to 21,
        "high-end home workout system" to 22,
        "complete classics library" to 23,
        "ultimate retro game console" to 24,
        "Omnipot" to 25,
        "fully-stocked wet bar" to 26,
        "four-poster bed" to 27,
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val installed = INSTALLED_FURNITURE.findAll(html)
            .map { match ->
                val slot = match.groupValues[1].toIntOrNull() ?: 0
                val name = match.groupValues[2]
                val id = FURNITURE_IDS.entries
                    .firstOrNull { it.key.equals(name, ignoreCase = true) }
                    ?.value ?: 0
                slot to id
            }
            .sortedBy { it.first }
            .joinToString(",") { it.second.toString() }
        preferences.setString("leprecondoInstalled", installed)

        val rearrangementsLeft = REARRANGEMENTS.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (rearrangementsLeft != null) {
            preferences.setInt("_leprecondoRearrangements", 3 - rearrangementsLeft)
            if (rearrangementsLeft > 0) {
                DISCOVERY_SELECT.find(html)?.groupValues?.getOrNull(1)?.let { optionsHtml ->
                    val discoveries = DISCOVERY_OPTION.findAll(optionsHtml)
                        .map { it.groupValues[1] }
                        .distinct()
                        .joinToString(",")
                    preferences.setString("leprecondoDiscovered", discoveries)
                }
            }
        }
        return true
    }
}
