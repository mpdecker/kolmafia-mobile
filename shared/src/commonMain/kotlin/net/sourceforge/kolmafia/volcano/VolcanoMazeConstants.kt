package net.sourceforge.kolmafia.volcano

object VolcanoMazeConstants {
    const val MAPS = 5
    const val NROWS = 13
    const val NCOLS = 13
    const val CELLS = NROWS * NCOLS
    const val MIN_SQUARE = 0
    const val MAX_SQUARE = CELLS - 1
    const val START = 162
    const val GOAL = 84

    val GOAL_NEIGHBORS: Set<Int> = setOf(70, 71, 72, 83, 85, 96, 97, 98)

    fun row(pos: Int): Int = pos / NCOLS
    fun col(pos: Int): Int = pos % NCOLS
    fun pos(row: Int, col: Int): Int = row * NCOLS + col

    fun coordinateString(pos: Int): String {
        if (pos < 0) return "(unknown)"
        return "${col(pos)},${row(pos)}"
    }

    fun coordinateString(pos: Int, map: Int): String {
        val cstr = coordinateString(pos)
        val mstr = if (map >= 0) "map ${map + 1}" else "(unknown map)"
        return "$cstr on $mstr"
    }
}
