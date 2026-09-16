package net.sourceforge.kolmafia.utilities

/**
 * KoL's seeded selection of [count] distinct indices from a list of [size] entries.
 * Mirrors desktop [PHPRandomSelection]: a single pick is an mtrand roll that rerolls
 * on the overflow value; multiple picks use a rand selection.
 */
class PHPRandomSelection(
    private val rng: PHPRandom,
    private val mtRng: PHPMTRandom,
) {
    constructor(seed: Long) : this(PHPRandom(seed), PHPMTRandom(seed))

    fun pick(size: Int, count: Int): IntArray {
        if (count <= 0 || size <= 0) return IntArray(0)
        if (count == 1) {
            var v = size
            while (v == size) {
                v = mtRng.nextInt(0, size)
            }
            return intArrayOf(v)
        }
        return rng.array(size, count)
    }
}
