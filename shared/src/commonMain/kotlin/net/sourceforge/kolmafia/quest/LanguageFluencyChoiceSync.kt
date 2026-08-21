package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] PirateRealm language fluency choices 1246–1254.
 */
object LanguageFluencyChoiceSync {

    val CHOICE_IDS = (1246..1254).toSet()

    const val SPACE_PIRATE_TREASURE_MAP = 9458
    const val MURDERBOT_DATA_CORE = 9431
    const val PROCRASTINATOR_LOCKER_KEY = 9462
    const val SPACE_BABY_BAWBAW = 9464

    private val FLUENCY_PATTERN = Regex("""Fluency is now (\d+)%""")

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId !in CHOICE_IDS || preferences == null) return false
        return when (choiceId) {
            1246 -> {
                if (decision != 1) return false
                parseLanguageFluency(html, "spacePirateLanguageFluency", preferences)
            }
            1247 -> {
                if (decision != 1) return false
                if (html.contains("You acquire an item")) {
                    preferences.setInt("spacePirateLanguageFluency", 0)
                    true
                } else {
                    parseLanguageFluency(html, "spacePirateLanguageFluency", preferences)
                }
            }
            1248 -> {
                if (!html.contains("You acquire an item")) return false
                consumeItem(SPACE_PIRATE_TREASURE_MAP, 1)
                true
            }
            1249 -> {
                if (decision != 1) return false
                parseLanguageFluency(html, "procrastinatorLanguageFluency", preferences)
                consumeItem(MURDERBOT_DATA_CORE, 1)
                true
            }
            1250 -> {
                if (decision != 1) return false
                if (!html.contains("You acquire an item")) return false
                preferences.setInt("procrastinatorLanguageFluency", 0)
                true
            }
            1251 -> {
                if (!html.contains("You acquire an item")) return false
                consumeItem(PROCRASTINATOR_LOCKER_KEY, 1)
                true
            }
            1252 -> decision == 1
            1253 -> {
                if (decision != 1) return false
                if (!html.contains("You acquire an item")) return false
                preferences.setInt("spaceBabyLanguageFluency", 0)
                true
            }
            1254 -> {
                if (!html.contains("You acquire an item")) return false
                consumeItem(SPACE_BABY_BAWBAW, 1)
                true
            }
            else -> false
        }
    }

    private fun parseLanguageFluency(
        html: String,
        setting: String,
        preferences: Preferences,
    ): Boolean {
        val value = FLUENCY_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        preferences.setInt(setting, value)
        return true
    }
}
