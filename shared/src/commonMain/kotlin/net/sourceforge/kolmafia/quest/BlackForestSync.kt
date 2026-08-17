package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager] BLACK_FOREST combat + woods blackmarket visit writers.
 */
object BlackForestSync {

    const val BLACK_FOREST = 405

    fun applyCombatWin(
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        adventureId: String,
        responseText: String,
        won: Boolean,
    ): Boolean {
        if (questDatabase == null || preferences == null || !won) return false
        if (adventureId != BLACK_FOREST.toString()) return false
        return applyBlackForestText(questDatabase, preferences, responseText)
    }

    fun applyBlackForestText(
        questDatabase: QuestDatabase,
        preferences: Preferences,
        responseText: String,
    ): Boolean {
        if (responseText.contains("discover the trail leading to the Black Market")) {
            questDatabase.setProgress(Quest.MACGUFFIN, "step1")
            questDatabase.setProgress(Quest.BLACK, "step2")
            preferences.setInt("blackForestProgress", 5)
            return true
        }
        var progress = 0
        when {
            responseText.contains("find a row of blackberry bushes so thick") -> progress = 1
            responseText.contains("find a cozy black cottage nestled deep") -> progress = 2
            responseText.contains("spot a mineshaft sunk deep into the black depths") -> progress = 3
            responseText.contains("find a church that would be picturesque if it wasn't so sinister") -> progress = 4
        }
        if (progress > 0) {
            preferences.setInt("blackForestProgress", progress)
        }
        questDatabase.setQuestIfBetter(Quest.BLACK, "step1")
        return true
    }

    /** Desktop ItemPool IDs consumed on Hidden Temple unlock via woods_dakota. */
    const val BENDY_STRAW = 7398
    const val PLANT_FOOD = 7399
    const val SEWING_KIT = 7300

    fun applyWoodsVisit(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        ascensionNumber: Int = 0,
        consumeItem: (Int) -> Unit = {},
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        val location = url.orEmpty()
        if (!location.contains("woods.php", ignoreCase = true) &&
            !location.contains("whichplace=woods", ignoreCase = true)
        ) {
            return false
        }
        var changed = false
        if (html.contains("wcroad.gif")) {
            questDatabase.setQuestIfBetter(Quest.CITADEL, "step1")
            changed = true
        }
        if (location.contains("action=woods_dakota", ignoreCase = true)) {
            if (html.contains("need you to pick up a couple things for me")) {
                questDatabase.setProgress(Quest.TEMPLE, QuestDatabase.STARTED)
                changed = true
            } else if (html.contains("make a note of the temple's location")) {
                if (preferences.getInt("lastTempleUnlock", -1) != ascensionNumber) {
                    preferences.setInt("lastTempleUnlock", ascensionNumber)
                    changed = true
                }
                consumeItem(BENDY_STRAW)
                consumeItem(PLANT_FOOD)
                consumeItem(SEWING_KIT)
                changed = true
            }
        } else if (location.contains("action=woods_hippy", ignoreCase = true) &&
            html.contains("You've got this cool boat")
        ) {
            questDatabase.setProgress(Quest.HIPPY, QuestDatabase.FINISHED)
            changed = true
        }
        if (html.contains("blackmarket.gif")) {
            questDatabase.setQuestIfBetter(Quest.BLACK, "step2")
            questDatabase.setQuestIfBetter(Quest.MACGUFFIN, "step1")
            preferences.setInt("blackForestProgress", 5)
            changed = true
        }
        if (html.contains("action=emptybm")) {
            preferences.setInt("lastWuTangDefeated", ascensionNumber)
            changed = true
        }
        if (html.contains("temple.gif")) {
            if (preferences.getInt("lastTempleUnlock", -1) != ascensionNumber) {
                preferences.setInt("lastTempleUnlock", ascensionNumber)
                changed = true
            }
        }
        return changed
    }
}
