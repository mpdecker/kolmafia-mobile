package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleChasmChange] + [QuestManager.handleHighlandsChange].
 */
object ToppingPlaceSync {

    const val MORNINGWOOD_PLANK = 5782
    const val HARDWOOD_PLANK = 5783
    const val WEIRDWOOD_PLANK = 5784
    const val THICK_CAULK = 5785
    const val LONG_SCREW = 5786
    const val BUTT_JOINT = 5787

    fun applyFromChasm(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        itemCount: (Int) -> Int = { 0 },
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (questDatabase == null) return false
        if (url != null && !url.contains("whichplace=orc_chasm", ignoreCase = true) &&
            !url.contains("orc_chasm", ignoreCase = true)
        ) {
            return false
        }
        if (!html.contains("Huzzah!  The bridge is finished!") &&
            !html.contains("deploy your handy-dandy portable bridge")
        ) {
            return false
        }
        consumeAll(MORNINGWOOD_PLANK, itemCount, consumeItem)
        consumeAll(HARDWOOD_PLANK, itemCount, consumeItem)
        consumeAll(WEIRDWOOD_PLANK, itemCount, consumeItem)
        consumeAll(THICK_CAULK, itemCount, consumeItem)
        consumeAll(LONG_SCREW, itemCount, consumeItem)
        consumeAll(BUTT_JOINT, itemCount, consumeItem)
        questDatabase.setQuestIfBetter(Quest.TOPPING, "step1")
        return true
    }

    fun applyFromHighlands(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        val location = url.orEmpty()
        if (!location.contains("whichplace=highlands", ignoreCase = true) &&
            !location.contains("highlands", ignoreCase = true) &&
            !html.contains("orcchasm/fire")
        ) {
            return false
        }
        var changed = false
        if (location.contains("action=highlands_dude", ignoreCase = true)) {
            if (html.contains("trying to, like, order a pizza") ||
                html.contains("trying to order a pizza")
            ) {
                questDatabase.setProgress(Quest.TOPPING, "step2")
                changed = true
            } else if (html.contains("you're the one who totally lit all those fires")) {
                questDatabase.setProgress(Quest.TOPPING, QuestDatabase.FINISHED)
                changed = true
            }
        }
        if (html.contains("orcchasm/fire1.gif")) {
            preferences.setBoolean("booPeakLit", true)
            preferences.setInt("booPeakProgress", 0)
            changed = true
        }
        if (html.contains("orcchasm/fire2.gif")) {
            preferences.setInt("twinPeakProgress", 15)
            changed = true
        }
        if (html.contains("orcchasm/fire3.gif")) {
            preferences.setBoolean("oilPeakLit", true)
            preferences.setString("oilPeakProgress", "0")
            changed = true
        }
        if (preferences.getBoolean("booPeakLit", false) &&
            preferences.getInt("twinPeakProgress", 0) == 15 &&
            preferences.getBoolean("oilPeakLit", false)
        ) {
            questDatabase.setQuestIfBetter(Quest.TOPPING, "step3")
            changed = true
        }
        return changed
    }

    private fun consumeAll(
        itemId: Int,
        itemCount: (Int) -> Int,
        consumeItem: (Int, Int) -> Unit,
    ) {
        val count = itemCount(itemId)
        if (count > 0) consumeItem(itemId, count)
    }
}
