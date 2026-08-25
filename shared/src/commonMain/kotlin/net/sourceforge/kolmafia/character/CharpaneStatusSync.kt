package net.sourceforge.kolmafia.character

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.SpelunkyRequest
import net.sourceforge.kolmafia.session.BatManager

/** Parses charpane.php HTML for status fields when api.php status is incomplete (Phase 408). */
object CharpaneStatusSync {

    const val TRANSFUNCTIONER_NAME = "continuum transfunctioner"

    data class ParsedStatus(
        val buffedMusc: Int? = null,
        val buffedMyst: Int? = null,
        val buffedMoxie: Int? = null,
        val currentHp: Int? = null,
        val maxHp: Int? = null,
        val currentMp: Int? = null,
        val maxMp: Int? = null,
        val meat: Int? = null,
        val adventuresLeft: Int? = null,
        val mindControlLevel: Int? = null,
        val inebriety: Int? = null,
        val currentPP: Int? = null,
        val maximumPP: Int? = null,
        val youRobotEnergy: Int? = null,
        val horde: Int? = null,
        val thunder: Int? = null,
        val rain: Int? = null,
        val lightning: Int? = null,
        val wildfireWater: Int? = null,
    )

    private val compactStatsPattern = Regex(""">Mus.*?<b>(.*?)</b>.*?Mys.*?<b>(.*?)</b>.*?Mox.*?<b>(.*?)</b>""", RegexOption.DOT_MATCHES_ALL)
    private val expandedStatsPattern = Regex(""">Muscle.*?<b>(.*?)</b>.*?Mysticality.*?<b>(.*?)</b>.*?Moxie.*?<b>(.*?)</b>""", RegexOption.DOT_MATCHES_ALL)
    private val modifiedStatPattern = Regex("""<font color=(?:red|blue)>(\d+)</font>&nbsp;\((\d+)\)""")

    private val compactHpPattern = Regex("""HP:.*?<b>(.*?)/(.*?)</b>""", RegexOption.DOT_MATCHES_ALL)
    private val compactMpPattern = Regex("""MP:.*?<b>(.*?)/(.*?)</b>""", RegexOption.DOT_MATCHES_ALL)
    private val compactMeatPattern = Regex("""Meat.*?<b>(.*?)</b>""", RegexOption.DOT_MATCHES_ALL)
    private val compactAdvPattern = Regex("""Adv.*?<b>(.*?)</b>""", RegexOption.DOT_MATCHES_ALL)
    private val compactPpPattern = Regex("""PP:.*?<b>(.*?)/(.*?)</b>""", RegexOption.DOT_MATCHES_ALL)
    private val compactRobotEnergyPattern = Regex("""E:.*?<b>(\d+) / .*?</b>""", RegexOption.DOT_MATCHES_ALL)
    private val compactHordePattern = Regex("""Horde: (\d+)""")

    private val expandedHpPattern = Regex("""/(?:slim)?hp\.gif.*?<span.*?>(.*?)&nbsp;/&nbsp;(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
    private val expandedMpPattern = Regex("""/(?:slim)?mp\.gif.*?<span.*?>(.*?)&nbsp;/&nbsp;(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
    private val expandedPpPattern = Regex("""/(?:slim)?pp\.gif.*?(\d+) / (\d+)<""", RegexOption.DOT_MATCHES_ALL)
    private val expandedRobotEnergyPattern = Regex("""/(?:slim)?jigawatts\.gif.*?(\d+)""")
    private val expandedHordePattern = Regex("""/(?:slim)?zombies/horde.*?\.gif.*?Horde: (\d+)""", RegexOption.DOT_MATCHES_ALL)
    private val expandedMeatPattern = Regex("""/(?:slim)?meat\.gif.*?<span.*?>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
    private val expandedAdvPattern = Regex("""/(?:slim)?hourglass\.gif.*?<span.*?>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)

    private val compactMcPatterns = listOf(
        Regex("""MC</a>: ?(?:</td><td>)?<b>(\d+)</b>"""),
        Regex("""Radio</a>: ?(?:</td><td>)?<b>(\d+)</b>"""),
        Regex("""AOT5K</a>: ?(?:</td><td>)?<b>(\d+)</b>"""),
        Regex("""HH</a>: ?(?:</td><td>)?<b>(\d+)</b>"""),
    )
    private val expandedMcPatterns = listOf(
        Regex("""Mind Control</a>: ?(?:</td><td>)?<b>(\d+)</b>"""),
        Regex("""Detuned Radio</a>: ?(?:</td><td>)?<b>(\d+)</b>"""),
        Regex("""Annoy-o-Tron 5k</a>: ?(?:</td><td>)?<b>(\d+)</b>"""),
        Regex("""Heartbreaker's</a>: ?(?:</td><td>)?<b>(\d+)</b>"""),
    )

    private val compactDrunkPattern = Regex("""Drunk</span></td><td(?: align=left)?><b><span class="(?:blur.)?">(\d+) / (-?\d+)</span>""")
    private val expandedDrunkPatterns = listOf(
        Regex("""Drunkenness:</span></td><td(?: align=left)?><b><span class="(?:blur.)?">(\d+) / (-?\d+)</span>"""),
        Regex("""Inebriety:</span></td><td(?: align=left)?><b><span class="(?:blur.)?">(\d+) / (-?\d+)</span>"""),
        Regex("""Temulency:</span></td><td(?: align=left)?><b><span class="(?:blur.)?">(\d+) / (-?\d+)</span>"""),
        Regex("""Tipsiness:</span></td><td(?: align=left)?><b><span class="(?:blur.)?">(\d+) / (-?\d+)</span>"""),
    )

    private val raincoreThunderPattern = Regex("""Thunder:</td><td align=left><b><font color=black>(\d+) dBs""")
    private val raincoreRainPattern = Regex("""Rain:</td><td align=left><b><font color=black>(\d+) drops""")
    private val raincoreLightningPattern = Regex("""Lightning:</td><td align=left><b><font color=black>(\d+) bolts""")
    private val firecoreWaterPattern = Regex("""Water(?: Collected)?:</td><td align=left><b>([\d,]+)</b>""")

    private val eightBitScorePattern = Regex("""<font color=(\w+)><span class='nes'[^>]*?>([\d,]+)</span></font>""")

    fun hasTransfunctionerEquipped(state: CharacterState): Boolean =
        state.equipment.values.any { it.equals(TRANSFUNCTIONER_NAME, ignoreCase = true) }

    fun isCompact(html: String): Boolean = html.contains("<br>Lvl. ")

    fun parse(html: String, state: CharacterState): ParsedStatus {
        val compact = isCompact(html)
        val stats = parseStats(html, compact)
        val misc = parseMisc(html, state, compact)
        val mcd = parseMindControl(html, compact)
        val drunk = parseInebriety(html, compact)
        val raincore = if (state.isRaincore) parseRaincore(html) else Triple(null, null, null)
        val wildfire = if (state.isFirecore) parseWildfireWater(html) else null
        return ParsedStatus(
            buffedMusc = stats.first,
            buffedMyst = stats.second,
            buffedMoxie = stats.third,
            currentHp = misc.currentHp,
            maxHp = misc.maxHp,
            currentMp = misc.currentMp,
            maxMp = misc.maxMp,
            meat = misc.meat,
            adventuresLeft = misc.adventuresLeft,
            mindControlLevel = mcd,
            inebriety = drunk,
            currentPP = misc.currentPP,
            maximumPP = misc.maximumPP,
            youRobotEnergy = misc.youRobotEnergy,
            horde = misc.horde,
            thunder = raincore.first,
            rain = raincore.second,
            lightning = raincore.third,
            wildfireWater = wildfire,
        )
    }

    fun apply(
        character: KoLCharacter,
        html: String,
        preferences: Preferences? = null,
    ) {
        val state = character.state.value
        val parsed = parse(html, state)
        character.updateFromCharpane(parsed)
        ClassResourceCharpaneSync.apply(character, html)
        val mode = character.state.value.limitMode
        if (preferences != null) {
            when {
                mode.equals("spelunky", ignoreCase = true) ||
                    mode.equals("spelunk", ignoreCase = true) ||
                    html.contains(">Last Spelunk</a>") ->
                    SpelunkyRequest.parseCharpane(html, preferences, character)
                mode.equals("batman", ignoreCase = true) ||
                    html.contains("You're Batfellow") ||
                    html.contains("Gotpork City explodes") ->
                    BatManager.parseCharpane(html, preferences, character)
            }
        }
        if (preferences != null && hasTransfunctionerEquipped(state)) {
            apply8BitScore(html, preferences)
        }
    }

    private fun apply8BitScore(html: String, preferences: Preferences) {
        val match = eightBitScorePattern.find(html) ?: return
        val color = match.groupValues[1]
        val score = parseDigits(match.groupValues[2])
        preferences.setInt("8BitScore", score)
        val previousColor = preferences.getString("8BitColor", "")
        if (color != previousColor) {
            preferences.setString("8BitColor", color)
            net.sourceforge.kolmafia.data.DefaultsDatabase.resetToDefault(preferences, "8BitBonusTurns")
        }
    }

    private data class MiscValues(
        val currentHp: Int? = null,
        val maxHp: Int? = null,
        val currentMp: Int? = null,
        val maxMp: Int? = null,
        val meat: Int? = null,
        val adventuresLeft: Int? = null,
        val currentPP: Int? = null,
        val maximumPP: Int? = null,
        val youRobotEnergy: Int? = null,
        val horde: Int? = null,
    )

    private fun parseStats(html: String, compact: Boolean): Triple<Int?, Int?, Int?> {
        val pattern = if (compact) compactStatsPattern else expandedStatsPattern
        val match = pattern.find(html) ?: return Triple(null, null, null)
        return Triple(
            parseStatGroup(match.groupValues[1]),
            parseStatGroup(match.groupValues[2]),
            parseStatGroup(match.groupValues[3]),
        )
    }

    private fun parseStatGroup(raw: String): Int {
        val modified = modifiedStatPattern.find(raw)
        if (modified != null) {
            return parseDigits(modified.groupValues[1])
        }
        return parseDigits(raw)
    }

    private fun parseMisc(html: String, state: CharacterState, compact: Boolean): MiscValues {
        val hpMatch = (if (compact) compactHpPattern else expandedHpPattern).find(html)
        val currentHp = hpMatch?.groupValues?.getOrNull(1)?.let(::parseDigits)
        val maxHp = hpMatch?.groupValues?.getOrNull(2)?.let(::parseDigits)

        val meatMatch = (if (compact) compactMeatPattern else expandedMeatPattern).find(html)
        val meat = meatMatch?.groupValues?.getOrNull(1)?.let(::parseDigits)

        val advMatch = (if (compact) compactAdvPattern else expandedAdvPattern).find(html)
        val adventures = advMatch?.groupValues?.getOrNull(1)?.let(::parseDigits)

        return when {
            state.inZombiecore -> {
                val hordeMatch = (if (compact) compactHordePattern else expandedHordePattern).find(html)
                val horde = hordeMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
                MiscValues(
                    currentHp = currentHp,
                    maxHp = maxHp,
                    horde = horde,
                    currentMp = horde,
                    maxMp = horde,
                    meat = meat,
                    adventuresLeft = adventures,
                )
            }
            state.ascensionPath == AscensionPath.PLUMBER ||
                state.ascensionPath == AscensionPath.PATH_OF_THE_PLUMBER -> {
                val ppMatch = (if (compact) compactPpPattern else expandedPpPattern).find(html)
                MiscValues(
                    currentHp = currentHp,
                    maxHp = maxHp,
                    currentPP = ppMatch?.groupValues?.getOrNull(1)?.toIntOrNull(),
                    maximumPP = ppMatch?.groupValues?.getOrNull(2)?.toIntOrNull(),
                    meat = meat,
                    adventuresLeft = adventures,
                )
            }
            state.inRobocore -> {
                val energyMatch = (if (compact) compactRobotEnergyPattern else expandedRobotEnergyPattern).find(html)
                MiscValues(
                    currentHp = currentHp,
                    maxHp = maxHp,
                    youRobotEnergy = energyMatch?.groupValues?.getOrNull(1)?.toIntOrNull(),
                    meat = meat,
                    adventuresLeft = adventures,
                )
            }
            else -> {
                val mpMatch = (if (compact) compactMpPattern else expandedMpPattern).find(html)
                MiscValues(
                    currentHp = currentHp,
                    maxHp = maxHp,
                    currentMp = mpMatch?.groupValues?.getOrNull(1)?.let(::parseDigits),
                    maxMp = mpMatch?.groupValues?.getOrNull(2)?.let(::parseDigits),
                    meat = meat,
                    adventuresLeft = adventures,
                )
            }
        }
    }

    private fun parseMindControl(html: String, compact: Boolean): Int? {
        val patterns = if (compact) compactMcPatterns else expandedMcPatterns
        for (pattern in patterns) {
            val level = pattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            if (level > 0) return level
        }
        return 0
    }

    private fun parseInebriety(html: String, compact: Boolean): Int? {
        if (compact) {
            return compactDrunkPattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        }
        for (pattern in expandedDrunkPatterns) {
            val level = pattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
            if (level > 0) return level
        }
        return 0
    }

    private fun parseRaincore(html: String): Triple<Int?, Int?, Int?> =
        Triple(
            raincoreThunderPattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull(),
            raincoreRainPattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull(),
            raincoreLightningPattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull(),
        )

    private fun parseWildfireWater(html: String): Int? =
        firecoreWaterPattern.find(html)?.groupValues?.getOrNull(1)
            ?.replace(",", "")
            ?.toIntOrNull()

    private fun parseDigits(text: String): Int =
        text.replace(Regex("<[^>]*>"), "")
            .replace(Regex("\\D+"), "")
            .toIntOrNull() ?: 0
}
