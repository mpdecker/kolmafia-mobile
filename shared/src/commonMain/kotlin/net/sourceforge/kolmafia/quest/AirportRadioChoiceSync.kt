package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] choice 984 Spooky Airport radio quest start/finish.
 */
object AirportRadioChoiceSync {

    const val CHOICE_ID = 984
    const val MINI_CASSETTE_RECORDER = 7832
    const val GORE_BUCKET = 7833
    const val FINGERNAIL_CLIPPERS = 7831
    const val ESP_COLLAR = 7835
    const val EXPERIMENTAL_SERUM_P00 = 7830
    const val GPS_WATCH = 7836
    const val PROJECT_TLB = 7837

    fun apply(
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
        itemCount: (Int) -> Int = { 0 },
    ): Boolean {
        if (questDatabase == null) return false
        var changed = false
        if (html.contains("your best paramilitary-sounding radio lingo")) {
            clearRepeatableRadioQuests(questDatabase)
            changed = true
        }
        if (html.contains("Maybe try again tomorrow")) {
            clearRepeatableRadioQuests(questDatabase)
            changed = true
        } else if (html.contains("navigation protocol")) {
            questDatabase.setProgress(Quest.EVE, QuestDatabase.STARTED)
            preferences?.setString("EVEDirections", "LLRLR0")
            changed = true
        } else if (html.contains("a tiny parachute")) {
            questDatabase.setProgress(Quest.EVE, QuestDatabase.FINISHED)
            preferences?.setString("EVEDirections", "LLRLR0")
            changed = true
        } else if (html.contains(
                "tape recorder self-destructs with a shower of sparks and a puff of smoke",
            )
        ) {
            consumeItem(MINI_CASSETTE_RECORDER, 1)
            questDatabase.setProgress(Quest.JUNGLE_PUN, QuestDatabase.UNSTARTED)
            preferences?.setInt("junglePuns", 0)
            changed = true
        } else if (html.contains("bucket came from")) {
            consumeItem(GORE_BUCKET, 1)
            questDatabase.setProgress(Quest.GORE, QuestDatabase.UNSTARTED)
            preferences?.setInt("goreCollected", 0)
            changed = true
        } else if (html.contains("return the fingernails and the clippers")) {
            consumeItem(FINGERNAIL_CLIPPERS, 1)
            questDatabase.setProgress(Quest.CLIPPER, QuestDatabase.UNSTARTED)
            preferences?.setInt("fingernailsClipped", 0)
            changed = true
        } else if (html.contains("maximal discretion")) {
            questDatabase.setProgress(Quest.FAKE_MEDIUM, QuestDatabase.STARTED)
            changed = true
        } else if (html.contains("toss the device into the ocean")) {
            consumeItem(ESP_COLLAR, 1)
            questDatabase.setProgress(Quest.FAKE_MEDIUM, QuestDatabase.FINISHED)
            changed = true
        } else if (html.contains("wonder how many vials they want")) {
            val step = if (itemCount(EXPERIMENTAL_SERUM_P00) >= 5) "step1" else QuestDatabase.STARTED
            questDatabase.setProgress(Quest.SERUM, step)
            changed = true
        } else if (html.contains("drop the vials into it")) {
            consumeItem(EXPERIMENTAL_SERUM_P00, 5)
            questDatabase.setProgress(Quest.SERUM, QuestDatabase.FINISHED)
            changed = true
        } else if (html.contains("acquire cigarettes")) {
            questDatabase.setProgress(Quest.SMOKES, QuestDatabase.STARTED)
            changed = true
        } else if (html.contains("cigarettes with a grappling gun")) {
            questDatabase.setProgress(Quest.SMOKES, QuestDatabase.FINISHED)
            changed = true
        } else if (html.contains("takes your nifty new watch")) {
            consumeItem(GPS_WATCH, 1)
            consumeItem(PROJECT_TLB, 1)
            questDatabase.setProgress(Quest.OUT_OF_ORDER, QuestDatabase.FINISHED)
            changed = true
        }
        return changed
    }

    private fun clearRepeatableRadioQuests(questDatabase: QuestDatabase) {
        questDatabase.setProgress(Quest.JUNGLE_PUN, QuestDatabase.UNSTARTED)
        questDatabase.setProgress(Quest.GORE, QuestDatabase.UNSTARTED)
        questDatabase.setProgress(Quest.CLIPPER, QuestDatabase.UNSTARTED)
    }
}
