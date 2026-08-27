package net.sourceforge.kolmafia.utilities

/**
 * PHP combined linear congruential generator (pre-7.1 `rand()`).
 * Desktop KoLmafia uses this for `php_seed` / `php_rand` ASH.
 */
class PHPLCG(seed: Long) {
    private var s1: Long
    private var s2: Long

    init {
        val clamped = if (seed < 0) -seed else seed
        s1 = (clamped % MOD1).let { if (it == 0L) 1L else it }
        s2 = (clamped % MOD2).let { if (it == 0L) 1L else it }
    }

    fun rand(): Int {
        s1 = (s1 * 13885L + 1L) % MOD1
        s2 = (s2 * 28573L + 1L) % MOD2
        val z = ((s1 - s2) % (MOD1 - 1L) + (MOD1 - 1L)) % (MOD1 - 1L)
        return z.toInt()
    }

    companion object {
        private const val MOD1 = 2_147_483_563L
        private const val MOD2 = 2_147_483_399L
    }
}
