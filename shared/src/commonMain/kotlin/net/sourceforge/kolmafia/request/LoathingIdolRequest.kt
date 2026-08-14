package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [LoathingIdolCommand] — Loathing Idol Microphone stance buffs (choice 1505). */
class LoathingIdolRequest(
    private val useItemRequest: UseItemRequest,
    private val choiceRequest: ChoiceRequest,
) {
    suspend fun takeStance(
        stance: Int,
        preferences: Preferences?,
        inventoryCounts: (Int) -> Int,
    ): Result<String> {
        if (stance !in 1..4) {
            return Result.failure(IllegalArgumentException("Unknown Loathing Idol stance."))
        }
        val micId = findMicrophone(inventoryCounts)
            ?: return Result.failure(
                IllegalStateException("You need a Loathing Idol Microphone first."),
            )
        val prefs = preferences
        val previous = prefs?.getInt(CHOICE_PREF, 0) ?: 0
        prefs?.setInt(CHOICE_PREF, stance)
        return try {
            val useResult = useItemRequest.use(micId, 1)
            useResult.exceptionOrNull()?.let { return Result.failure(it) }
            // Desktop auto-follows choiceAdventure1505; if still in choice, submit explicitly.
            val choose = choiceRequest.choose(CHOICE_ID, stance)
            Result.success(choose.getOrNull()?.first ?: useResult.getOrThrow())
        } finally {
            prefs?.setInt(CHOICE_PREF, previous)
        }
    }

    companion object {
        const val CHOICE_ID = 1505
        const val CHOICE_PREF = "choiceAdventure1505"
        val MICROPHONE_IDS = listOf(11279, 11278, 11277, 11263)

        fun findMicrophone(inventoryCounts: (Int) -> Int): Int? =
            MICROPHONE_IDS.firstOrNull { inventoryCounts(it) > 0 }

        /** Desktop aliases: pop/moxie/init → 1, ballad/combat → 2, rhyme/item → 3, country/exp/res → 4. */
        fun findStance(parameters: String): Int {
            val p = parameters.trim().lowercase()
            if (p.isEmpty()) return 0
            return when {
                p.contains("pop") || p.contains("moxie") || p.contains("init") -> 1
                p.contains("ballad") || p.contains("combat") -> 2
                p.contains("rhyme") || p.contains("item") -> 3
                p.contains("country") || p.contains("exp") || p.contains("res") -> 4
                else -> 0
            }
        }
    }
}
