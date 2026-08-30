package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [RumpleManager] — Rumpelstiltskin gnome psychosis engine
 * (materials, mastery, spy/sin/bribe, choices 844–850) — Phases 3216–3230.
 * Headless advisor strings only (no Relay decorate HTML).
 */
object RumpleManager {

    const val NEITHER = "neither parent"
    const val FATHER = "the father"
    const val MOTHER = "the mother"
    const val BOTH = "both parents"

    const val NONE = "good nature"
    const val GREED = "inherent greed"
    const val GLUTTONY = "gluttony"
    const val VANITY = "vanity"
    const val LAZINESS = "laziness"
    const val LUSTFULNESS = "lustfulness"
    const val VIOLENCE = "violent nature"

    enum class State { CLOSED, STARTED, ENDED }

    private var parent: String = NEITHER
    private var sin: String = NONE
    private var state: State = State.CLOSED
    private val sins: Array<Array<String>?> = arrayOfNulls(3)

    private val GUTS = Regex("""<span class='guts'>(.*?)</span>""", RegexOption.DOT_MATCHES_ALL)
    private val PATTERN1 = Regex("""without fear of being noticed\.\s*Y([^.]*)""", RegexOption.DOT_MATCHES_ALL)
    private val PATTERN2 = Regex("""and then y([^.]*)""", RegexOption.DOT_MATCHES_ALL)
    private val PATTERN3 = Regex("""when you look back y([^.]*)""", RegexOption.DOT_MATCHES_ALL)
    private val MATERIAL = Regex("""alt="(.*?)"></td><td valign=center>(\d+)<""")
    private val MASTERY = Regex("""(\d+) more tries""")

    private val MATERIAL_IDS = mapOf(
        "straw" to ItemPool.STRAW,
        "leather" to ItemPool.LEATHER,
        "clay" to ItemPool.CLAY,
        "filling" to ItemPool.FILLING,
        "parchment" to ItemPool.PARCHMENT,
        "glass" to ItemPool.GLASS,
    )

    private val TELLS = listOf(
        Triple("counting his possessions", GREED, NONE),
        Triple("counting her possessions", GREED, NONE),
        Triple("gold-plating a lily", GREED, NONE),
        Triple("studying a treasure map", GREED, NONE),
        Triple("writing a contract to make a high-interest loan", GREED, NONE),
        Triple("chowing down on a fistful of bacon", GLUTTONY, NONE),
        Triple("eating a chocolate rabbit", GLUTTONY, NONE),
        Triple("putting a fried egg on top of a cheeseburger", GLUTTONY, NONE),
        Triple("sprinkling extra cheese on a pizza already dripping with the stuff", GLUTTONY, NONE),
        Triple("checking his reflection in a spoon", VANITY, NONE),
        Triple("checking her reflection in a spoon", VANITY, NONE),
        Triple("plucking his eyebrows", VANITY, NONE),
        Triple("plucking her eyebrows", VANITY, NONE),
        Triple("putting in a teeth-whitening tray", VANITY, NONE),
        Triple("telling everyone that the song on the radio", VANITY, NONE),
        Triple("asking one of the kids to find the remote control", LAZINESS, NONE),
        Triple("collapsed deep in an easy chair, stifling a yawn", LAZINESS, NONE),
        Triple("lying on the floor, calling his spouse to come give a kiss", LAZINESS, LUSTFULNESS),
        Triple("lying on the floor, calling her spouse to come give a kiss", LAZINESS, LUSTFULNESS),
        Triple("lying on the floor", LAZINESS, NONE),
        Triple("sleeping soundly", LAZINESS, NONE),
        Triple("eyeing her spouse lasciviously", LUSTFULNESS, NONE),
        Triple("eyeing his spouse lasciviously", LUSTFULNESS, NONE),
        Triple("flipping through a lingerie catalog", LUSTFULNESS, NONE),
        Triple("moaning softly", LUSTFULNESS, NONE),
        Triple("peeking through the blinds at the attractive neighbors", LUSTFULNESS, NONE),
        Triple("kicking a dog", VIOLENCE, NONE),
        Triple("punching a hole in the wall of the house", VIOLENCE, NONE),
        Triple("screaming at the television", VIOLENCE, NONE),
        Triple("stubbing his toe on an ottoman", VIOLENCE, NONE),
        Triple("stubbing her toe on an ottoman", VIOLENCE, NONE),
        Triple("cutting a huge piece of cake to eat alone", GREED, GLUTTONY),
        Triple("opening a family-size bag of Cheat-Os", GREED, GLUTTONY),
        Triple("putting a golden ring on each finger", GREED, VANITY),
        Triple("shining a golden chalice to a reflective finish", GREED, VANITY),
        Triple("reclined in a chair, ordering stuff from the Home Shopping Network", GREED, LAZINESS),
        Triple("trying to shortchange the maid who is washing the dishes", GREED, LAZINESS),
        Triple("admiring a solid marble nude statue", GREED, LUSTFULNESS),
        Triple("admiring a valuable collection of artistic nudes", GREED, LUSTFULNESS),
        Triple("polishing a collection of solid silver daggers", GREED, VIOLENCE),
        Triple("loading a jewel-encrusted pistol with golden bullets", GREED, VIOLENCE),
        Triple("checking for stretch marks while downing a huge chocolate shake", GLUTTONY, VANITY),
        Triple("trying to squeeze into a girdle", GLUTTONY, VANITY),
        Triple("calling the dog over to lick french-fry grease", GLUTTONY, LAZINESS),
        Triple("reclined in an overstuffed chair eating a bag of bacon-flavored onion rings", GLUTTONY, LAZINESS),
        Triple("licking an all-day sucker", GLUTTONY, LUSTFULNESS),
        Triple("sensually over a rack of ribs", GLUTTONY, LUSTFULNESS),
        Triple("tearing apart an entire roasted chicken so hard the bones snap", GLUTTONY, VIOLENCE),
        Triple("throwing a bag of chips on the ground and stomping on it to open it", GLUTTONY, VIOLENCE),
        Triple("collapsed in an overstuffed chair, curling his eyelashes", VANITY, LAZINESS),
        Triple("collapsed in an overstuffed chair, curling her eyelashes", VANITY, LAZINESS),
        Triple("using the remote control to turn the TV to the Beauty Channel", VANITY, LAZINESS),
        Triple("checking out own his body and licking his lips seductively", VANITY, LUSTFULNESS),
        Triple("checking out own her body and licking her lips seductively", VANITY, LUSTFULNESS),
        Triple("practicing pick-up lines on his own reflection in a window", VANITY, LUSTFULNESS),
        Triple("practicing pick-up lines on her own reflection in a window", VANITY, LUSTFULNESS),
        Triple("angrily plucking stray eyebrow hairs", VANITY, VIOLENCE),
        Triple("kicking the dog, then making sure the kick didn't scuff", VANITY, VIOLENCE),
        Triple("reclined on the bed, idly peeping through the window to the neighbor's house", LAZINESS, LUSTFULNESS),
        Triple("half-heartedly kicking at the cat when it comes too close", LAZINESS, VIOLENCE),
        Triple("sleepily swiping at a whining kid", LAZINESS, VIOLENCE),
        Triple("aggressively kissing", LUSTFULNESS, VIOLENCE),
        Triple("tearing down the blinds to peep out of the window", LUSTFULNESS, VIOLENCE),
    )

    /** Sin → bribe material rows (headless advisor; not Relay HTML). */
    val BRIBES: List<List<String>> = listOf(
        listOf(GREED, "straw"),
        listOf(GREED, "straw", "parchment"),
        listOf(GREED, "clay", "filling"),
        listOf(GLUTTONY, "straw"),
        listOf(GLUTTONY, "leather", "parchment"),
        listOf(GLUTTONY, "clay", "filling"),
        listOf(VANITY, "leather"),
        listOf(VANITY, "clay", "parchment"),
        listOf(VANITY, "straw", "glass"),
        listOf(LAZINESS, "leather"),
        listOf(LAZINESS, "leather", "filling"),
        listOf(LAZINESS, "straw", "glass"),
        listOf(LUSTFULNESS, "clay"),
        listOf(LUSTFULNESS, "straw", "parchment"),
        listOf(LUSTFULNESS, "leather", "glass"),
        listOf(VIOLENCE, "clay"),
        listOf(VIOLENCE, "clay", "glass"),
        listOf(VIOLENCE, "leather", "filling"),
    )

    fun resetForTest() {
        parent = NEITHER
        sin = NONE
        state = State.CLOSED
        sins[0] = null
        sins[1] = null
        sins[2] = null
    }

    fun currentState(): State = state
    fun currentParent(): String = parent
    fun currentSin(): String = sin
    fun detectedSins(): List<Array<String>?> = sins.toList()

    fun visitChoice(choiceId: Int, html: String, preferences: Preferences?) {
        preferences?.setString("grimstoneMaskPath", "gnome")
        when (choiceId) {
            848, 850 -> updateMaterials(html, null)
            849 -> {
                updateMaterials(html, null)
                updateMastery(html, 0, preferences)
            }
        }
    }

    fun postChoice(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager? = null,
        sessionLogger: SessionLogger? = null,
    ) {
        when (choiceId) {
            844 -> if (decision == 1) spyOnParents(html, sessionLogger)
            846 -> pickParent(decision, preferences)
            847 -> pickSin(decision)
            848 -> if (decision != 4) recordTrade(html, preferences, sessionLogger)
            849 -> {
                when (decision) {
                    1 -> ResultProcessor.processItem(ItemPool.STRAW, -3, inventory = inventory)
                    2 -> ResultProcessor.processItem(ItemPool.LEATHER, -3, inventory = inventory)
                    3 -> ResultProcessor.processItem(ItemPool.CLAY, -3, inventory = inventory)
                }
                updateMastery(html, decision, preferences)
            }
        }
    }

    fun reset(choice: Int, inventory: InventoryManager?, preferences: Preferences?) {
        if (choice == 4) state = State.STARTED
        if (state == State.CLOSED) return

        for (id in MATERIAL_IDS.values) {
            val qty = inventory?.state?.value?.items?.get(id)?.quantity ?: 0
            if (qty > 0) ResultProcessor.processItem(id, -qty, inventory = inventory)
        }
        preferences?.setInt("rumpelstiltskinTurnsUsed", 0)
        preferences?.setInt("rumpelstiltskinKidsRescued", 0)
        resetSins()
        if (choice != 4) state = State.CLOSED
    }

    fun resetSins() {
        parent = NEITHER
        sins[0] = null
        sins[1] = null
        sins[2] = null
    }

    fun spyOnParents(responseText: String, sessionLogger: SessionLogger? = null) {
        val guts = GUTS.find(responseText)?.groupValues?.getOrNull(1) ?: return
        sins[0] = detectSins(PATTERN1, guts, sessionLogger)
        sins[1] = detectSins(PATTERN2, guts, sessionLogger)
        sins[2] = detectSins(PATTERN3, guts, sessionLogger)
    }

    fun pickParent(choice: Int, preferences: Preferences?) {
        when (choice) {
            1 -> parent = FATHER
            2 -> parent = MOTHER
            3 -> parent = BOTH
            4 -> {
                if ((preferences?.getInt("rumpelstiltskinTurnsUsed", 0) ?: 0) == 30) {
                    state = State.ENDED
                }
                resetSins()
            }
        }
    }

    fun pickSin(choice: Int) {
        sin = when (choice) {
            1 -> GREED
            2 -> GLUTTONY
            3 -> VANITY
            4 -> LAZINESS
            5 -> LUSTFULNESS
            6 -> VIOLENCE
            else -> NONE
        }
    }

    fun recordTrade(text: String, preferences: Preferences?, sessionLogger: SessionLogger?) {
        val kids = when {
            text.contains("one of h") || text.contains("one child") -> 1
            text.contains("three of their") || text.contains("three kids") ||
                text.contains("three whole children") -> 3
            text.contains("semi-precious children") || text.contains("five kids") -> 5
            text.contains("seven children") || text.contains("seven kids") ||
                text.contains("seven of their not-so-precious-after-all children") -> 7
            else -> 0
        }
        preferences?.let { it.setInt("rumpelstiltskinKidsRescued", it.getInt("rumpelstiltskinKidsRescued", 0) + kids) }
        val message =
            "Appealing to the $sin of $parent allowed you to rescue $kids " +
                if (kids == 1) "child." else "children."
        RequestLogger.updateSessionLog(message, sessionLogger)
    }

    fun updateMaterials(text: String, inventory: InventoryManager?) {
        MATERIAL.findAll(text).forEach { match ->
            val material = match.groupValues[1].lowercase()
            val number = match.groupValues[2].toIntOrNull() ?: return@forEach
            val itemId = MATERIAL_IDS[material] ?: return@forEach
            val have = inventory?.state?.value?.items?.get(itemId)?.quantity ?: 0
            if (have != number) {
                ResultProcessor.processItem(itemId, number - have, inventory = inventory)
            }
        }
    }

    fun updateMastery(text: String, decision: Int, preferences: Preferences?) {
        preferences ?: return
        if (decision == 0) {
            if (text.contains("master straw craftsman")) preferences.setInt("craftingStraw", 0)
            if (text.contains("master leather craftsman")) preferences.setInt("craftingLeather", 0)
            if (text.contains("master clay craftsman")) preferences.setInt("craftingClay", 0)
            return
        }
        val property = when (decision) {
            1 -> "craftingStraw"
            2 -> "craftingLeather"
            3 -> "craftingClay"
            else -> return
        }
        when {
            text.contains("You've figured it out") -> preferences.setInt(property, 0)
            text.contains("one more try will get you there") -> preferences.setInt(property, 1)
            else -> MASTERY.find(text)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let {
                preferences.setInt(property, it)
            }
        }
    }

    /** Headless advisor lines (desktop decorateWorkshop text without HTML). */
    fun advisorLines(preferences: Preferences?): List<String> {
        if (state == State.ENDED) return listOf("Gnome parents no longer an issue; craft for tinkering.")
        val turns = preferences?.getInt("rumpelstiltskinTurnsUsed", 0) ?: 0
        val lines = mutableListOf<String>()
        if (turns > 0 && turns % 6 == 0) {
            if (sins[0] == null) {
                lines += "Go spy on the parents to get clues about their sins"
            } else {
                for (record in sins) {
                    record ?: continue
                    val parentName = record[0]
                    val sin1 = record[1]
                    val sin2 = record[2]
                    val desc = buildString {
                        append(parentName)
                        append(": ")
                        append(if (sin1 == NONE) "UNKNOWN" else sin1)
                        if (sin2 != NONE) {
                            append(" or ")
                            append(sin2)
                        }
                    }
                    lines += desc
                }
            }
        } else {
            lines += "The portal is not open yet."
        }
        lines += "Bribe table:"
        var lastSin = NONE
        for (row in BRIBES) {
            val rowSin = row[0]
            if (rowSin != lastSin) {
                lines += "  $rowSin:"
                lastSin = rowSin
            }
            val materials = row.drop(1).joinToString(" + ")
            lines += "    $materials"
        }
        return lines
    }

    fun statusLines(preferences: Preferences?): List<String> = listOf(
        "Rumple state: $state",
        "Parent: $parent",
        "Sin: $sin",
        "Kids rescued: ${preferences?.getInt("rumpelstiltskinKidsRescued", 0) ?: 0}",
        "Turns used: ${preferences?.getInt("rumpelstiltskinTurnsUsed", 0) ?: 0}",
        "Crafting straw: ${preferences?.getInt("craftingStraw", -1) ?: -1}",
        "Crafting leather: ${preferences?.getInt("craftingLeather", -1) ?: -1}",
        "Crafting clay: ${preferences?.getInt("craftingClay", -1) ?: -1}",
    ) + advisorLines(preferences)

    private fun detectSins(
        pattern: Regex,
        text: String,
        sessionLogger: SessionLogger?,
    ): Array<String>? {
        val match = pattern.find(text) ?: return null
        val sinText = "Y" + match.groupValues[1]
        val record = parseSins(sinText)
        val sin1 = record[1]
        val sin2 = record[2]
        val message = buildString {
            append(sinText)
            append(" (")
            append(if (sin1 == NONE) "UNKNOWN" else sin1)
            if (sin2 != NONE) {
                append(" or ")
                append(sin2)
            }
            append(")")
        }
        RequestLogger.updateSessionLog(message, sessionLogger)
        return record
    }

    private fun parseSins(text: String): Array<String> {
        val parentName = when {
            text.contains("father") -> FATHER
            text.contains("mother") -> MOTHER
            else -> NEITHER
        }
        var sin1 = NONE
        var sin2 = NONE
        for ((tell, s1, s2) in TELLS) {
            if (text.contains(tell)) {
                sin1 = s1
                sin2 = s2
                break
            }
        }
        return arrayOf(parentName, sin1, sin2)
    }
}
