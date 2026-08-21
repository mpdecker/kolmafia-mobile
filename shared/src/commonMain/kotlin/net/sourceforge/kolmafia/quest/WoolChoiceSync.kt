package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Slagging Off 1489 + Woolin' Around 1490.
 */
object WoolChoiceSync {

    const val SLAGGING_CHOICE = 1489
    const val WOOL_CHOICE = 1490

    const val CRIMBO_CRYSTAL_SHARDS_ID = 11066
    const val GRUBBY_WOOL_ID = 11091

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (preferences == null) return false
        return when (choiceId) {
            SLAGGING_CHOICE -> {
                if (decision !in 1..2) return false
                consumeItem(CRIMBO_CRYSTAL_SHARDS_ID, 1)
                true
            }
            WOOL_CHOICE -> {
                if (decision !in 1..6) return false
                consumeItem(GRUBBY_WOOL_ID, 1)
                true
            }
            else -> false
        }
    }
}
