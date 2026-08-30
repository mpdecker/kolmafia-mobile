package net.sourceforge.kolmafia.session

/** PHP-compatible deterministic Voting Booth initiative selection. */
object VotingBoothManager {
    data class ModifierValue(val name: String, val value: String) {
        override fun toString(): String = "$name: $value"
    }

    private val positive = arrayOf(
        ModifierValue("Monster Level", "+10"), ModifierValue("Food Drop", "+30"),
        ModifierValue("Monster Level", "-10"), ModifierValue("Initiative", "+25"),
        ModifierValue("Stench Damage", "+10"), ModifierValue("Sleaze Damage", "+10"),
        ModifierValue("Pants Drop", "+30"), ModifierValue("Maximum MP Percent", "+30"),
        ModifierValue("Moxie Percent", "+25"), ModifierValue("Ranged Damage Percent", "+100"),
        ModifierValue("Experience (Mysticality)", "+4"), ModifierValue("Experience (Moxie)", "+4"),
        ModifierValue("Weapon Damage Percent", "+100"), ModifierValue("Stench Resistance", "+3"),
        ModifierValue("Booze Drop", "+30"), ModifierValue("Item Drop", "+15"),
        ModifierValue("Cold Damage", "+10"), ModifierValue("Hot Resistance", "+3"),
        ModifierValue("Weapon Damage", "[20*unarmed]"), ModifierValue("Muscle Percent", "+25"),
        ModifierValue("Experience", "+3"), ModifierValue("Spell Damage Percent", "+20"),
        ModifierValue("Spooky Resistance", "+3"), ModifierValue("Hot Damage", "+10"),
        ModifierValue("Meat Drop", "+30"), ModifierValue("Experience (familiar)", "+2"),
        ModifierValue("Mysticality Percent", "+25"), ModifierValue("Cold Resistance", "+3"),
        ModifierValue("Experience (Muscle)", "+4"), ModifierValue("Gear Drop", "+30"),
        ModifierValue("Adventures", "+1"), ModifierValue("Candy Drop", "+30"),
        ModifierValue("Maximum HP Percent", "+30"), ModifierValue("Sleaze Resistanc", "+3"),
    )
    private val negative = arrayOf(
        ModifierValue("Maximum MP Percent", "-50"), ModifierValue("Initiative", "-30"),
        ModifierValue("Moxie", "-20"), ModifierValue("Experience", "-3"),
        ModifierValue("Spell Damage Percent", "-50"), ModifierValue("Muscle", "-20"),
        ModifierValue("Meat Drop", "-30"), ModifierValue("Adventures", "-2"),
        ModifierValue("Item Drop", "-20"), ModifierValue("Critical Hit Percent", "-10"),
        ModifierValue("Experience (familiar)", "-2"), ModifierValue("Gear Drop", "-50"),
        ModifierValue("Maximum HP Percent", "-50"), ModifierValue("Mysticality", "-20"),
        ModifierValue("Weapon Damage Percent", "-50"),
    )

    fun calculateSeed(clss: Int, path: Int, dayCount: Int): Int = 4 * path + 9 * clss + 79 * dayCount

    fun getInitiatives(clss: Int, path: Int, dayCount: Int): List<ModifierValue> {
        val seed = calculateSeed(clss, path, dayCount)
        val rng = PhpRandom(seed)
        val selected = rng.array(positive.size, 3).map { positive[it] }.toMutableList()
        val mt = PhpMtRandom(seed)
        var n = 15
        while (n > 14) n = mt.nextInt(0, 15)
        selected += negative[n]
        return selected
    }

    private class PhpRandom(seed: Int) {
        private val state = ArrayList<Int>(400)
        init {
            state += seed
            for (i in 1 until 31) {
                var value = ((16_807L * state[i - 1].toLong()) % Int.MAX_VALUE).toInt()
                if (value < 0) value += Int.MAX_VALUE
                state += value
            }
            for (i in 31 until 34) state += state[i - 31]
            repeat(310) { nextBits() }
        }
        private fun nextBits(): Int {
            val i = state.size
            val value = state[i - 31] + state[i - 3]
            state += value
            return value ushr 1
        }
        private fun nextDouble(): Double = nextBits() / (Int.MAX_VALUE + 1.0)
        fun array(count: Int, required: Int): IntArray {
            val result = IntArray(required.coerceAtMost(count))
            var j = 0
            for (i in 0 until count) {
                if (j == result.size) break
                if (nextDouble() < (result.size - j).toDouble() / (count - i)) result[j++] = i
            }
            return result
        }
    }

    private class PhpMtRandom(seed: Int) {
        private val state = LongArray(624)
        private var index = 624
        init {
            state[0] = seed.toLong() and 0xffffffffL
            for (i in 1 until 624) {
                state[i] = (1_812_433_253L * (state[i - 1] xor (state[i - 1] shr 30)) + i) and 0xffffffffL
            }
        }
        private fun nextBits(): Int {
            if (index >= 624) {
                for (i in 0 until 624) {
                    val y = (state[i] and 0x80000000L) or (state[(i + 1) % 624] and 0x7fffffffL)
                    state[i] = state[(i + 397) % 624] xor (y shr 1) xor if ((y and 1L) != 0L) 0x9908b0dfL else 0L
                }
                index = 0
            }
            var y = state[index++]
            y = y xor (y shr 11)
            y = y xor ((y shl 7) and 2_636_928_640L)
            y = y xor ((y shl 15) and 4_022_730_752L)
            return (y xor (y shr 18)).toInt() ushr 1
        }
        private fun nextDouble(): Double = nextBits() / (Int.MAX_VALUE + 1.0)
        fun nextInt(min: Int, max: Int): Int = min + ((max - min + 1) * nextDouble()).toInt()
    }
}
