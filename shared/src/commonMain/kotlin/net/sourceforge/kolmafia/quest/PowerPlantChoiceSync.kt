package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Potted Power Plant choice 1448 —
 * visit stalk parse + post harvest zeroing.
 */
object PowerPlantChoiceSync {

    const val CHOICE_ID = 1448

    const val STALKS_PREF = "_pottedPowerPlant"
    const val STALK_COUNT = 7

    private val STALK_PATTERN = Regex(
        """<button.*?name="pp" value="(\d+)".*?>\s+<img.*?src=".*?/otherimages/powerplant/(\d+)\.png""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val PP_URL_PATTERN = Regex("""pp=(\d+)""", RegexOption.IGNORE_CASE)

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val stalkStatus = Array(STALK_COUNT) { "" }
        var found = false
        STALK_PATTERN.findAll(html).forEach { match ->
            val pp = (match.groupValues[1].toIntOrNull() ?: return@forEach) - 1
            if (pp !in 0 until STALK_COUNT) return@forEach
            stalkStatus[pp] = match.groupValues[2]
            found = true
        }
        if (!found) return false
        preferences.setString(STALKS_PREF, stalkStatus.joinToString(","))
        return true
    }

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        if (!html.contains("You acquire an item:")) return false
        val pp = PP_URL_PATTERN.find(choiceUrl)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        val index = pp - 1
        if (index !in 0 until STALK_COUNT) return false
        val parts = preferences.getString(STALKS_PREF, "")
            .split(",")
            .toMutableList()
        while (parts.size < STALK_COUNT) parts.add("")
        parts[index] = "0"
        preferences.setString(STALKS_PREF, parts.take(STALK_COUNT).joinToString(","))
        return true
    }
}
