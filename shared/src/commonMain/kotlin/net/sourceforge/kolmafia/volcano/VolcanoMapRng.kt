package net.sourceforge.kolmafia.volcano

import kotlin.random.Random

/** Deterministic RNG matching desktop [VolcanoMazeManager] seed. */
object VolcanoMapRng {
    private const val SEED = 0xe1d2c3b4a596L
    private var rng: Random = Random(SEED)

    fun resetRng() {
        rng = Random(SEED)
    }

    internal fun nextInt(bound: Int): Int = rng.nextInt(bound)
}
