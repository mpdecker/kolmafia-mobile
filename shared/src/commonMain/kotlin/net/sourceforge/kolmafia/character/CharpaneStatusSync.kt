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
        familiarManager: net.sourceforge.kolmafia.familiar.FamiliarManager? = null,
    ) {
        val state = character.state.value
        val parsed = parse(html, state)
        character.updateFromCharpane(parsed)
        ClassResourceCharpaneSync.apply(character, html)
        parseAvatar(html, character, state)
        parseTitle(html, character)
        parseLevel(html, character)
        setLastAdventure(html, preferences)
        checkNoncombatForcers(html, preferences)
        checkOtherModifiers(html, preferences)
        checkFamiliar(html, character, familiarManager)
        checkClancy(html, preferences, state)
        checkYouRobot(html, character, state)
        CharpaneInteraction.applyInteraction(character, preferences)
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

    private val avatarPattern = Regex(
        """<img\s+(?:crossorigin="Anonymous"\s+|)?src=[^>]*?(?:cloudfront\.net|images\.kingdomofloathing\.com|/images)/([^>'"\s]+)""",
        RegexOption.IGNORE_CASE,
    )
    private val titlePattern = Regex(
        """<a class=nounder target=mainpane href="charsheet\.php"><b>[^>]*?</b></a><br>(?<title>[^<]*?)<br>[^<]*?<table""",
        RegexOption.IGNORE_CASE,
    )
    private val levelPattern = Regex("""<br>(?:\(?Level |Lvl\. )(\d+)\)?<""")

    fun parseAvatar(html: String, character: KoLCharacter, state: CharacterState = character.state.value) {
        if (state.inRobocore) return
        val path = avatarPattern.find(html)?.groupValues?.getOrNull(1) ?: return
        character.setAvatar(path)
    }

    fun parseTitle(html: String, character: KoLCharacter) {
        val title = titlePattern.find(html)?.groups?.get("title")?.value ?: return
        if (title.isNotBlank()) character.setTitle(title.trim())
    }

    fun parseLevel(html: String, character: KoLCharacter) {
        val level = levelPattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return
        character.setLevel(level)
    }

    private val noncombatForcerPattern = Regex(
        """<b><font size=2>Adventure Modifiers:</font></b><br><div style='text-align: left'><small>(.*?)</small>""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val noncombatForcers = mapOf(
        "You are temporarily in the mostly-combatless world of Clara's Bell." to "clara",
        "Your spikes are scaring away most monsters." to "spikolodon",
        "With the jelly all over you, you are probably not going to encounter anything" to "stench jelly",
        "You've engaged exit mode on your cincho and will avoid most combats." to "cincho exit",
        "You are avoiding fights until something cool happens." to "sneakisol",
        "Your tuba playing has scared away most monsters." to "band tuba",
        "You triggered an avalanche clearing the area of monsters." to "ski avalanche",
        "A sniper is guiding you out of trouble." to "sniper support",
    )

    fun checkNoncombatForcers(html: String, preferences: Preferences?) {
        preferences ?: return
        val match = noncombatForcerPattern.find(html)
        val active = match != null
        preferences.setBoolean(Preferences.Keys.NONCOMBAT_FORCER_ACTIVE, active)
        if (!active) {
            preferences.setString("noncombatForcers", "")
            return
        }
        val body = match!!.groupValues[1]
        val tokens = body.split("<br>", ignoreCase = true)
            .map { it.trim() }
            .mapNotNull { desc ->
                noncombatForcers.entries.firstOrNull { (key, _) ->
                    desc.startsWith(key) || desc.contains(key)
                }?.value
            }
        preferences.setString("noncombatForcers", tokens.joinToString("|"))
    }

    private val legendaryAmygdalaPattern =
        Regex("""Your amygdala full of legendary noodles will lead you into (\d+) more fight""")
    private val legendarySkinPattern =
        Regex("""Your skin will be really tough for (\d+) more fight""")
    private val legendaryStomachPattern =
        Regex("""Your stomach will be more efficient for (\d+) more meal""")

    fun checkOtherModifiers(html: String, preferences: Preferences?) {
        preferences ?: return
        preferences.setInt(
            "legendaryNoodlesAmygdala",
            legendaryAmygdalaPattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0,
        )
        preferences.setInt(
            "legendaryNoodlesSkin",
            legendarySkinPattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0,
        )
        preferences.setInt(
            "legendaryNoodlesStomach",
            legendaryStomachPattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0,
        )
    }

    private val compactLastAdventurePattern = Regex(
        """<a onclick=[^>]+ title="Last Adventure: ([^"]+)" target=mainpane href="([^"]*)">.*?</a>:""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val expandedLastAdventurePattern = Regex(
        """Last Adventure:</a></b></font><br>\s*<table.*?><tr><td><font.*?><a .*?href="(?<link>[^"]*)">(?<name>[^<]*)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val compactTrailPattern = Regex(
        """<span id="lastadvmenu"[^>]*><font size=1>(?<trail>.*?)</font></span>""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val trailElementPattern = Regex(
        """<nobr><a [^>]*?href="(?<link>.*?)">(?<name>[^<]*?)</a></nobr>""",
    )

    fun setLastAdventure(html: String, preferences: Preferences?) {
        preferences ?: return
        val compact = isCompact(html)
        var adventureName: String? = null
        var adventureUrl: String? = null
        var trailString: String? = null
        if (compact) {
            compactLastAdventurePattern.find(html)?.let {
                adventureName = it.groupValues[1]
                adventureUrl = it.groupValues[2]
            }
            trailString = compactTrailPattern.find(html)?.groups?.get("trail")?.value
        } else {
            expandedLastAdventurePattern.find(html)?.let { m ->
                adventureName = m.groups["name"]?.value
                adventureUrl = m.groups["link"]?.value
            }
            // Trail after the last-adventure table (optional)
            val after = adventureName?.let { name ->
                val idx = html.indexOf(name)
                if (idx >= 0) html.substring(idx) else null
            }
            if (after != null) {
                trailString = trailElementPattern.findAll(after).joinToString("|") {
                    it.groups["name"]!!.value
                }.takeIf { it.isNotBlank() }
            }
        }
        if (adventureName.isNullOrBlank() || adventureName == "The Naughty Sorceress' Tower") {
            return
        }
        preferences.setString("lastAdventure", adventureName!!)
        if (!adventureUrl.isNullOrBlank()) {
            preferences.setString("lastAdventureUrl", adventureUrl!!)
        }
        if (!trailString.isNullOrBlank()) {
            val trail = if (compact) {
                val list = mutableListOf<String>()
                trailElementPattern.findAll(trailString!!).forEach { list.add(it.groups["name"]!!.value) }
                list
            } else {
                listOf(adventureName!!) + trailString!!.split("|").filter {
                    it.isNotBlank() && it != adventureName
                }
            }
            if (trail.isNotEmpty()) {
                preferences.setString("lastAdventureTrail", trail.joinToString("|"))
            }
        }
    }

    private val compactFamiliarWeightPattern = Regex("""<br>(\d+) lb""")
    private val expandedFamiliarWeightPattern = Regex("""<b>(\d+)</b> pound""")
    private val familiarImagePattern = Regex(
        """<a.*?class="familiarpick"><img.*?((?:item|other)images)/(.*?\.(?:gif|png))""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun checkFamiliar(
        html: String,
        character: KoLCharacter,
        familiarManager: net.sourceforge.kolmafia.familiar.FamiliarManager? = null,
    ) {
        val state = character.state.value
        if (state.isAxecore || state.inPokefam || state.inRobocore) return
        val compact = isCompact(html)
        val weightPattern = if (compact) compactFamiliarWeightPattern else expandedFamiliarWeightPattern
        val weight = weightPattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val wellFed = html.contains("well-fed", ignoreCase = true)
        val image = familiarImagePattern.find(html)?.groupValues?.getOrNull(2)
        if (weight != null || image != null) {
            character.setFamiliarPane(weight = weight, wellFed = wellFed, image = image)
        } else if (wellFed) {
            character.setFamiliarPane(wellFed = true)
        }
        if (weight != null) {
            familiarManager?.applyActiveWeightXpLocally(weight, state.familiarExp)
            familiarManager?.applyActiveFeastedLocally(wellFed)
        }
    }

    private val compactClancyPattern = Regex(
        """otherimages/clancy_([123])(_att)?\.gif.*?L\. (\d+)""",
        RegexOption.DOT_MATCHES_ALL,
    )
    private val expandedClancyPattern = Regex(
        """<b>Clancy</b>.*?Level <b>(\d+)</b>.*?otherimages/clancy_([123])(_att)?\.gif""",
        RegexOption.DOT_MATCHES_ALL,
    )

    fun checkClancy(html: String, preferences: Preferences?, state: CharacterState) {
        preferences ?: return
        if (!state.isAxecore) return
        val compact = isCompact(html)
        val match = (if (compact) compactClancyPattern else expandedClancyPattern).find(html) ?: return
        val level: Int
        val instrument: String
        val wantsAttention: Boolean
        if (compact) {
            instrument = match.groupValues[1]
            wantsAttention = match.groupValues[2].isNotBlank()
            level = match.groupValues[3].toIntOrNull() ?: return
        } else {
            level = match.groupValues[1].toIntOrNull() ?: return
            instrument = match.groupValues[2]
            wantsAttention = match.groupValues[3].isNotBlank()
        }
        preferences.setInt("clancyLevel", level)
        preferences.setString(
            "clancyInstrument",
            when (instrument) {
                "1" -> "sackbut"
                "2" -> "crumhorn"
                "3" -> "lute"
                else -> ""
            },
        )
        preferences.setBoolean("clancyWantsAttention", wantsAttention)
    }

    private val youRobotScrapsExpanded = Regex("""scrap\.gif.*?>([\d,]+)<""")
    private val youRobotScrapsCompact = Regex("""Scrap.*?<b>([\d,]+)</b>""", RegexOption.DOT_MATCHES_ALL)

    fun checkYouRobot(html: String, character: KoLCharacter, state: CharacterState) {
        if (!state.inRobocore) return
        val compact = isCompact(html)
        val pattern = if (compact) youRobotScrapsCompact else youRobotScrapsExpanded
        val scraps = pattern.find(html)?.groupValues?.getOrNull(1)
            ?.replace(",", "")
            ?.toIntOrNull()
            ?: return
        character.setYouRobotScraps(scraps)
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
