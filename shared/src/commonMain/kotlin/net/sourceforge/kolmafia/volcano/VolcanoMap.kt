package net.sourceforge.kolmafia.volcano

import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.CELLS
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.GOAL
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.NCOLS
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.NROWS
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.pos

class VolcanoMap(val coordinates: String) : Comparable<VolcanoMap> {
    val platforms: IntArray
    val board = BooleanArray(CELLS)

    init {
        val list = mutableListOf<Int>()
        for (token in coordinates.split(',')) {
            val trimmed = token.trim()
            if (trimmed.isEmpty()) continue
            val value = trimmed.toIntOrNull() ?: continue
            if (value !in VolcanoMazeConstants.MIN_SQUARE..VolcanoMazeConstants.MAX_SQUARE) continue
            list.add(value)
            board[value] = true
        }
        platforms = list.toIntArray()
        board[GOAL] = true
    }

    fun inMap(row: Int, col: Int): Boolean = inMap(pos(row, col))

    fun inMap(square: Int): Boolean = board[square]

    fun neighbors(square: Int): Neighbors = Neighbors(square, this)

    fun pickNeighbor(square: Int): Int {
        val neighborPlatforms = neighbors(square).platforms
        if (neighborPlatforms.isEmpty()) return -1
        if (neighborPlatforms.size == 1) {
            val next = neighborPlatforms[0]
            return if (next != GOAL) next else -1
        }
        var next = GOAL
        while (next == GOAL) {
            val rnd = VolcanoMapRng.nextInt(neighborPlatforms.size)
            next = neighborPlatforms[rnd]
        }
        return next
    }

    fun printMap(player: Int, print: (String) -> Unit) {
        val prow = if (player >= 0) VolcanoMazeConstants.row(player) else -1
        val pcol = if (player >= 0) VolcanoMazeConstants.col(player) else -1
        for (row in 0 until NROWS) {
            val line = buildString {
                if (row < 9) append(' ')
                append(row + 1)
                for (col in 0 until NCOLS) {
                    append(' ')
                    append(
                        when {
                            player >= 0 && row == prow && col == pcol -> '@'
                            row == 6 && col == 6 -> '*'
                            board[pos(row, col)] -> 'O'
                            else -> '.'
                        },
                    )
                }
            }
            print(line)
        }
    }

    override fun equals(other: Any?): Boolean =
        other is VolcanoMap && coordinates == other.coordinates

    override fun hashCode(): Int = coordinates.hashCode()

    override fun compareTo(other: VolcanoMap): Int = coordinates.compareTo(other.coordinates)
}
