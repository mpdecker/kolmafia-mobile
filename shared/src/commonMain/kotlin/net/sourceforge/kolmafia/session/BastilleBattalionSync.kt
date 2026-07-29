package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ChoiceUtilities
import net.sourceforge.kolmafia.data.BastilleDatabase
import net.sourceforge.kolmafia.data.BastilleDatabase.Castle
import net.sourceforge.kolmafia.data.BastilleDatabase.Stat
import net.sourceforge.kolmafia.data.BastilleDatabase.Stats
import net.sourceforge.kolmafia.data.BastilleDatabase.Style
import net.sourceforge.kolmafia.data.BastilleDatabase.Upgrade
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Bastille Battalion (Battle Royale for Cheese) choice pref sync.
 * Mirrors desktop [BastilleBattalionManager] visit/pre/post choice hooks.
 */
object BastilleBattalionSync {

    const val CHOICE_RIG = 1313
    const val CHOICE_MASTER_OF_NONE = 1314
    const val CHOICE_CASTLE_VS_CASTLE = 1315
    const val CHOICE_GAME_OVER = 1316
    const val CHOICE_HELLO_TO_ARMS = 1317
    const val CHOICE_DEFENSIVE_POSTURING = 1318
    const val CHOICE_CHEESE_SEEKING = 1319

    private val BASTILLE_CHOICES = setOf(
        CHOICE_RIG,
        CHOICE_MASTER_OF_NONE,
        CHOICE_CASTLE_VS_CASTLE,
        CHOICE_GAME_OVER,
        CHOICE_HELLO_TO_ARMS,
        CHOICE_DEFENSIVE_POSTURING,
        CHOICE_CHEESE_SEEKING,
    )

    private val IMAGE_PATTERN = Regex(
        """<img style='(.*top: (\d+).*?; left: (\d+).*?;.*?)'[^>]*otherimages/bbatt/([^>]*)>""",
    )
    private val CASTLE_PATTERN =
        Regex("""the nearest enemy castle is ((.*?), (an? .*?)\.)""")
    private val LOOMING_CASTLE_PATTERN =
        Regex("""otherimages/bbatt/([a-z]+_3\.png)""")
    private val TURN_PATTERN = Regex("""\(turn #(\d+)\)""")
    private val BATTLE_PATTERN = Regex(
        """(Military|Castle|Psychological) results:.*?(Your|Their) (attack strength|defense) is (higher|lower) than (your|their) (defense|attack strength)""",
    )
    private val CHEESE_PATTERN = Regex("""You gain (\d+) cheese!""")
    private val TOTAL_CHEESE_PATTERN =
        Regex("""You survived for (\d+) turns and collected ([\d,]+) cheese""")

    private val PIXELS = Stats(ma = 124, md = 240, ca = 124, cd = 240, pa = 124, pd = 240)

    private val imageToStyle = Style.entries.associateBy { it.image }

    private var currentStyles = mutableMapOf<Upgrade, Style>()
    private var currentStats = Stats()
    private var currentCastle: Castle? = null

    fun isBastilleChoice(choiceId: Int): Boolean = choiceId in BASTILLE_CHOICES

    fun syncVisit(choiceId: Int, html: String, url: String?, prefs: Preferences) {
        if (url?.contains("forceoption=0") == true) {
            loadStats(prefs)
            parseStyles(html, prefs)
        }

        if (choiceId in CHOICE_MASTER_OF_NONE..CHOICE_CHEESE_SEEKING) {
            parseChoiceEncounter(choiceId, html, prefs)
        }

        when (choiceId) {
            CHOICE_RIG -> return
            CHOICE_MASTER_OF_NONE -> {
                parseTurn(html, prefs)
                clearChoices(prefs)
            }
            CHOICE_CASTLE_VS_CASTLE -> {
                parseLoomingCastle(html)
                clearChoices(prefs)
            }
            CHOICE_GAME_OVER -> return
            CHOICE_HELLO_TO_ARMS, CHOICE_DEFENSIVE_POSTURING, CHOICE_CHEESE_SEEKING ->
                getChoices(html, prefs)
        }
    }

    fun syncPreChoice(choiceId: Int, decision: Int, prefs: Preferences) {
        when (choiceId) {
            CHOICE_CASTLE_VS_CASTLE -> {
                if (decision != 0) {
                    startBattle(decision, prefs)
                }
            }
            CHOICE_HELLO_TO_ARMS, CHOICE_DEFENSIVE_POSTURING, CHOICE_CHEESE_SEEKING -> {
                if (decision in 1..3) {
                    prefs.setString(PREF_LAST_ENCOUNTER, prefs.getString("${PREF_CHOICE}$decision"))
                }
            }
        }
    }

    fun syncPostChoice(
        choiceId: Int,
        decision: Int,
        html: String,
        prefs: Preferences,
        activeEffectNames: Set<String> = emptySet(),
    ) {
        loadStats(prefs)
        when (choiceId) {
            CHOICE_RIG -> {
                when {
                    decision in 1..4 -> {
                        parseStyles(html, prefs)
                        checkPredictions(prefs)
                    }
                    decision == 5 -> {
                        startGame(prefs)
                        parseCastle(html, prefs)
                        parseTurn(html, prefs)
                        parseStyles(html, prefs)
                        logBoosts(prefs, activeEffectNames)
                    }
                }
            }
            CHOICE_MASTER_OF_NONE -> return
            CHOICE_CASTLE_VS_CASTLE -> {
                endBattle(html, prefs)
                when (ChoiceUtilities.extractChoiceId(html)) {
                    CHOICE_MASTER_OF_NONE -> {
                        parseCastle(html, prefs)
                    }
                    CHOICE_GAME_OVER -> endGame(prefs)
                }
            }
            CHOICE_GAME_OVER -> return
            CHOICE_HELLO_TO_ARMS, CHOICE_DEFENSIVE_POSTURING, CHOICE_CHEESE_SEEKING -> {
                collectCheese(html, prefs)
                if (!parseTurn(html, prefs)) {
                    nextTurn(prefs)
                }
                parseNeedles(html, prefs)
            }
        }
    }

    private fun parseChoiceEncounter(choiceId: Int, html: String, prefs: Preferences) {
        if (choiceId in CHOICE_MASTER_OF_NONE..CHOICE_CHEESE_SEEKING) {
            gainCheese(html, prefs)
        }
    }

    private fun loadStats(prefs: Preferences) {
        currentStats = Stats()
        val setting = prefs.getString(PREF_STATS)
        STAT_SETTING_PATTERN.findAll(setting).forEach { match ->
            val code = match.groupValues[1]
            val value = match.groupValues[2].toIntOrNull() ?: return@forEach
            val stat = Stat.entries.firstOrNull { it.code == code } ?: return@forEach
            currentStats = currentStats.withStat(stat, value)
        }
    }

    private fun saveStats(prefs: Preferences) {
        prefs.setString(PREF_STATS, currentStats.toSetting())
    }

    private fun saveStyles(prefs: Preferences) {
        val value = currentStyles.values.joinToString(",") { it.name }
        prefs.setString(PREF_CURRENT_STYLES, value)
    }

    private fun parseStyles(html: String, prefs: Preferences) {
        currentStats = Stats()
        currentStyles.clear()
        for (match in IMAGE_PATTERN.findAll(html)) {
            val image = match.groupValues[4]
            if (image.startsWith("needle")) {
                parseNeedle(match.groupValues[2], match.groupValues[3])
                continue
            }
            imageToStyle[image]?.let { style ->
                currentStyles[style.upgrade] = style
            }
        }
        saveStyles(prefs)
        saveStats(prefs)
    }

    private fun parseNeedles(html: String, prefs: Preferences) {
        currentStats = Stats()
        for (match in IMAGE_PATTERN.findAll(html)) {
            val image = match.groupValues[4]
            if (image.startsWith("needle")) {
                parseNeedle(match.groupValues[2], match.groupValues[3])
            }
        }
        saveStats(prefs)
    }

    private fun parseNeedle(topString: String, leftString: String) {
        val top = topString.toIntOrNull() ?: return
        val left = leftString.toIntOrNull() ?: return
        val stat = when (top) {
            233 -> if (left < 200) Stat.MA else Stat.MD
            252 -> if (left < 200) Stat.CA else Stat.CD
            270 -> if (left < 200) Stat.PA else Stat.PD
            else -> return
        }
        val value = left - PIXELS.get(stat)
        currentStats = currentStats.withStat(stat, value)
    }

    private fun parseCastle(html: String, prefs: Preferences) {
        val match = CASTLE_PATTERN.find(html) ?: return
        val castle = Castle.fromDescription(match.groupValues[3]) ?: return
        currentCastle = castle
        prefs.setString(PREF_ENEMY_NAME, match.groupValues[2])
        prefs.setString(PREF_ENEMY_CASTLE, castle.prefix)
    }

    private fun parseLoomingCastle(html: String) {
        val image = LOOMING_CASTLE_PATTERN.find(html)?.groupValues?.getOrNull(1) ?: return
        currentCastle = Castle.fromBattleImage(image)
    }

    private fun parseTurn(html: String, prefs: Preferences): Boolean {
        val turn = TURN_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return false
        prefs.setInt(PREF_GAME_TURN, turn)
        return true
    }

    private fun getChoices(html: String, prefs: Preferences) {
        val choices = ChoiceUtilities.parseChoices(html)
        if (choices.size == 3) {
            prefs.setString(PREF_CHOICE + "1", choices[1] ?: "")
            prefs.setString(PREF_CHOICE + "2", choices[2] ?: "")
            prefs.setString(PREF_CHOICE + "3", choices[3] ?: "")
        }
    }

    private fun clearChoices(prefs: Preferences) {
        prefs.setString(PREF_CHOICE + "1", "")
        prefs.setString(PREF_CHOICE + "2", "")
        prefs.setString(PREF_CHOICE + "3", "")
    }

    private fun startGame(prefs: Preferences) {
        prefs.setInt(PREF_CHEESE, 0)
        clearChoices(prefs)
    }

    private fun nextTurn(prefs: Preferences) {
        prefs.setInt(PREF_GAME_TURN, prefs.getInt(PREF_GAME_TURN) + 1)
    }

    private fun endGame(prefs: Preferences) {
        prefs.setInt(PREF_GAMES, prefs.getInt(PREF_GAMES) + 1)
        prefs.setInt(PREF_GAME_TURN, 0)
    }

    private fun startBattle(decision: Int, prefs: Preferences) {
        // Stance tracking deferred; battle results come from HTML parse.
        prefs.getInt(PREF_GAME_TURN)
        decision
    }

    private fun endBattle(html: String, prefs: Preferences) {
        val results = logBattle(html)
        val won = results.won()
        prefs.setBoolean(PREF_LAST_BATTLE_WON, won)
        prefs.setString(PREF_LAST_BATTLE_RESULTS, results.value)

        if (html.contains("GAME OVER")) {
            val match = TOTAL_CHEESE_PATTERN.find(html)
            if (match != null) {
                val calculated = prefs.getInt(PREF_CHEESE)
                val total = match.groupValues[2].replace(",", "").toIntOrNull() ?: calculated
                if (calculated != total) {
                    prefs.setInt(PREF_CHEESE, total)
                }
            }
        }
    }

    private fun gainCheese(html: String, prefs: Preferences): Int {
        val match = CHEESE_PATTERN.find(html)
        val cheese = match?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0
        if (cheese > 0) {
            prefs.setInt(PREF_CHEESE, prefs.getInt(PREF_CHEESE) + cheese)
        }
        prefs.setInt(PREF_LAST_CHEESE, cheese)
        return cheese
    }

    private fun collectCheese(html: String, prefs: Preferences) {
        val encounterName = prefs.getString(PREF_LAST_ENCOUNTER)
        val curds = prefs.getInt(PREF_LAST_CHEESE)
        if (curds == 0) {
            if (encounterName != "Use the wishing well" ||
                html.contains("You can't afford to make a wish, so you move on.")
            ) {
                return
            }
        }
        prefs.getInt(PREF_GAME_TURN)
    }

    private fun logBoosts(prefs: Preferences, activeEffectNames: Set<String>) {
        val boosts = buildString {
            if (activeEffectNames.any { it.equals(SHARK_TOOTH_GRIN, ignoreCase = true) }) append('M')
            if (activeEffectNames.any { it.equals(BOILING_DETERMINATION, ignoreCase = true) }) append('C')
            if (activeEffectNames.any { it.equals(ENHANCED_INTERROGATION, ignoreCase = true) }) append('P')
        }
        prefs.setString(PREF_BOOSTS, boosts)
    }

    private fun checkPredictions(prefs: Preferences) {
        val predicted = BastilleDatabase.predictedStats(currentStyles) ?: return
        for (stat in Stat.entries) {
            if (predicted.get(stat) != currentStats.get(stat)) {
                // Diagnostic mismatch — prefs still saved; full session logging deferred.
            }
        }
        saveStats(prefs)
    }

    private fun logBattle(html: String): BattleResults {
        var aggressor = false
        var military = false
        var castle = false
        var psychological = false

        for (match in BATTLE_PATTERN.findAll(html)) {
            aggressor = match.groupValues[3] == "attack strength"
            val won = match.groupValues[4] == "higher"
            when (match.groupValues[1]) {
                "Military" -> military = won
                "Castle" -> castle = won
                "Psychological" -> psychological = won
            }
        }

        return BattleResults(aggressor, military, castle, psychological)
    }

    private data class BattleResults(
        val aggressor: Boolean,
        val military: Boolean,
        val castle: Boolean,
        val psychological: Boolean,
    ) {
        val value: String
            get() {
                val buf = StringBuilder()
                appendPair(buf, 'M', aggressor, military)
                buf.append(',')
                appendPair(buf, 'C', aggressor, castle)
                buf.append(',')
                appendPair(buf, 'P', aggressor, psychological)
                return buf.toString()
            }

        fun won(): Boolean = (if (military) 1 else 0) + (if (castle) 1 else 0) + (if (psychological) 1 else 0) >= 2

        private fun appendPair(buf: StringBuilder, prefix: Char, aggressor: Boolean, won: Boolean) {
            buf.append(prefix)
            buf.append(if (aggressor) 'A' else 'D')
            buf.append(if (won) '>' else '<')
            buf.append(prefix)
            buf.append(if (aggressor) 'D' else 'A')
        }
    }

    private fun Stats.withStat(stat: Stat, value: Int): Stats = when (stat) {
        Stat.MA -> copy(ma = value)
        Stat.MD -> copy(md = value)
        Stat.CA -> copy(ca = value)
        Stat.CD -> copy(cd = value)
        Stat.PA -> copy(pa = value)
        Stat.PD -> copy(pd = value)
    }

    internal fun resetSessionForTest() {
        currentStyles.clear()
        currentStats = Stats()
        currentCastle = null
    }

    private const val PREF_STATS = "_bastilleStats"
    private const val PREF_CURRENT_STYLES = "_bastilleCurrentStyles"
    private const val PREF_GAMES = "_bastilleGames"
    private const val PREF_BOOSTS = "_bastilleBoosts"
    private const val PREF_GAME_TURN = "_bastilleGameTurn"
    private const val PREF_CHEESE = "_bastilleCheese"
    private const val PREF_ENEMY_CASTLE = "_bastilleEnemyCastle"
    private const val PREF_ENEMY_NAME = "_bastilleEnemyName"
    private const val PREF_CHOICE = "_bastilleChoice"
    private const val PREF_LAST_ENCOUNTER = "_bastilleLastEncounter"
    private const val PREF_LAST_BATTLE_RESULTS = "_bastilleLastBattleResults"
    private const val PREF_LAST_BATTLE_WON = "_bastilleLastBattleWon"
    private const val PREF_LAST_CHEESE = "_bastilleLastCheese"

    private val STAT_SETTING_PATTERN = Regex("""(MA|MD|CA|CD|PA|PD)=(\d+)""")

    private const val SHARK_TOOTH_GRIN = "Shark Tooth Grin"
    private const val BOILING_DETERMINATION = "Boiling Determination"
    private const val ENHANCED_INTERROGATION = "Enhanced Interrogation"
}
