package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * The Mad Tea Party chess puzzle engine.  Relay decoration is deliberately desktop-only, but the
 * board parser, notation, solver, choice synchronization, and CLI-facing operations are shared.
 */
object RabbitHoleManager {
    const val TEA_PARTY_CHOICE = 441
    const val RABBIT_HOLE_CHOICE = 442
    const val CHESS_CHOICE = 443
    const val REFLECTION_OF_MAP_ID = 4509

    enum class Side { UNKNOWN, WHITE, BLACK }
    enum class Piece(val code: Char) {
        EMPTY(' '), PAWN('P'), ROOK('R'), KNIGHT('N'), BISHOP('B'), KING('K'), QUEEN('Q');

        companion object {
            fun fromCode(code: Char): Piece = entries.firstOrNull {
                it.code.equals(code, ignoreCase = true)
            } ?: EMPTY
        }
    }

    data class Square(
        val color: Side,
        val piece: Piece = Piece.EMPTY,
        val side: Side = Side.UNKNOWN,
    ) {
        val isPiece get() = piece != Piece.EMPTY
        fun convert() = copy(side = if (side == Side.WHITE) Side.BLACK else Side.WHITE)
        fun notation(index: Int): String =
            "${if (side == Side.WHITE) "W" else if (side == Side.BLACK) "B" else ""}" +
                "${if (piece == Piece.EMPTY) "" else piece.code}${coords(index)}"
    }

    class Board {
        private val squares = Array(64) { Square(color(it)) }
        var current: Int = -1
            private set
        var pieces: Int = 0
            private set

        constructor()

        constructor(config: String) : this() {
            CONFIG.findAll(config).forEach { match ->
                val side = if (match.groupValues[1].equals("w", true)) Side.WHITE else Side.BLACK
                val piece = Piece.fromCode(match.groupValues[2][0])
                val col = match.groupValues[3].lowercase()[0] - 'a'
                val row = match.groupValues[4][0] - '1'
                add(square(row, col), Square(color(square(row, col)), piece, side))
            }
        }

        private constructor(source: Board) : this() {
            source.squares.copyInto(squares)
            current = source.current
            pieces = source.pieces
        }

        fun copyBoard() = Board(this)
        operator fun get(index: Int) = squares[index]

        fun add(index: Int, value: Square) {
            squares[index] = value
            if (value.isPiece) {
                pieces++
                if (value.side == Side.WHITE) current = index
            }
        }

        fun remove(index: Int): Square {
            val old = squares[index]
            if (old.isPiece) {
                squares[index] = Square(old.color)
                pieces--
                if (current == index) current = -1
            }
            return old
        }

        fun set(index: Int, value: Square) {
            remove(index)
            add(index, value)
        }

        fun move(from: Int, to: Int): Square {
            val moving = remove(from)
            val captured = remove(to)
            add(to, if (captured.isPiece) captured.convert() else moving)
            return captured
        }

        fun config(): String = squares.indices
            .filter { squares[it].isPiece }
            .joinToString(",") { squares[it].notation(it) }

        fun moves(): List<Int> {
            if (current !in 0..63) return emptyList()
            val here = squares[current]
            val row = current / 8
            val col = current % 8
            val result = mutableListOf<Int>()
            fun capture(r: Int, c: Int): Boolean {
                if (r !in 0..7 || c !in 0..7) return false
                val target = square(r, c)
                if (!squares[target].isPiece) return false
                result += target
                return true
            }
            fun ray(dr: Int, dc: Int) {
                var r = row + dr
                var c = col + dc
                while (r in 0..7 && c in 0..7) {
                    if (capture(r, c)) break
                    r += dr
                    c += dc
                }
            }
            when (here.piece) {
                Piece.PAWN -> listOf(-1 to -1, -1 to 1).forEach { capture(row + it.first, col + it.second) }
                Piece.KING -> (-1..1).forEach { dr ->
                    (-1..1).filter { dr != 0 || it != 0 }.forEach { capture(row + dr, col + it) }
                }
                Piece.KNIGHT -> listOf(
                    -2 to -1, -2 to 1, -1 to 2, 1 to 2,
                    2 to 1, 2 to -1, 1 to -2, -1 to -2,
                ).forEach { capture(row + it.first, col + it.second) }
                Piece.ROOK -> listOf(0 to -1, 0 to 1, -1 to 0, 1 to 0).forEach { ray(it.first, it.second) }
                Piece.BISHOP -> listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1).forEach { ray(it.first, it.second) }
                Piece.QUEEN -> listOf(
                    0 to -1, 0 to 1, -1 to 0, 1 to 0,
                    -1 to -1, -1 to 1, 1 to -1, 1 to 1,
                ).forEach { ray(it.first, it.second) }
                Piece.EMPTY -> Unit
            }
            return result
        }

        fun winningMove(): Int {
            if (current !in 0..63) return -1
            val row = current / 8
            val col = current % 8
            return when (squares[current].piece) {
                Piece.PAWN, Piece.KING -> if (row == 1) col else -1
                Piece.ROOK, Piece.QUEEN -> col
                Piece.KNIGHT -> when (row) {
                    1 -> if (col < 2) col + 2 else col - 2
                    2 -> if (col < 1) col + 1 else col - 1
                    else -> -1
                }
                Piece.BISHOP -> when {
                    row + col <= 7 -> row + col
                    col - row >= 0 -> col - row
                    else -> -1
                }
                Piece.EMPTY -> -1
            }
        }

        companion object {
            private val CONFIG = Regex("""([bw])([prnbkq])([a-h])([1-8])""", RegexOption.IGNORE_CASE)
            fun color(index: Int): Side =
                if ((index / 8 + index % 8) % 2 == 0) Side.WHITE else Side.BLACK
            fun square(row: Int, col: Int) = row * 8 + col
        }
    }

    private val squarePattern = Regex(
        """<td.*?background-color:\s*#([0-9a-f]+);.*?title="(.*?)".*?otherimages/chess/(blanktrans|chess_([prnbkq])([bw])([bw]))\.gif.*?</td>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val movePattern = Regex("""xy=(\d+)(?:%2C|,)(\d+)""", RegexOption.IGNORE_CASE)
    private var currentBoard: Board? = null
    private var moveCount = 0

    fun reset() {
        currentBoard = null
        moveCount = 0
    }

    fun load(config: String, save: Boolean = false, preferences: Preferences? = null): Board? {
        if (config.isBlank()) return null
        return Board(config).also {
            currentBoard = it
            if (save) preferences?.setString("lastChessboard", it.config())
        }
    }

    fun load(preferences: Preferences?): Board? =
        currentBoard ?: load(preferences?.getString("lastChessboard", "").orEmpty())

    fun boardConfig(preferences: Preferences? = null): String? = load(preferences)?.config()

    fun parseChessPuzzle(
        responseText: String?,
        preferences: Preferences? = null,
        initialVisit: Boolean = true,
    ): Board? {
        if (responseText == null) return null
        val matches = squarePattern.findAll(responseText).toList()
        if (matches.isEmpty() && responseText.contains("queen cookie", true)) {
            currentBoard = Board()
            if (!initialVisit) moveCount++
            return currentBoard
        }
        if (matches.size != 64) {
            currentBoard = null
            return null
        }
        val parsed = Board()
        matches.forEachIndexed { index, match ->
            val color = if (match.groupValues[1].startsWith("fff", true)) Side.WHITE else Side.BLACK
            val piece = if (match.groupValues[3].startsWith("blank", true)) Piece.EMPTY
            else Piece.fromCode(match.groupValues[4][0])
            val side = when (match.groupValues[5].lowercase()) {
                "w" -> Side.WHITE
                "b" -> Side.BLACK
                else -> Side.UNKNOWN
            }
            parsed.add(index, Square(color, piece, side))
        }
        currentBoard = parsed
        if (initialVisit) {
            moveCount = 0
            preferences?.setString("lastChessboard", parsed.config())
        } else {
            moveCount++
        }
        return parsed
    }

    fun parseChessMove(url: String, responseText: String, preferences: Preferences? = null): String? {
        val destination = movePattern.find(url) ?: return null
        val old = currentBoard ?: return null
        val from = old.current
        if (from !in 0..63) return null
        val to = Board.square(destination.groupValues[2].toInt(), destination.groupValues[1].toInt())
        val moving = old[from]
        val captured = old[to]
        val parsed = parseChessPuzzle(responseText, preferences, initialVisit = false) ?: return null
        val completed = parsed.pieces == 0
        if (!completed && parsed.current != to) return null
        if (completed && responseText.contains("queen cookie", true)) {
            preferences?.setInt(
                "chessboardsCleared",
                ((preferences.getInt("chessboardsCleared", 0) + 1).coerceAtMost(50)),
            )
        }
        return "$moveCount: ${moving.notation(from)}${if (completed) "-" else "x"}" +
            if (completed) coords(to) else captured.notation(to)
    }

    fun solve(preferences: Preferences? = null): List<Int>? =
        load(preferences)?.let { solve(it.copyBoard(), mutableListOf()) }

    fun step(preferences: Preferences? = null): Pair<Int, Int>? {
        val next = solve(preferences)?.firstOrNull() ?: return null
        return next % 8 to next / 8
    }

    fun test(preferences: Preferences? = null): List<String>? {
        val board = load(preferences) ?: return null
        val path = solve(preferences) ?: return null
        var from = board.current
        var piece = board[from]
        return buildList {
            add("The ${piece.piece.name.lowercase()} on square ${coords(from)}")
            path.forEachIndexed { index, to ->
                if (index == path.lastIndex) {
                    add("...which moves to square ${coords(to)} to win.")
                } else {
                    val captured = board[to]
                    add("...takes the ${captured.piece.name.lowercase()} on square ${coords(to)}")
                    piece = captured
                }
                from = to
            }
        }
    }

    private fun solve(board: Board, path: MutableList<Int>): List<Int>? {
        if (board.current < 0) return null
        if (board.pieces == 1) {
            val end = board.winningMove()
            return if (end < 0) null else path + end
        }
        val from = board.current
        val moving = board[from]
        for (to in board.moves()) {
            val captured = board.move(from, to)
            path += to
            solve(board, path)?.let { return it }
            path.removeAt(path.lastIndex)
            board.set(to, captured)
            board.set(from, moving)
        }
        return null
    }

    fun applyChoice(
        choiceId: Int,
        decision: Int,
        choiceUrl: String,
        responseText: String,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean = when (choiceId) {
        TEA_PARTY_CHOICE -> {
            if (decision == 1 || responseText.contains("already attended a Tea Party today", true)) {
                preferences?.setBoolean("_madTeaParty", true)
            }
            true
        }
        RABBIT_HOLE_CHOICE -> {
            if (decision == 5) {
                consumeItem(REFLECTION_OF_MAP_ID, 1)
                parseChessPuzzle(responseText, preferences)
            }
            true
        }
        CHESS_CHOICE -> {
            if (movePattern.containsMatchIn(choiceUrl)) parseChessMove(choiceUrl, responseText, preferences)
            else parseChessPuzzle(responseText, preferences)
            true
        }
        else -> false
    }

    fun hatDescription(length: Int): String =
        RabbitHoleAvailability.hatDataForLength(length)?.let { "${it.effect} (${it.modifier})" }
            ?: "unknown ($length characters)"

    fun hatCommand(
        inventoryCount: (Int) -> Int,
        equippedHatName: String?,
    ): List<String> {
        val hats = linkedMapOf<Int, MutableList<String>>()
        if (!equippedHatName.isNullOrBlank()) {
            hats.getOrPut(RabbitHoleAvailability.hatLength(equippedHatName)) { mutableListOf() } += equippedHatName
        }
        net.sourceforge.kolmafia.data.ItemDatabase.all().forEach { item ->
            if (item.primaryUse == net.sourceforge.kolmafia.data.ItemPrimaryUse.HAT && inventoryCount(item.id) > 0) {
                hats.getOrPut(RabbitHoleAvailability.hatLength(item.name)) { mutableListOf() } += item.name
            }
        }
        return hats.toSortedMap().mapNotNull { (length, names) ->
            RabbitHoleAvailability.hatDataForLength(length)?.let {
                "${names.distinct().joinToString(" | ")}: ${it.modifier} [${it.effect}]"
            }
        }
    }

    fun coords(index: Int): String = "${('a'.code + index % 8).toChar()}${index / 8 + 1}"
}
