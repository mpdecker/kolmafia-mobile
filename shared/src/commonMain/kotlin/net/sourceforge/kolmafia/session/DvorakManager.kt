package net.sourceforge.kolmafia.session

/**
 * Headless solver state for the Hidden Temple Dvorak tile puzzle.
 *
 * Relay controls and HTML decoration are deliberately omitted.  The tile
 * board and the next HTTP action are useful to both CLI and automation.
 */
object DvorakManager {
    private const val ROWS = 7
    private const val COLUMNS = 9
    private const val SOLUTION = "BANANAS"
    private val tilePattern = Regex(
        """<td\s+class=['"]([^'"]*cell[^'"]*)['"][^>]*>.*?Tile labeled\s+['"]([^'"]+)['"].*?</td>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val tileUrlPattern = Regex("""whichtile=(\d+)""", RegexOption.IGNORE_CASE)

    private var board: List<List<Char>> = emptyList()
    var currentRow: Int = 0
        private set
    var currentSolution: String = ""
        private set

    fun reset() {
        board = emptyList()
        currentRow = 0
        currentSolution = ""
    }

    fun parseResponse(url: String?, html: String): Boolean {
        if (url == null || !url.substringAfterLast('/').startsWith("tiles.php", true)) return false
        if (html.contains("Squish!", true)) {
            currentRow = -1
            return true
        }
        val cells = tilePattern.findAll(html).toList()
        if (cells.size != ROWS * COLUMNS) return false
        val rows = cells.mapIndexed { index, match ->
            val classes = match.groupValues[1].lowercase().split(Regex("\\s+"))
            if ("cell" in classes && "greyed" !in classes) {
                currentRow = index / COLUMNS
            }
            match.groupValues[2].first()
        }.chunked(COLUMNS)
        board = rows
        currentSolution = SOLUTION.substring(0, (6 - currentRow).coerceIn(0, 6))
        return true
    }

    fun registerRequest(url: String, sessionLogger: SessionLogger?): Boolean {
        if (!url.substringAfterLast('/').startsWith("tiles.php", true)) return false
        val raw = tileUrlPattern.find(url)?.groupValues?.get(1)?.toIntOrNull()
            ?: return true
        if (currentRow !in 0 until ROWS || raw !in 0 until COLUMNS || board.isEmpty()) return false
        val letter = board[currentRow][raw]
        currentSolution += letter
        sessionLogger?.appendRawLine("Give me ${if (letter in "AEFILMNORSX") "an" else "a"} $letter!")
        return true
    }

    fun nextStepUrl(): String? {
        if (currentRow !in 0 until ROWS || board.size != ROWS) return null
        val wanted = SOLUTION.getOrNull(6 - currentRow) ?: return null
        val column = board[currentRow].indexOf(wanted)
        return column.takeIf { it >= 0 }?.let { "tiles.php?action=jump&whichtile=$it" }
    }

    fun status(): String {
        if (board.isEmpty()) return "Dvorak tile board is unknown. Visit tiles.php first."
        if (currentRow < 0) return "Dvorak puzzle state is invalid after a wrong tile."
        return "Dvorak row ${currentRow + 1}/$ROWS; solution so far \"$currentSolution\"; next=${nextStepUrl() ?: "unknown"}"
    }
}
