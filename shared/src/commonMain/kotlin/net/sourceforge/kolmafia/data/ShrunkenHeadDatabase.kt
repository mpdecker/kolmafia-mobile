package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.utilities.PHPMTRandom
import net.sourceforge.kolmafia.utilities.PHPRandom

/** Desktop [ShrunkenHeadDatabase] — path/monster seeded ability picks. */
object ShrunkenHeadDatabase {
    private val abilities = listOf(
        "Item Drop Bonus",
        "Meat Drop Bonus",
        "Physical Attack",
        "Hot Attack",
        "Cold Attack",
        "Sleaze Attack",
        "Stench Attack",
        "Spooky Attack",
        "MP Regen",
        "HP Regen",
    )

    fun shrunkenHeadZombie(monsterId: Int, pathId: Int): List<String> {
        val seed = monsterId * 12345L + pathId * 99L
        val mtRand = PHPMTRandom(seed)
        val rand = PHPRandom(seed)
        val count = mtRand.nextInt(1, 2) + mtRand.nextInt(1, 2)
        val lst = (0 until 10).toMutableList()
        rand.shuffle(lst)
        return lst.take(count).sorted().map { abilities[it] }
    }
}
