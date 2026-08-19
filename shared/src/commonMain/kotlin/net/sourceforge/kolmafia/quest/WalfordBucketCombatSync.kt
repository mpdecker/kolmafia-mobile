package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.updateQuestData] Walford bucket combat-win writers.
 */
object WalfordBucketCombatSync {

    const val ICE_HOTEL = 455
    const val VYKEA = 456
    const val ICE_HOLE = 457
    const val PREF = "walfordBucketProgress"

    private val WALFORD_PATTERN =
        Regex("""\(Walford's bucket filled by (?:an additional |)(\d+)%\)""")

    private val LOCATIONS = setOf(ICE_HOTEL, VYKEA, ICE_HOLE)

    fun apply(
        adventureId: String,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        if (adventureId.toIntOrNull() !in LOCATIONS) return false
        if (html.contains("you should take it back to Walford!")) {
            preferences.setInt(PREF, 100)
            questDatabase?.setProgress(Quest.BUCKET, "step2")
            return true
        }
        var changed = false
        WALFORD_PATTERN.findAll(html).forEach { match ->
            val amount = match.groupValues[1].toIntOrNull() ?: return@forEach
            val next = preferences.getInt(PREF, 0) + amount
            preferences.setInt(PREF, next)
            if (next >= 100) {
                questDatabase?.setProgress(Quest.BUCKET, "step2")
            }
            changed = true
        }
        return changed
    }
}
