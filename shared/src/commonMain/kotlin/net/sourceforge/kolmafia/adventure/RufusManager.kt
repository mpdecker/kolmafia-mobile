package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.preferences.Preferences

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
}
