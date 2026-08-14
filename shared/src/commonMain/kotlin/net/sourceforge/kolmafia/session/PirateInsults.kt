package net.sourceforge.kolmafia.session

import kotlin.math.roundToInt
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [BeerPongRequest] insult retorts and odds — dump-only, no HTTP. */
object PirateInsults {
    const val VALID_COUNT = 8

    val RETORTS = listOf(
        "Obviously neither your tongue nor your wit is sharp enough for the job.",
        "It can't be any worse than the smell of your breath!",
        "That reminds me, tell your wife and sister I had a lovely time last night.",
        "I'd've thought yellow would be more your color.",
        "I'm not really comfortable being compared to your girlfriend that way.",
        "It's an honor to learn from such an expert in the field.",
        "Amazing!  How do you manage to shave without using a mirror?",
        "It only seems that way because you haven't learned to count to one.",
    )

    fun prefKey(index: Int): String = "lastPirateInsult$index"

    fun knownRetorts(preferences: Preferences): List<String> =
        (1..VALID_COUNT).mapNotNull { i ->
            if (preferences.getBoolean(prefKey(i), false)) RETORTS.getOrNull(i - 1) else null
        }

    fun countKnown(preferences: Preferences): Int = knownRetorts(preferences).size

    /** Desktop [BeerPongRequest.pirateInsultOdds] — 0 if fewer than 3 known. */
    fun pirateInsultOdds(count: Int): Double {
        if (count < 3) return 0.0
        val n = count.toDouble()
        return (n / VALID_COUNT) *
            ((n - 1) / (VALID_COUNT - 1)) *
            ((n - 2) / (VALID_COUNT - 2))
    }

    fun formatOddsPercent(count: Int): String {
        val pct = pirateInsultOdds(count) * 100.0
        val hundredths = (pct * 100.0).roundToInt() / 100.0
        return if (hundredths == hundredths.toLong().toDouble()) {
            hundredths.toLong().toString()
        } else {
            hundredths.toString()
        }
    }
}
