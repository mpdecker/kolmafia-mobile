package net.sourceforge.kolmafia.session

import kotlin.math.abs
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.NumberologyRequest

/** Desktop [NumberologyManager] prize table + reverse seed map + Calculate the Universe submit. */
object NumberologyManager {
    const val TRY_AGAIN = "Try Again"

    private val PRIZES = arrayOf(
        "0 Meat",
        "seal-clubbing club",
        "100 Sleepy",
        "100 Confused",
        "100 Embarrassed",
        "100 Far Out",
        "100 Wings",
        "100 Beaten Up",
        "100 Hardly Poisoned at All",
        "100 Knob Goblin Perfume",
        "100 Steroid Boost",
        "Drunkenness (-1,+3)",
        "fight Gnollish Gearhead",
        "Nothing. Maybe.",
        "moxie weed",
        "15 Meat",
        "magicalness-in-a-can",
        "1 Adventure",
        "bottle of booze",
        "+ Moxie",
        "- Mainstate",
        "1 Fite",
        "pygmy phone number",
        "+ Muscle",
        TRY_AGAIN,
        TRY_AGAIN,
        TRY_AGAIN,
        "+ Moxie",
        TRY_AGAIN,
        TRY_AGAIN,
        "fight ghuol",
        TRY_AGAIN,
        TRY_AGAIN,
        "magicalness-in-a-can",
        "+ Mainstat",
        "+ Muscle",
        "2 Adventures",
        "3 Fites",
        "+ Mysticality",
        TRY_AGAIN,
        "40 Meat",
        TRY_AGAIN,
        TRY_AGAIN,
        "+ Muscle",
        "bottle of booze",
        "magicalness-in-a-can",
        TRY_AGAIN,
        "+ Moxie",
        "fight your butt",
        "+ Mysticality",
        "- Mysticality",
        "fight War Frat 151st Infantryman",
        TRY_AGAIN,
        TRY_AGAIN,
        TRY_AGAIN,
        TRY_AGAIN,
        TRY_AGAIN,
        "+ Moxie",
        "10 Teleportitis",
        TRY_AGAIN,
        TRY_AGAIN,
        TRY_AGAIN,
        TRY_AGAIN,
        "+ Muscle",
        TRY_AGAIN,
        TRY_AGAIN,
        "magicalness-in-a-can",
        TRY_AGAIN,
        "+ Mysticality",
        "3 Adventures",
        "+ Mainstat",
        "- Mysticality",
        TRY_AGAIN,
        TRY_AGAIN,
        TRY_AGAIN,
        "bottle of booze",
        TRY_AGAIN,
        "spooky stick",
        "+ Mysticality",
        TRY_AGAIN,
        TRY_AGAIN,
        TRY_AGAIN,
        TRY_AGAIN,
        "+ Muscle",
        TRY_AGAIN,
        TRY_AGAIN,
        TRY_AGAIN,
        "+ Moxie",
        "magicalness-in-a-can",
        "+ Mainstat",
        "- Muscle",
        TRY_AGAIN,
        TRY_AGAIN,
        "93 Meat",
        TRY_AGAIN,
        TRY_AGAIN,
        TRY_AGAIN,
        "magicalness-in-a-can",
        "+ Mysticality",
        "bottle of booze",
    )

    fun numberologyPrize(result: Int): String = PRIZES[Math.floorMod(result, PRIZES.size)]

    fun signIndex(cs: CharacterState): Int =
        ZodiacSign.find(cs.zodiacSign)?.id ?: cs.moonSign

    fun rawNumberology(
        cs: CharacterState,
        seed: Int,
        adventureDelta: Int = 0,
        spleenDelta: Int = 0,
    ): Int = (abs(seed) + cs.ascensionNumber + signIndex(cs)) *
        (cs.spleenUsed + spleenDelta + cs.level) +
        (cs.adventuresLeft - adventureDelta)

    fun numberology(
        cs: CharacterState,
        seed: Int,
        adventureDelta: Int = 0,
        spleenDelta: Int = 0,
    ): Int = Math.floorMod(rawNumberology(cs, seed, adventureDelta, spleenDelta), 100)

    fun reverseNumberology(
        cs: CharacterState,
        adventureDelta: Int = 0,
        spleenDelta: Int = 0,
    ): Map<Int, Int> {
        val results = linkedMapOf<Int, Int>()
        for (seed in 0 until 100) {
            val result = numberology(cs, seed, adventureDelta, spleenDelta)
            if (results.containsKey(result)) return results.toSortedMap()
            results[result] = seed
        }
        return results.toSortedMap()
    }

    suspend fun calculateTheUniverse(
        seed: Int,
        request: NumberologyRequest,
        preferences: Preferences?,
        characterState: CharacterState,
    ): Result<String> = request.calculate(seed, preferences, characterState)
}
