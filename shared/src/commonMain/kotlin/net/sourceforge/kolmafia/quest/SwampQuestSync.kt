package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleCanadiaChange] + [QuestManager.handleMaraisChange].
 */
object SwampQuestSync {

    fun applyFromCanadia(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
    ): Boolean {
        if (questDatabase == null) return false
        val location = url.orEmpty()
        if (!location.contains("whichplace=canadia", ignoreCase = true) &&
            !location.contains("action=lc_marty", ignoreCase = true)
        ) {
            return false
        }
        if (!location.contains("action=lc_marty", ignoreCase = true)) return false
        if (!html.contains("All right, Marty, I'll see what I can do")) return false
        questDatabase.setProgress(Quest.SWAMP, QuestDatabase.STARTED)
        return true
    }

    fun applyFromMarais(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        if (url != null && !url.contains("whichplace=marais", ignoreCase = true)) return false
        var changed = false
        if (html.contains("The Edge of the Swamp")) {
            questDatabase.setQuestIfBetter(Quest.SWAMP, QuestDatabase.STARTED)
            changed = true
        }
        if (html.contains("The Dark and Spooky Swamp")) {
            preferences.setBoolean("maraisDarkUnlock", true)
            changed = true
        }
        if (html.contains("The Wildlife Sanctuarrrrrgh")) {
            preferences.setBoolean("maraisWildlifeUnlock", true)
            changed = true
        }
        if (html.contains("The Corpse Bog")) {
            preferences.setBoolean("maraisCorpseUnlock", true)
            changed = true
        }
        if (html.contains("The Ruined Wizard Tower")) {
            preferences.setBoolean("maraisWizardUnlock", true)
            changed = true
        }
        if (html.contains("Swamp Beaver Territory")) {
            preferences.setBoolean("maraisBeaverUnlock", true)
            changed = true
        }
        if (html.contains("The Weird Swamp Village")) {
            preferences.setBoolean("maraisVillageUnlock", true)
            changed = true
        }
        return changed
    }
}
