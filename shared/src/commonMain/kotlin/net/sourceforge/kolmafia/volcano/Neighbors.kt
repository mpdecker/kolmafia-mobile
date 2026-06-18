package net.sourceforge.kolmafia.volcano

import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.NCOLS
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.NROWS
import net.sourceforge.kolmafia.volcano.VolcanoMazeConstants.pos

class Neighbors(square: Int, map: VolcanoMap?) {
    val platforms: IntArray

    init {
        val row = VolcanoMazeConstants.row(square)
        val col = VolcanoMazeConstants.col(square)
        val list = mutableListOf<Int>()
        addSquare(list, map, row - 1, col - 1)
        addSquare(list, map, row - 1, col)
        addSquare(list, map, row - 1, col + 1)
        addSquare(list, map, row, col - 1)
        addSquare(list, map, row, col + 1)
        addSquare(list, map, row + 1, col - 1)
        addSquare(list, map, row + 1, col)
        addSquare(list, map, row + 1, col + 1)
        platforms = list.toIntArray()
    }

    private companion object {
        fun addSquare(list: MutableList<Int>, map: VolcanoMap?, row: Int, col: Int) {
            if (row in 0 until NROWS && col in 0 until NCOLS) {
                val square = pos(row, col)
                if (map == null || map.inMap(square)) {
                    list.add(square)
                }
            }
        }
    }
}
