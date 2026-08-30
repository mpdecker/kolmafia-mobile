package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop SpaaaceRequest headless hub — transponder prep, Porko solver, isotope shops
 * (Phases 3336–3350).
 */
object SpaaaceRequest {

    const val TRANSPONDER_ITEM_ID = 5170
    const val LUNAR_ISOTOPE_ITEM_ID = 5134
    const val TRANSPONDENT_EFFECT = "Transpondent"

    private val TOKEN_PATTERN = Regex("""You have ([\d,]+) lunar isotope""")
    private val PEG_PATTERN = Regex("""title="peg style ([123])"""")
    private val PAYOUT_PATTERN = Regex("""<div class="blank">x(\d)</div>""")

    private const val RIGHT = 1
    private const val LEFT = 2
    private const val RANDOM = 3

    private var pegs: Array<IntArray>? = null
    private var expected: FloatArray? = null

    fun immediatelyAccessible(activeEffectNames: Set<String>, inventoryCount: (Int) -> Int): Boolean =
        activeEffectNames.any { it.equals(TRANSPONDENT_EFFECT, ignoreCase = true) }

    fun accessible(
        activeEffectNames: Set<String>,
        inventoryCount: (Int) -> Int,
        quests: QuestDatabase?,
    ): String? {
        val generatorStarted = quests?.isQuestStarted(Quest.GENERATOR) == true
        val generatorFinished = quests?.isQuestFinished(Quest.GENERATOR) == true
        if (!generatorStarted && !generatorFinished) {
            return "You need to repair the Elves' Shield Generator to go there."
        }
        if (immediatelyAccessible(activeEffectNames, inventoryCount) ||
            inventoryCount(TRANSPONDER_ITEM_ID) > 0
        ) {
            return null
        }
        return "You need a transporter transponder to go there."
    }

    fun parseResponse(url: String, @Suppress("UNUSED_PARAMETER") responseText: String, quests: QuestDatabase?) {
        if (!url.startsWith("spaaace.php")) return
        quests?.setQuestIfBetter(Quest.GENERATOR, QuestDatabase.STARTED)
    }

    fun parseIsotopeBalance(responseText: String): Int? =
        TOKEN_PATTERN.find(responseText)?.groupValues?.get(1)
            ?.replace(",", "")?.toIntOrNull()

    fun parseGameBoard(responseText: String): String {
        val buffer = StringBuilder()
        PEG_PATTERN.findAll(responseText).forEach { buffer.append(it.groupValues[1]) }
        return buffer.toString()
    }

    fun parseGamePayouts(responseText: String): String {
        val buffer = StringBuilder()
        PAYOUT_PATTERN.findAll(responseText).forEach { buffer.append(it.groupValues[1]) }
        return buffer.toString()
    }

    fun validBoard(board: String, payouts: String): Boolean =
        board.length == 8 * (9 + 8) && payouts.length == 9

    fun initializeGameBoard(preferences: Preferences?) {
        preferences?.setString("lastPorkoBoard", "")
        preferences?.setString("lastPorkoPayouts", "")
        preferences?.setString("lastPorkoExpected", "")
        pegs = null
        expected = null
    }

    fun loadGameBoard(board: String, payouts: String, preferences: Preferences?) {
        preferences?.setString("lastPorkoBoard", board)
        preferences?.setString("lastPorkoPayouts", payouts)
        val matrix = Array(17) { IntArray(17) }
        var index = 0
        for (row in 0 until 16) {
            val off = row and 1
            for (col in off until 17 step 2) {
                matrix[row][col] = board[index++].digitToIntOrNull() ?: 0
            }
        }
        for (col in 0 until 17 step 2) {
            matrix[16][col] = payouts[col / 2].digitToIntOrNull() ?: 0
        }
        pegs = matrix
    }

    fun solveGameBoard(preferences: Preferences?) {
        val matrix = pegs ?: return
        val unreachable = calculateUnreachable(matrix)
        val min = IntArray(17)
        val max = IntArray(17)
        val exp = FloatArray(17)
        for (col in 0 until 17 step 2) {
            val val_ = if (unreachable[15][col]) -1 else matrix[16][col]
            min[col] = val_
            max[col] = val_
            exp[col] = val_.toFloat()
        }
        for (row in 14 downTo -1) {
            val off = row and 1
            for (col in off until 17 step 2) {
                if (row >= 0 && unreachable[row][col]) continue
                val peg = when {
                    col == 0 -> RIGHT
                    col == 16 -> LEFT
                    else -> matrix[row + 1][col]
                }
                val (minVal, maxVal, eVal) = when (peg) {
                    RIGHT -> Triple(min[col + 1], max[col + 1], exp[col + 1])
                    LEFT -> Triple(min[col - 1], max[col - 1], exp[col - 1])
                    RANDOM -> Triple(
                        minOf(min[col - 1], min[col + 1]),
                        maxOf(max[col - 1], max[col + 1]),
                        (exp[col - 1] + exp[col + 1]) / 2f,
                    )
                    else -> Triple(0, 0, 0f)
                }
                min[col] = minVal
                max[col] = maxVal
                exp[col] = eVal
            }
        }
        val slotExpected = FloatArray(9)
        val buffer = StringBuilder()
        for (col in 0 until 17 step 2) {
            if (col > 0) buffer.append(':')
            buffer.append(formatExpected(exp[col]))
            slotExpected[col / 2] = exp[col]
        }
        expected = slotExpected
        preferences?.setString("lastPorkoExpected", buffer.toString())
    }

    fun visitPorkoChoice(
        responseText: String,
        preferences: Preferences?,
        inventoryAdjust: ((Int, Int) -> Unit)? = null,
    ) {
        if (responseText.contains("You hand Juliedriel your isotope")) {
            inventoryAdjust?.invoke(LUNAR_ISOTOPE_ITEM_ID, -1)
        }
        initializeGameBoard(preferences)
        val board = parseGameBoard(responseText)
        val payouts = parseGamePayouts(responseText)
        if (!validBoard(board, payouts)) return
        loadGameBoard(board, payouts, preferences)
        solveGameBoard(preferences)
    }

    fun visitGeneratorChoice(responseText: String, preferences: Preferences?) {
        initializeGameBoard(preferences)
        val board = parseGameBoard(responseText)
        val payouts = "000010000"
        if (!validBoard(board, payouts)) return
        loadGameBoard(board, payouts, preferences)
        solveGameBoard(preferences)
    }

    fun bestExpectedPayout(): Float? {
        val values = expected ?: return null
        return values.maxOrNull()
    }

    fun expectedPayouts(): FloatArray? = expected?.copyOf()

    fun registerRequest(url: String, sessionLogger: SessionLogger?, adventureCount: Int = 0): Boolean {
        if (!url.startsWith("spaaace.php")) return false
        if (url.contains("place=shop")) return false
        val action = Regex("""[?&]action=([^&]+)""").find(url)?.groupValues?.get(1)
        val message = when {
            action == null && url.contains("place=porko") -> "Visiting The Porko Palace"
            action == "playporko" -> "[$adventureCount] Porko Game"
            else -> null
        } ?: return false
        sessionLogger?.appendRawLine("")
        sessionLogger?.appendRawLine(message)
        return true
    }

    private fun calculateUnreachable(matrix: Array<IntArray>): Array<BooleanArray> {
        val divs = Array(17) { BooleanArray(17) }
        var reach = BooleanArray(17) { true }
        for (row in 0 until 16) {
            val reach2 = BooleanArray(17)
            val off = row and 1
            for (col in off until 17 step 2) {
                if (!reach[col]) continue
                when (matrix[row][col]) {
                    RIGHT -> {
                        if (col < 16) reach2[col + 1] = true else reach2[col - 1] = true
                    }
                    LEFT -> {
                        if (col > 0) reach2[col - 1] = true else reach2[col + 1] = true
                    }
                    RANDOM -> {
                        if (col > 0) reach2[col - 1] = true
                        if (col < 16) reach2[col + 1] = true
                    }
                }
            }
            reach = reach2
            for (col in off until 17 step 2) {
                if (!reach[col]) divs[row][col] = true
            }
        }
        return divs
    }

    private fun formatExpected(value: Float): String {
        val rounded = (value * 100).toInt() / 100f
        return if (rounded == rounded.toLong().toFloat()) {
            rounded.toLong().toString()
        } else {
            rounded.toString()
        }
    }

    fun resetForTest() {
        pegs = null
        expected = null
    }
}
