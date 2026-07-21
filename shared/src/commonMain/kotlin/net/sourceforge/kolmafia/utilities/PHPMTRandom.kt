package net.sourceforge.kolmafia.utilities

/**
 * PHP-compatible Mersenne Twister. Mirrors desktop [PHPMTRandom].
 */
class PHPMTRandom(seed: Long) {
    private val state = ArrayList<Long>(STATE_LENGTH * 2)
    private var index = 0

    init {
        setSeed(seed)
    }

    fun nextInt(max: Int): Int = nextInt(0, max)

    fun nextInt(min: Int, max: Int): Int {
        val clamped = (max - min + 1.0) * nextDouble()
        return min + clamped.toInt()
    }

    fun nextDouble(): Double = nextBits() / (Int.MAX_VALUE + 1.0)

    private fun nextBits(): Int {
        if (index >= state.size) {
            reload()
        }
        val value = state[index++]
        return (temper(value) shr 1).toInt()
    }

    private fun setSeed(seed: Long) {
        var s = seed
        if (s < 0) {
            s += 0x100000000L
        }
        initialize(s)
        reload()
    }

    private fun initialize(seed: Long) {
        state.clear()
        state.add(seed and Long.MAX_VALUE)
        for (i in 1 until STATE_LENGTH) {
            val prev = state[i - 1]
            val value = ((1_812_433_253L * (prev xor (prev shr 30))) + i) and 4_294_967_295L
            state.add(value)
        }
        index = state.size
    }

    private fun reload() {
        val stateShift = index - STATE_LENGTH
        for (i in stateShift until stateShift + STATE_LENGTH) {
            val value = twist(state[i + PERIOD], state[i], state[i + 1])
            state.add(value)
        }
    }

    companion object {
        private const val STATE_LENGTH = 624
        private const val PERIOD = 397
        private const val MAGIC_CONSTANT = 0x9908b0dfL
        private const val MAX_UNSIGNED = 0xFFFFFFFFL

        private fun hiBit(u: Long): Long = u and 0x80000000L
        private fun loBit(u: Long): Long = u and 0x00000001L
        private fun loBits(u: Long): Long = u and 0x7FFFFFFFL
        private fun mixBits(u: Long, v: Long): Long = hiBit(u) or loBits(v)

        private fun twist(m: Long, u: Long, v: Long): Long =
            m xor (mixBits(u, v) shr 1) xor ((MAX_UNSIGNED * loBit(u)) and MAGIC_CONSTANT)

        private fun temper(value: Long): Long {
            var v = value
            v = v xor (v shr 11)
            v = v xor ((v shl 7) and 2_636_928_640L)
            v = v xor ((v shl 15) and 4_022_730_752L)
            return v xor (v shr 18)
        }
    }
}
