package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestDatabase

class RufusManager(private val preferences: Preferences) {

    val questType: String
        get() = preferences.getString(Preferences.RUFUS_QUEST_TYPE, "entity").lowercase()

    /**
     * Choice 1498: maps the desired quest type to an option number.
     * Desktop option order: 1=entity, 2=artifact, 3=monument.
     * Returns null if the desired type's label is not found in [responseText].
     */
    fun chooseQuestOption(responseText: String): Int? {
        val desired = questType
        val optionLabels = listOf(
            "entity"   to 1,
            "artifact" to 2,
            "monument" to 3,
        )
        return optionLabels
            .firstOrNull { (label, _) ->
                label == desired && responseText.contains(label, ignoreCase = true)
            }
            ?.second
    }

    /** Choice 1499: always confirm (option 1). */
    fun confirmChoice(): Int = 1

    /** Store the target name after quest is accepted (extracted from post-choice response). */
    fun recordQuestTarget(target: String) {
        preferences.setString(Preferences.RUFUS_QUEST_TARGET, target)
    }

    /**
     * Parse Rufus quest-log detail text and update quest prefs.
     * Returns a quest step for [QuestDatabase.advance], or null when text is unrelated.
     */
    fun handleQuestLog(details: String, gameDatabase: GameDatabase? = null): String? {
        val text = details.trim()
        if (text.startsWith("Rufus wants you")) {
            RUFUS_ENTITY_PATTERN.find(text)?.let { match ->
                preferences.setString(Preferences.RUFUS_QUEST_TYPE, "entity")
                preferences.setString(Preferences.RUFUS_QUEST_TARGET, match.groupValues[1])
                return QuestDatabase.STARTED
            }
            RUFUS_ARTIFACT_PATTERN.find(text)?.let { match ->
                preferences.setString(Preferences.RUFUS_QUEST_TYPE, "artifact")
                preferences.setString(Preferences.RUFUS_QUEST_TARGET, match.groupValues[1])
                return QuestDatabase.STARTED
            }
            RUFUS_ITEMS_PATTERN.find(text)?.let { match ->
                preferences.setString(Preferences.RUFUS_QUEST_TYPE, "items")
                val items = match.groupValues[1]
                val itemName = resolveItemName(items, gameDatabase)
                preferences.setString(Preferences.RUFUS_QUEST_TARGET, itemName)
                return QuestDatabase.STARTED
            }
            return QuestDatabase.STARTED
        }
        if (text.startsWith("Call Rufus")) {
            if (RUFUS_ENTITY_DONE_PATTERN.containsMatchIn(text)) {
                preferences.setString(Preferences.RUFUS_QUEST_TYPE, "entity")
                return "step1"
            }
            RUFUS_ARTIFACT_DONE_PATTERN.find(text)?.let { match ->
                preferences.setString(Preferences.RUFUS_QUEST_TYPE, "artifact")
                preferences.setString(Preferences.RUFUS_QUEST_TARGET, match.groupValues[1])
                return "step1"
            }
            RUFUS_ITEMS_DONE_PATTERN.find(text)?.let { match ->
                preferences.setString(Preferences.RUFUS_QUEST_TYPE, "items")
                val items = match.groupValues[1]
                val itemName = resolveItemName(items, gameDatabase)
                preferences.setString(Preferences.RUFUS_QUEST_TARGET, itemName)
                return "step1"
            }
            return "step1"
        }
        return null
    }

    private fun resolveItemName(items: String, gameDatabase: GameDatabase?): String {
        gameDatabase?.item(items)?.name?.let { return it }
        ItemDatabase.getByPluralOrName(items)?.name?.let { return it }
        return items
    }

    companion object {
        private val RUFUS_ENTITY_PATTERN = Regex("""defeat a (.*?)\.""")
        private val RUFUS_ENTITY_DONE_PATTERN = Regex("""you defeated that monster\.""")
        private val RUFUS_ARTIFACT_PATTERN = Regex("""find a (.*?)\.""")
        private val RUFUS_ARTIFACT_DONE_PATTERN = Regex("""you found his (.*?)\.""")
        private val RUFUS_ITEMS_PATTERN = Regex("""find him 3 (.*?) from Shadow Rifts\.""")
        private val RUFUS_ITEMS_DONE_PATTERN = Regex("""you've got the 3 (.*?) he wanted\.""")
    }
}
