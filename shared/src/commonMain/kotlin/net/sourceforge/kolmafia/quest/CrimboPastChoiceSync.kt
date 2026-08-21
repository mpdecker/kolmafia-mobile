package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [SkeletonOfCrimboPastRequest.visit] for choice 1567 —
 * sold-out / daily-special prefs only (no coinmaster buy HTTP).
 */
object CrimboPastChoiceSync {

    const val CHOICE_ID = 1567

    private val DAILY_SPECIAL = Regex(
        """Daily Special:.*?descitem\((\d+)\).*?\((\d+) knucklebones\)?""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        itemIdFromDesc: (String) -> Int? = { ItemDatabase.getByDescId(it)?.id },
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        preferences.setBoolean("_crimboPastSmokingPope", !html.contains("Buy a Smoking Pope"))
        preferences.setBoolean("_crimboPastPrizeTurkey", !html.contains("Buy a prize turkey"))
        preferences.setBoolean("_crimboPastMedicalGruel", !html.contains("Buy medical gruel"))
        preferences.setBoolean("_crimboPastDailySpecial", !html.contains("Daily Special"))
        DAILY_SPECIAL.find(html)?.let { match ->
            val itemId = itemIdFromDesc(match.groupValues[1]) ?: return@let
            val price = match.groupValues[2].toIntOrNull() ?: return@let
            preferences.setInt("_crimboPastDailySpecialItem", itemId)
            preferences.setInt("_crimboPastDailySpecialPrice", price)
        }
        return true
    }
}
