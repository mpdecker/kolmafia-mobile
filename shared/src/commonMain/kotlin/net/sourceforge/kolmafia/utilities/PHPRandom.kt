package net.sourceforge.kolmafia.utilities

/**
 * PHP &lt; 7.1.0 glibc-style `rand` — mirrors desktop [PHPRandom].
 */
class PHPRandom(seed: Long) {
    private val state = ArrayList<Int>()

    init {
        setSeed(seed)
    }

    fun next(bits: Int = 32): Int {
        val i = state.size
        val value = state[i - 31] + state[i - 3]
        state.add(value)
        return value ushr 1
    }

    fun nextDouble(): Double = nextInt() / (Int.MAX_VALUE + 1.0)

    fun nextInt(): Int = next(32)

    fun nextInt(max: Int): Int = nextInt(0, max)

    fun nextInt(min: Int, max: Int): Int {
        val clamped = (max - min + 1.0) * nextDouble()
        return min + clamped.toInt()
    }

    fun setSeed(seed: Long) {
        state.clear()
        state.add(seed.toInt())
        for (i in 1 until 31) {
            var value = ((16_807L * state[i - 1]) % Int.MAX_VALUE).toInt()
            if (value < 0) value += Int.MAX_VALUE
            state.add(value)
        }
        for (i in 31 until 34) {
            state.add(state[i - 31])
        }
        for (i in 34 until 344) {
            next(32)
        }
    }

    fun <T> shuffle(array: MutableList<T>) {
        for (i in array.size - 1 downTo 1) {
            val roll = nextInt(0, i)
            if (roll == i) continue
            val temp = array[i]
            array[i] = array[roll]
            array[roll] = temp
        }
    }
}
