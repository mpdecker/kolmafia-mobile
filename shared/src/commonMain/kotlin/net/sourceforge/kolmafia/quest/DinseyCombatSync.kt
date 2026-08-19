package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.updateQuestData] Dinsey location combat-win writers.
 */
object DinseyCombatSync {

    const val SECRET_GOVERNMENT_LAB = 416
    const val DEEP_DARK_JUNGLE = 417
    const val BARF_MOUNTAIN = 442
    const val GARBAGE_BARGES = 443
    const val TOXIC_TEACUPS = 444
    const val LIQUID_WASTE_SLUICE = 445

    private val GORE_PATTERN = Regex("""(\d+) pounds of (?:the gore|gore)""")

    fun apply(
        adventureId: String,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (preferences == null) return false
        return when (adventureId.toIntOrNull()) {
            DEEP_DARK_JUNGLE -> applyJungle(html, questDatabase, preferences)
            SECRET_GOVERNMENT_LAB -> applyGore(html, questDatabase, preferences)
            BARF_MOUNTAIN -> applyBarf(html, preferences)
            GARBAGE_BARGES -> applyBarges(html, questDatabase, preferences)
            TOXIC_TEACUPS -> applyTeacups(html, questDatabase, preferences)
            LIQUID_WASTE_SLUICE -> applySluice(html, questDatabase, preferences)
            else -> false
        }
    }

    private fun applyJungle(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences,
    ): Boolean {
        if (!html.contains("jungle pun occurs to you")) return false
        val next = preferences.getInt("junglePuns", 0) + 1
        preferences.setInt("junglePuns", next)
        if (next >= 11) {
            questDatabase?.setProgress(Quest.JUNGLE_PUN, "step2")
        }
        return true
    }

    private fun applyGore(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences,
    ): Boolean {
        var changed = false
        val amount = GORE_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (amount != null) {
            val next = preferences.getInt("goreCollected", 0) + amount
            preferences.setInt("goreCollected", next)
            if (next >= 100) {
                questDatabase?.setProgress(Quest.GORE, "step2")
            }
            changed = true
        }
        if (html.contains("The gore sloshes around nauseatingly in your bucket")) {
            questDatabase?.setProgress(Quest.GORE, "step2")
            changed = true
        }
        return changed
    }

    private fun applyBarf(html: String, preferences: Preferences): Boolean {
        if (!html.contains("made it to the front of the line")) return false
        preferences.setBoolean("dinseyRollercoasterNext", true)
        return true
    }

    private fun applyBarges(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences,
    ): Boolean {
        var changed = false
        if (questDatabase != null && questDatabase.isQuestLaterThan(Quest.SOCIAL_JUSTICE_I, QuestDatabase.UNSTARTED)) {
            preferences.setInt(
                "dinseySocialJusticeIProgress",
                preferences.getInt("dinseySocialJusticeIProgress", 0) + 1,
            )
            changed = true
        } else if (html.contains("probably not embarrassingly sexist anymore")) {
            preferences.setInt("dinseySocialJusticeIProgress", 15)
            questDatabase?.setProgress(Quest.SOCIAL_JUSTICE_I, "step1")
            changed = true
        }
        if (html.contains("at least the barges aren't getting hung up on it anymore")) {
            preferences.setInt("dinseyFilthLevel", 0)
            questDatabase?.setProgress(Quest.FISH_TRASH, "step2")
            changed = true
        } else if (html.contains("larger chunks of garbage out of the waterway")) {
            preferences.setInt(
                "dinseyFilthLevel",
                (preferences.getInt("dinseyFilthLevel", 0) - 5).coerceAtLeast(0),
            )
            questDatabase?.setProgress(Quest.FISH_TRASH, "step1")
            changed = true
        }
        return changed
    }

    private fun applyTeacups(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences,
    ): Boolean {
        return when {
            html.contains("pretend to be having a good time") -> {
                preferences.setInt("dinseyFunProgress", preferences.getInt("dinseyFunProgress", 0) + 1)
                true
            }
            html.contains("surrounding crowd seems to be pretty excited about the ride") -> {
                preferences.setInt("dinseyFunProgress", 15)
                questDatabase?.setProgress(Quest.ZIPPITY_DOO_DAH, "step2")
                true
            }
            else -> false
        }
    }

    private fun applySluice(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences,
    ): Boolean {
        if (html.contains("probably not unacceptably racist anymore")) {
            preferences.setInt("dinseySocialJusticeIIProgress", 15)
            questDatabase?.setProgress(Quest.SOCIAL_JUSTICE_II, "step1")
            return true
        }
        if (questDatabase != null &&
            questDatabase.isQuestLaterThan(Quest.SOCIAL_JUSTICE_II, QuestDatabase.UNSTARTED)
        ) {
            preferences.setInt(
                "dinseySocialJusticeIIProgress",
                preferences.getInt("dinseySocialJusticeIIProgress", 0) + 1,
            )
            return true
        }
        return false
    }
}
