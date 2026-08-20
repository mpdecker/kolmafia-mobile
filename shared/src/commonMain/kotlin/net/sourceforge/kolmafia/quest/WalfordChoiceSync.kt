package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Walford Rusley choices 1114–1116.
 */
object WalfordChoiceSync {

    const val COLLECTOR = 1114
    const val VYKEA = 1115
    const val ICE_HOTEL = 1116

    val WALFORD_PATTERN =
        Regex("""\(Walford's bucket filled by (?:an additional |)?(\d+)%\)""")

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        return when (choiceId) {
            COLLECTOR -> applyCollector(decision, html, questDatabase, preferences)
            VYKEA -> applyVykea(decision, html, questDatabase, preferences)
            ICE_HOTEL -> applyIceHotel(decision, html, questDatabase, preferences)
            else -> false
        }
    }

    private fun applyCollector(
        decision: Int,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences,
    ): Boolean {
        if (questDatabase == null) return false
        if (decision == 1) {
            questDatabase.setProgress(Quest.BUCKET, QuestDatabase.UNSTARTED)
            preferences.setInt("walfordBucketProgress", 0)
            preferences.setString("walfordBucketItem", "")
            return true
        }
        if (decision >= 5) return false
        questDatabase.setProgress(Quest.BUCKET, QuestDatabase.STARTED)
        preferences.setInt("walfordBucketProgress", 0)
        preferences.setBoolean("_walfordQuestStartedToday", true)
        val item = when {
            html.contains("Bucket of balls") -> "balls"
            html.contains("bucket with blood") -> "blood"
            html.contains("Bolts, mainly") -> "bolts"
            html.contains("bucket of chicken") -> "chicken"
            html.contains("Here y'go -- chum") -> "chum"
            html.contains("fill that with ice") -> "ice"
            html.contains("fill it up with milk") -> "milk"
            html.contains("bucket of moonbeams") -> "moonbeams"
            html.contains("bucket with rain") -> "rain"
            else -> null
        }
        if (item != null) {
            preferences.setString("walfordBucketItem", item)
        }
        return true
    }

    private fun applyVykea(
        decision: Int,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences,
    ): Boolean = when (decision) {
        1 -> {
            preferences.setBoolean("_VYKEACafeteriaRaided", true)
            true
        }
        3 -> incrementBucket(html, questDatabase, preferences)
        4 -> {
            preferences.setBoolean("_VYKEALoungeRaided", true)
            true
        }
        else -> false
    }

    private fun applyIceHotel(
        decision: Int,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences,
    ): Boolean = when (decision) {
        3 -> incrementBucket(html, questDatabase, preferences)
        5 -> {
            preferences.setBoolean("_iceHotelRoomsRaided", true)
            true
        }
        else -> false
    }

    private fun incrementBucket(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences,
    ): Boolean {
        val amount = WALFORD_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return false
        val next = preferences.getInt("walfordBucketProgress", 0) + amount
        preferences.setInt("walfordBucketProgress", next)
        if (next >= 100) {
            questDatabase?.setProgress(Quest.BUCKET, "step2")
        }
        return true
    }
}
