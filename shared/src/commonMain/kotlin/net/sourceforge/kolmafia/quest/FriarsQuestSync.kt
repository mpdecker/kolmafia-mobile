package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleFriarsCopseChange] + [QuestManager.handleFriarsChange].
 */
object FriarsQuestSync {

    const val DARK_ELBOW = 539
    const val DARK_HEART = 540
    const val DARK_NECK = 541

    const val DODECAGRAM = 479
    const val CANDLES = 480
    const val BUTTERKNIFE = 481

    const val ELBOW_NAME = "The Dark Elbow of the Woods"
    const val HEART_NAME = "The Dark Heart of the Woods"
    const val NECK_NAME = "The Dark Neck of the Woods"

    private val elbowNcs = setOf(
        "Deep Imp Act",
        "Imp Art, Some Wisdom",
        "A Secret, But Not the Secret You're Looking For",
        "Butter Knife?  I'll Take the Knife",
    )
    private val heartNcs = setOf(
        "Moon Over the Dark Heart",
        "Running the Lode",
        "I, Martin",
        "Imp Be Nimble, Imp Be Quick",
    )
    private val neckNcs = setOf(
        "How Do We Do It? Quaint and Curious Volume!",
        "Strike One!",
        "Olive My Love To You, Oh.",
        "Dodecahedrariffic!",
    )

    fun applyFromAdventure(
        adventureId: String?,
        html: String,
        preferences: Preferences?,
        getTurns: (String) -> Int = { 0 },
        url: String? = null,
    ): Boolean {
        if (preferences == null) return false
        val area = adventureId?.toIntOrNull()
            ?: Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE).find(url.orEmpty())
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        return when (area) {
            DARK_ELBOW -> stampNc(html, elbowNcs, "lastFriarsElbowNC", ELBOW_NAME, preferences, getTurns)
            DARK_HEART -> stampNc(html, heartNcs, "lastFriarsHeartNC", HEART_NAME, preferences, getTurns)
            DARK_NECK -> stampNc(html, neckNcs, "lastFriarsNeckNC", NECK_NAME, preferences, getTurns)
            else -> false
        }
    }

    fun applyCeremony(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        if (url != null && !url.contains("friars.php", ignoreCase = true)) return false
        if (!html.contains("Thank you") &&
            !html.contains("Please return to us if there's ever anything we can do for you in return")
        ) {
            return false
        }
        consumeItem(DODECAGRAM, 1)
        consumeItem(CANDLES, 1)
        consumeItem(BUTTERKNIFE, 1)
        preferences.setInt(
            "lastFriarCeremonyAscension",
            preferences.getInt("knownAscensions", 0),
        )
        questDatabase.setProgress(Quest.FRIAR, QuestDatabase.FINISHED)
        return true
    }

    private fun stampNc(
        html: String,
        titles: Set<String>,
        pref: String,
        locationName: String,
        preferences: Preferences,
        getTurns: (String) -> Int,
    ): Boolean {
        if (titles.none { html.contains(it) }) return false
        preferences.setInt(pref, getTurns(locationName))
        return true
    }
}
