package net.sourceforge.kolmafia.adventure.choice.solvers

/**
 * Desktop [ArcadeRequest.autoChoiceFightersOfFighting] (Phases 1701–1715).
 * Sets [lastAttackCode] when a mid-fight move is chosen (form field `attack`).
 */
object FightersOfFighting {

    var lastAttackCode: String? = null
        private set

    fun clearAttack() {
        lastAttackCode = null
    }

    private val MATCH = Regex("""&quot;(.*?) Vs\. (.*?) FIGHT!&quot;""", RegexOption.DOT_MATCHES_ALL)
    private val ROUND = Regex(
        """Results:.*?<td>(.*?)</td>.*?Score: [0123456789,]*</td>.*?title="\d+ HP".*?title="\d+ HP".*?<b>(.*?)</b>.*?>VS<.*?<b>(.*?)</b>""",
        RegexOption.DOT_MATCHES_ALL,
    )

    private val OPPONENTS = arrayOf(
        "Kitty the Zmobie Basher",
        "Morbidda",
        "Roo",
        "Serenity",
        "Thorny Toad",
        "Vaso De Agua",
    )

    private val THREATS = arrayOf(
        arrayOf(
            "launches a kick straight at your forehead",
            "get some paininess in your sexy parts",
            "ready to sweep your leg",
            "about to punch you in the throat",
            "like a punch to the gut",
            "aims a punch at your kneecap",
        ),
        arrayOf(
            "launching itself at your head",
            "aims a knee square at your groin",
            "trying to trip you up",
            "aims it at your throat",
            "aims a punch at your solar plexus",
            "fist to kneecap",
        ),
        arrayOf(
            "aims one big, flat foot at your head",
            "aims a foot right at your crotch",
            "aims his tail at your ankles",
            "punch you in the throat",
            "prepares to suckerpunch you in the gut",
            "aims a punch square at your kneecap",
        ),
        arrayOf(
            "a hard boot to the head",
            "a nice, solid kick to the gonads",
            "knock your ankles out from under you",
            "launches a fist at your throat",
            "punched in the small intestine",
            "about to punch you in the knee",
        ),
        arrayOf(
            "a vicious kick to the head",
            "he's going for the groin",
            "crouches to try and sweep your legs",
            "launches a fist at your throat",
            "aims it square at your gut",
            "aims a punch at your knee",
        ),
        arrayOf(
            "you see his foot flying at your head",
            "a well-placed foot to the groin",
            "my feet had been knocked out from under me",
            "aims it straight at your throat",
            "aims a helpful fist at your gut",
            "about to punch you in the knee",
        ),
    )

    private const val FAIL = 0
    private const val POOR = 1
    private const val FAIR = 2
    private const val GOOD = 3

    private val MCODE = arrayOf("hk", "gk", "lk", "tp", "gp", "kp")

    // EFFECTIVENESS[opponent][threat][move] — desktop ArcadeRequest matrix
    private val EFFECTIVENESS = arrayOf(
        // Kitty
        arrayOf(
            intArrayOf(GOOD, FAIR, FAIL, FAIL, FAIL, POOR),
            intArrayOf(FAIR, FAIL, GOOD, POOR, FAIL, FAIL),
            intArrayOf(FAIL, GOOD, FAIR, FAIL, POOR, FAIL),
            intArrayOf(FAIL, FAIL, POOR, FAIR, FAIL, GOOD),
            intArrayOf(POOR, FAIL, FAIL, FAIL, GOOD, FAIR),
            intArrayOf(FAIL, POOR, FAIL, GOOD, FAIR, FAIL),
        ),
        // Morbidda
        arrayOf(
            intArrayOf(FAIL, GOOD, FAIR, POOR, FAIL, FAIL),
            intArrayOf(GOOD, FAIR, FAIL, FAIL, POOR, FAIL),
            intArrayOf(FAIR, FAIL, GOOD, FAIL, FAIL, POOR),
            intArrayOf(POOR, FAIL, FAIL, GOOD, FAIR, FAIL),
            intArrayOf(FAIL, POOR, FAIL, FAIR, FAIL, GOOD),
            intArrayOf(FAIL, FAIL, POOR, FAIL, GOOD, FAIR),
        ),
        // Roo
        arrayOf(
            intArrayOf(FAIL, POOR, FAIL, FAIL, FAIR, GOOD),
            intArrayOf(POOR, FAIL, FAIL, GOOD, FAIL, FAIR),
            intArrayOf(FAIL, FAIL, POOR, FAIR, GOOD, FAIL),
            intArrayOf(FAIR, GOOD, FAIL, FAIL, POOR, FAIL),
            intArrayOf(FAIL, FAIR, GOOD, POOR, FAIL, FAIL),
            intArrayOf(GOOD, FAIL, FAIR, FAIL, FAIL, POOR),
        ),
        // Serenity
        arrayOf(
            intArrayOf(FAIL, FAIL, GOOD, FAIL, FAIL, FAIL),
            intArrayOf(FAIL, GOOD, FAIR, FAIL, FAIL, FAIL),
            intArrayOf(GOOD, FAIL, FAIL, FAIL, FAIL, FAIL),
            intArrayOf(FAIL, POOR, FAIL, FAIL, GOOD, FAIL),
            intArrayOf(FAIL, FAIL, FAIL, GOOD, FAIL, FAIL),
            intArrayOf(POOR, FAIL, FAIL, FAIR, FAIL, GOOD),
        ),
        // Thorny Toad
        arrayOf(
            intArrayOf(POOR, FAIL, FAIL, GOOD, FAIL, FAIR),
            intArrayOf(FAIL, FAIL, POOR, FAIR, GOOD, FAIL),
            intArrayOf(FAIL, POOR, FAIL, FAIL, FAIR, GOOD),
            intArrayOf(GOOD, FAIL, FAIR, POOR, FAIL, FAIL),
            intArrayOf(FAIR, GOOD, FAIL, FAIL, FAIL, POOR),
            intArrayOf(FAIL, FAIR, GOOD, FAIL, POOR, FAIL),
        ),
        // Vaso
        arrayOf(
            intArrayOf(FAIL, FAIL, POOR, FAIR, GOOD, FAIL),
            intArrayOf(FAIL, POOR, FAIL, FAIL, FAIR, GOOD),
            intArrayOf(FAIR, FAIL, FAIL, GOOD, FAIL, POOR),
            intArrayOf(FAIL, FAIR, GOOD, FAIL, FAIL, POOR),
            intArrayOf(GOOD, FAIL, FAIR, FAIL, POOR, FAIL),
            intArrayOf(FAIR, GOOD, FAIL, POOR, FAIL, FAIL),
        ),
    )

    /** @return choice option (6 = start match, 1 = attack) or null for manual */
    fun autoChoice(responseText: String): Int? {
        lastAttackCode = null
        if (MATCH.containsMatchIn(responseText)) {
            return 6
        }
        val m = ROUND.find(responseText) ?: return null
        val challenge = m.groupValues[1]
        val oname = m.groupValues[3]
        val opponent = OPPONENTS.indexOfFirst { it == oname }
        if (opponent < 0) return null
        val threat = THREATS[opponent].indexOfFirst { challenge.contains(it) }
        if (threat < 0) return null
        val effects = EFFECTIVENESS[opponent][threat]
        for (i in effects.indices) {
            if (effects[i] == GOOD) {
                lastAttackCode = MCODE[i]
                return 1
            }
        }
        return null
    }
}
