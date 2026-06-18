package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Witchess puzzle solutions from [witchess_solutions.txt]. Mirrors desktop
 * [WitchessSolutionDatabase].
 */
@OptIn(ExperimentalResourceApi::class)
object WitchessSolutionDatabase {

    private val solutions = mutableMapOf<Int, WitchessSolution>()
    private var loaded = false

    private val moveDict = mapOf(
        'r' to intArrayOf(0, 2),
        'l' to intArrayOf(0, -2),
        'u' to intArrayOf(-2, 0),
        'd' to intArrayOf(2, 0),
    )

    val isLoaded: Boolean get() = loaded
    val loadedEntryCount: Int get() = solutions.size

    data class WitchessSolution(
        val puzzleId: Int,
        val moves: List<Char>,
        val coords: String,
    )

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/witchess_solutions.txt").decodeToString()
        applyParse(parse(text))
        loaded = true
    }

    fun getWitchessSolution(puzzleId: Int): WitchessSolution? = solutions[puzzleId]

    internal fun parseForTest(text: String): Map<Int, WitchessSolution> = parse(text)

    internal fun injectForTest(snapshot: Map<Int, WitchessSolution>) {
        solutions.clear()
        solutions.putAll(snapshot)
        loaded = true
    }

    internal fun resetForTest() {
        solutions.clear()
        loaded = false
    }

    private fun applyParse(snapshot: Map<Int, WitchessSolution>) {
        solutions.clear()
        solutions.putAll(snapshot)
    }

    private fun parse(text: String): Map<Int, WitchessSolution> {
        val parsed = mutableMapOf<Int, WitchessSolution>()
        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val tab = line.indexOf('\t')
            if (tab < 0) continue
            val puzzleId = line.substring(0, tab).trim().toIntOrNull() ?: continue
            val moveText = line.substring(tab + 1).trim()
            if (moveText.isEmpty()) continue
            val moves = moveText.split(Regex("\\s+")).mapNotNull { token ->
                token.firstOrNull()?.takeIf { it in moveDict }
            }
            if (moves.isEmpty()) continue
            parsed[puzzleId] = WitchessSolution(puzzleId, moves, solvePath(moves))
        }
        return parsed
    }

    internal fun solvePath(moves: List<Char>): String {
        val moveCounts = mutableMapOf<Char, Int>()
        for (move in moves) {
            moveCounts[move] = moveCounts.getOrDefault(move, 0) + 1
        }

        val netVerticalDisplacement =
            kotlin.math.abs(moveCounts.getOrDefault('u', 0) - moveCounts.getOrDefault('d', 0))
        val netHorizontalDisplacement =
            kotlin.math.abs(moveCounts.getOrDefault('l', 0) - moveCounts.getOrDefault('r', 0))

        val maxX = netVerticalDisplacement * 2
        val maxY = netHorizontalDisplacement * 2

        var x = maxX
        var y = 0
        val path = linkedSetOf<String>()

        for (move in moves) {
            val start = intArrayOf(x, y)
            val delta = moveDict[move] ?: continue
            x = maxOf(0, minOf(x + delta[0], maxX))
            y = maxOf(0, minOf(y + delta[1], maxY))
            val end = intArrayOf(x, y)
            val midpoint = intArrayOf((start[0] + end[0]) / 2, (start[1] + end[1]) / 2)
            path.add("${midpoint[0]},${midpoint[1]}")
        }

        return path.sortedWith(
            compareBy<String> { it.substringBefore(',').toInt() }
                .thenBy { it.substringAfter(',').toInt() },
        ).joinToString("|")
    }
}
