package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] Dinsey Employee Assignment Kiosk 1066 and rollercoaster 1073.
 */
object DinseyKioskChoiceSync {

    const val KIOSK = 1066
    const val ROLLERCOASTER = 1073
    const val MAINT_MISBEHAVIN = 1067
    const val GARBAGE_BAG = 8211
    const val TRASH_NET = 8245
    const val TOXIC_GLOBULE = 8218
    const val LUBE_SHOES = 8244
    const val DINSEY_REFRESHMENTS = 8243
    const val MASCOT_MASK = 8246

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        itemCount: (Int) -> Int = { 0 },
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        return when (choiceId) {
            KIOSK -> applyKiosk(html, questDatabase, preferences, itemCount, consumeItem)
            ROLLERCOASTER -> applyRollercoaster(decision, html, questDatabase, preferences)
            MAINT_MISBEHAVIN -> {
                if (preferences == null || !html.contains("throw a bag of garbage into it")) return false
                consumeItem(GARBAGE_BAG, 1)
                preferences.setBoolean("_dinseyGarbageDisposed", true)
                true
            }
            else -> false
        }
    }

    private fun applyKiosk(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        itemCount: (Int) -> Int,
        consumeItem: (Int, Int) -> Unit,
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        when {
            html.contains("Performance Review:  Sufficient") -> {
                consumeItem(TRASH_NET, 1)
                preferences.setInt("dinseyFilthLevel", 0)
                questDatabase.setProgress(Quest.FISH_TRASH, QuestDatabase.UNSTARTED)
            }
            html.contains("Performance Review:  Unobjectionable") -> {
                consumeItem(TOXIC_GLOBULE, 20)
                questDatabase.setProgress(Quest.GIVE_ME_FUEL, QuestDatabase.UNSTARTED)
            }
            html.contains("Performance Review:  Bearable") -> {
                preferences.setInt("dinseyNastyBearsDefeated", 0)
                questDatabase.setProgress(Quest.NASTY_BEARS, QuestDatabase.UNSTARTED)
            }
            html.contains("Performance Review:  Acceptable") -> {
                preferences.setInt("dinseySocialJusticeIProgress", 0)
                questDatabase.setProgress(Quest.SOCIAL_JUSTICE_I, QuestDatabase.UNSTARTED)
            }
            html.contains("Performance Review:  Fair") -> {
                preferences.setInt("dinseySocialJusticeIIProgress", 0)
                questDatabase.setProgress(Quest.SOCIAL_JUSTICE_II, QuestDatabase.UNSTARTED)
            }
            html.contains("Performance Review:  Average") -> {
                consumeItem(LUBE_SHOES, 1)
                questDatabase.setProgress(Quest.SUPER_LUBER, QuestDatabase.UNSTARTED)
            }
            html.contains("Performance Review:  Adequate") -> {
                preferences.setInt("dinseyTouristsFed", 0)
                consumeItem(DINSEY_REFRESHMENTS, 1)
                questDatabase.setProgress(Quest.WORK_WITH_FOOD, QuestDatabase.UNSTARTED)
            }
            html.contains("Performance Review:  Tolerable") -> {
                consumeItem(MASCOT_MASK, 1)
                preferences.setInt("dinseyFunProgress", 0)
                questDatabase.setProgress(Quest.ZIPPITY_DOO_DAH, QuestDatabase.UNSTARTED)
            }
            html.contains("weren't kidding about the power") -> {
                val step = if (itemCount(TOXIC_GLOBULE) >= 20) "step1" else QuestDatabase.STARTED
                questDatabase.setProgress(Quest.GIVE_ME_FUEL, step)
            }
            html.contains("anatomical diagram of a nasty bear") -> {
                questDatabase.setProgress(Quest.NASTY_BEARS, QuestDatabase.STARTED)
            }
            html.contains("lists all of the sexist aspects of the ride") -> {
                questDatabase.setProgress(Quest.SOCIAL_JUSTICE_I, QuestDatabase.STARTED)
            }
            html.contains("ideas are all themselves so racist") -> {
                questDatabase.setProgress(Quest.SOCIAL_JUSTICE_II, QuestDatabase.STARTED)
            }
            html.contains("box of snacks issues forth") -> {
                preferences.setInt("dinseyTouristsFed", 0)
                questDatabase.setProgress(Quest.WORK_WITH_FOOD, QuestDatabase.STARTED)
            }
            else -> return false
        }
        return true
    }

    private fun applyRollercoaster(
        decision: Int,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        var changed = false
        if (decision == 1) {
            preferences?.setBoolean("dinseyRollercoasterNext", false)
            changed = true
        }
        if (html.contains("lubricating every inch of the tracks")) {
            questDatabase?.setProgress(Quest.SUPER_LUBER, "step2")
            changed = true
        }
        return changed
    }
}
