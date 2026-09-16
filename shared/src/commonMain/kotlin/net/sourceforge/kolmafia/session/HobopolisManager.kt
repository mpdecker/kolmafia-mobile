package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool

/**
 * Desktop [HobopolisDecorator] + [ChoiceControl] Hobopolis residual.
 *
 * Not a zone state machine — desktop has none. Headless parse/wait/sewer
 * accounting only; Relay image overlay is a non-goal.
 */
object HobopolisManager {

    const val SEWER_LOCATION = "A Maze of Sewer Tunnels"
    const val REQUIRE_SEWER_TEST_ITEMS = "requireSewerTestItems"

    data class SewerExplorationResult(
        val explorations: Int,
        val message: String,
        val error: Boolean = false,
    )

    private val BOSS_DROPS: List<Pair<String, String>> = listOf(
        "wrinkly heap on the ground" to "hobo skin",
        "smoking pair of boots" to "charred hobo boots",
        "pair of frozen eyeballs" to "frozen hobo eyeballs",
        "pile of foul-smelling guts" to "stinking hobo guts",
        "he left his skull behind" to "creepy hobo skull",
        "he ran off without his crotch" to "hobo crotch",
    )

    private var pendingStop: String? = null

    /**
     * Parses Hodgman Town Square boss-kill from fight HTML.
     *
     * @return the item name if a boss drop was detected, `null` otherwise.
     */
    fun parseTownSquareWin(html: String): String? {
        if (!html.contains("WINWINWIN")) return null
        for ((snippet, item) in BOSS_DROPS) {
            if (html.contains(snippet)) return item
        }
        return null
    }

    /** Desktop [HobopolisDecorator.handleTownSquare] session-log line. */
    fun richardTakesMessage(itemName: String): String = "Richard takes a $itemName"

    /** Desktop [ChoiceControl.hobopolisBossName]. */
    fun hobopolisBossName(choice: Int): String = when (choice) {
        200 -> "Hodgman"
        201 -> "Ol' Scratch"
        202 -> "Frosty"
        203 -> "Oscus"
        204 -> "Zombo"
        205 -> "Chester"
        518 -> "Uncle Hobo"
        else -> "nobody"
    }

    /**
     * Desktop ChoiceControl cases 200–205 / 518: stop when skipping into a waiting boss
     * during auto-adventure (decision 2).
     */
    fun bossWaitMessage(choice: Int, decision: Int): String? {
        if (decision != 2) return null
        val name = hobopolisBossName(choice)
        if (name == "nobody") return null
        return "$name waits for you."
    }

    fun recordPendingStop(message: String) {
        pendingStop = message
    }

    fun consumePendingStop(): String? {
        val msg = pendingStop
        pendingStop = null
        return msg
    }

    fun peekPendingStop(): String? = pendingStop

    /**
     * Desktop [HobopolisDecorator.decorate] Heap tire→kills calculator, without Relay HTML.
     * `kills = 0.1 * tires² + 0.7 * tires + 0.5`, truncated toward zero.
     */
    fun tireKills(tires: Int): Int {
        if (tires < 1) return 0
        val i = tires.toFloat()
        return (0.1f * i * i + 0.7f * i + 0.5f).toInt()
    }

    /**
     * Desktop [ChoiceControl.checkDungeonSewers] — choices 197–199, decision 1.
     */
    fun checkDungeonSewers(
        html: String,
        accessibleCount: (Int) -> Int,
        consumeItem: (Int, Int) -> Unit,
        hasEquipped: (Int) -> Boolean = { false },
        unequipItem: (Int) -> Unit = {},
        requireSewerTestItems: Boolean = false,
        sessionLog: (String) -> Unit = {},
    ): SewerExplorationResult? {
        if (!html.contains("You steel your nerves and descend into the darkened tunnel.")) {
            return null
        }

        var explorations = 0
        var dumplings = accessibleCount(ItemPool.DUMPLINGS)
        var wads = accessibleCount(ItemPool.SEWER_WAD)
        var oozeo = accessibleCount(ItemPool.OOZE_O)
        var oil = accessibleCount(ItemPool.OIL_OF_OILINESS)
        var umbrella = accessibleCount(ItemPool.GATORSKIN_UMBRELLA)

        if (html.contains("'crewcut'")) explorations += 1
        if (html.contains("in a big circle")) explorations += 3
        if (html.contains("Amalgamated Ladderage")) explorations += 5

        if (html.contains("some of your unfortunate dumplings")) {
            consumeItem(ItemPool.DUMPLINGS, 1)
            explorations += 1
            dumplings = accessibleCount(ItemPool.DUMPLINGS)
            if (dumplings <= 0) sessionLog("That was your last unfortunate dumplings.")
        }
        if (html.contains("the sight of your sewer wad")) {
            consumeItem(ItemPool.SEWER_WAD, 1)
            explorations += 1
            wads = accessibleCount(ItemPool.SEWER_WAD)
            if (wads <= 0) sessionLog("That was your last sewer wad.")
        }
        if (html.contains("He finds a bottle of Ooze-O")) {
            consumeItem(ItemPool.OOZE_O, 1)
            explorations += 1
            oozeo = accessibleCount(ItemPool.OOZE_O)
            if (oozeo <= 0) sessionLog("That was your last bottle of Ooze-O.")
        }
        if (html.contains("it takes three whole bottles")) {
            consumeItem(ItemPool.OIL_OF_OILINESS, 3)
            explorations += 1
            oil = accessibleCount(ItemPool.OIL_OF_OILINESS)
            if (oil < 3) sessionLog("You have less than 3 bottles of oil of oiliness left.")
        }
        if (html.contains("your gatorskin umbrella allows you to pass")) {
            if (hasEquipped(ItemPool.GATORSKIN_UMBRELLA)) unequipItem(ItemPool.GATORSKIN_UMBRELLA)
            consumeItem(ItemPool.GATORSKIN_UMBRELLA, 1)
            explorations += 1
            umbrella = accessibleCount(ItemPool.GATORSKIN_UMBRELLA)
            if (umbrella <= 0) sessionLog("That was your last gatorskin umbrella.")
        }
        if (html.contains("somebody else opened this grate")) explorations += 5

        var message = "+$explorations Explorations"
        var error = false
        if (requireSewerTestItems) {
            val missing = buildList {
                if (dumplings < 1) add("unfortunate dumplings")
                if (wads < 1) add("sewer wad")
                if (oozeo < 1) add("bottle of Ooze-O")
                if (oil < 1) add("oil of oiliness")
                if (umbrella < 1) add("gatorskin umbrella")
            }
            if (missing.isNotEmpty()) {
                error = true
                message += ", NEED: ${missing.joinToString(", ")}"
            }
        }
        sessionLog(message)
        return SewerExplorationResult(explorations, message, error)
    }

    /** Desktop [KoLAdventure.prepareForAdventure] Hobopolis sewer item gate. */
    fun sewerPrepError(
        requireSewerTestItems: Boolean,
        hasEquippedUmbrella: Boolean,
        hasEquippedBinder: Boolean,
        hasSewerWad: Boolean,
        hasOozeO: Boolean,
        hasDumplings: Boolean,
        hasOilOfOiliness: Boolean,
    ): String? {
        if (!requireSewerTestItems) return null
        if (hasEquippedUmbrella && hasEquippedBinder && hasSewerWad && hasOozeO &&
            hasDumplings && hasOilOfOiliness
        ) {
            return null
        }
        val parts = buildList {
            add("requireSewerTestItems is true so:")
            if (!hasEquippedUmbrella) add("Equip a gatorskin umbrella.")
            if (!hasEquippedBinder) add("Equip a hobo code binder.")
            if (!hasSewerWad) add("Acquire 1 sewer wad.")
            if (!hasOozeO) add("Acquire 1 bottle of Ooze-O.")
            if (!hasDumplings) add("Acquire 1 unfortunate dumpling.")
            if (!hasOilOfOiliness) add("Acquire 3 oil of oiliness.")
        }
        return parts.joinToString(" ")
    }

    fun postChoice(
        choiceId: Int,
        decision: Int,
        html: String,
        accessibleCount: (Int) -> Int,
        consumeItem: (Int, Int) -> Unit,
        hasEquipped: (Int) -> Boolean = { false },
        unequipItem: (Int) -> Unit = {},
        requireSewerTestItems: Boolean = false,
        sessionLog: (String) -> Unit = {},
    ): Boolean {
        when (choiceId) {
            197, 198, 199 -> {
                if (decision != 1) return false
                return checkDungeonSewers(
                    html = html,
                    accessibleCount = accessibleCount,
                    consumeItem = consumeItem,
                    hasEquipped = hasEquipped,
                    unequipItem = unequipItem,
                    requireSewerTestItems = requireSewerTestItems,
                    sessionLog = sessionLog,
                ) != null
            }
            200, 201, 202, 203, 204, 205, 518 -> {
                val wait = bossWaitMessage(choiceId, decision) ?: return false
                recordPendingStop(wait)
                sessionLog(wait)
                return true
            }
        }
        return false
    }
}
