package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleManorFirstFloorChange] / [QuestManager.handleManorSecondFloorChange]
 * / manor3 / manor4 place visit hooks.
 */
object SpookyravenManorVisitSync {

    const val HAUNTED_KITCHEN = 388
    const val HAUNTED_BILLIARDS_ROOM = 391
    const val HAUNTED_BATHROOM = 392
    const val HAUNTED_BALLROOM = 395

    const val POWDER_PUFF = 7305
    const val FINEST_GOWN = 7306
    const val DANCING_SHOES = 7307
    const val ED_EYE = 7962
    const val ED_AMULET = 7963
    const val ED_FATS_STAFF = 7964

    data class ManorVisitContext(
        val ascensionNumber: Int = 0,
        val hasItemId: (Int) -> Boolean = { false },
        val consumeItem: (Int, Int) -> Unit = { _, _ -> },
    )

    fun parseSnarfblat(url: String?): Int? =
        url?.let { Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE).find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }

    fun applyFromVisit(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        context: ManorVisitContext = ManorVisitContext(),
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        val location = url.orEmpty()
        val area = parseSnarfblat(url)
        var changed = false

        if (area == HAUNTED_BILLIARDS_ROOM || html.contains("That's Your Cue")) {
            if (html.contains("That's Your Cue")) {
                questDatabase.setProgress(Quest.SPOOKYRAVEN_NECKLACE, "step2")
                changed = true
            }
        }

        val place = Regex("""whichplace=([a-zA-Z0-9_]+)""", RegexOption.IGNORE_CASE)
            .find(location)?.groupValues?.getOrNull(1)?.lowercase()
        val isLegacyManor = location.contains("manor", ignoreCase = true) &&
            !location.contains("whichplace=", ignoreCase = true)

        when {
            place == "manor1" || isLegacyManor ->
                changed = applyFirstFloor(location, html, questDatabase, preferences, context) || changed
            place == "manor2" || area == HAUNTED_BALLROOM ->
                changed = applySecondFloor(location, html, area, questDatabase, preferences, context) || changed
            place == "manor3" ->
                changed = applyThirdFloor(html, questDatabase, preferences, context) || changed
            place == "manor4" ->
                changed = applyCellar(html, questDatabase, preferences, context) || changed
        }
        return changed
    }

    fun applyFirstFloor(
        location: String,
        html: String,
        questDatabase: QuestDatabase,
        preferences: Preferences,
        context: ManorVisitContext = ManorVisitContext(),
    ): Boolean {
        var changed = false
        if (location.contains("action=manor1_ladys", ignoreCase = true) &&
            html.contains("ghostly copy of the necklace")
        ) {
            questDatabase.setProgress(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED)
            changed = true
        }
        if (html.contains("snarfblat=$HAUNTED_KITCHEN")) {
            questDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.STARTED)
            changed = true
        }
        if (html.contains("whichplace=manor2")) {
            questDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED)
            preferences.setInt("lastSecondFloorUnlock", context.ascensionNumber)
            changed = true
        }
        if (html.contains("whichplace=manor4")) {
            questDatabase.setQuestIfBetter(Quest.MANOR, "step1")
            changed = true
        }
        return changed
    }

    fun applySecondFloor(
        location: String,
        html: String,
        area: Int?,
        questDatabase: QuestDatabase,
        preferences: Preferences,
        context: ManorVisitContext = ManorVisitContext(),
    ): Boolean {
        // Desktop gates on the second-floor title; ballroom NC may still carry Having a Ball.
        val onSecondFloor = html.contains("Spookyraven Manor Second Floor")
        if (!onSecondFloor && area != HAUNTED_BALLROOM) return false
        var changed = false
        if (onSecondFloor || location.contains("action=manor2_ladys", ignoreCase = true)) {
            if (location.contains("action=manor2_ladys", ignoreCase = true)) {
                if (html.contains("just want to dance")) {
                    questDatabase.setProgress(Quest.SPOOKYRAVEN_DANCE, "step1")
                    changed = true
                }
                if (html.contains("Meet me in the ballroom")) {
                    questDatabase.setProgress(Quest.SPOOKYRAVEN_DANCE, "step3")
                    context.consumeItem(POWDER_PUFF, 1)
                    context.consumeItem(FINEST_GOWN, 1)
                    context.consumeItem(DANCING_SHOES, 1)
                    changed = true
                }
            }
            if (html.contains("snarfblat=$HAUNTED_BATHROOM")) {
                questDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_DANCE, "step1")
                changed = true
            }
            if (html.contains("snarfblat=$HAUNTED_BALLROOM")) {
                questDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_DANCE, "step3")
                changed = true
            }
            if (html.contains("whichplace=manor3")) {
                questDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED)
                changed = true
            }
            if (onSecondFloor) {
                questDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED)
                preferences.setInt("lastSecondFloorUnlock", context.ascensionNumber)
                changed = true
            }
        }
        if (area == HAUNTED_BALLROOM && html.contains("Having a Ball in the Ballroom")) {
            questDatabase.setProgress(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED)
            changed = true
        }
        return changed
    }

    fun applyThirdFloor(
        html: String,
        questDatabase: QuestDatabase,
        preferences: Preferences,
        context: ManorVisitContext = ManorVisitContext(),
    ): Boolean {
        if (!html.contains("Spookyraven Manor Third Floor")) return false
        questDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED)
        questDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED)
        preferences.setInt("lastSecondFloorUnlock", context.ascensionNumber)
        return true
    }

    fun applyCellar(
        html: String,
        questDatabase: QuestDatabase,
        preferences: Preferences,
        context: ManorVisitContext = ManorVisitContext(),
    ): Boolean {
        var changed = false
        if (html.contains("Spookyraven Manor Cellar")) {
            questDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_NECKLACE, QuestDatabase.FINISHED)
            questDatabase.setQuestIfBetter(Quest.SPOOKYRAVEN_DANCE, QuestDatabase.FINISHED)
            changed = true
        }
        if (html.contains("sr_brickhole.gif")) {
            questDatabase.setQuestIfBetter(Quest.MANOR, "step3")
            changed = true
        } else if (!html.contains("You shouldn't be down here yet.  I mean here.  Wherever here is.")) {
            questDatabase.setQuestIfBetter(Quest.MANOR, "step1")
            changed = true
        }
        if (!html.contains("You shouldn't be down here yet.  I mean here.  Wherever here is.")) {
            preferences.setInt("lastSecondFloorUnlock", context.ascensionNumber)
            changed = true
        }
        if (html.contains("Cold as ice and twice as smooth")) {
            questDatabase.setProgress(Quest.MANOR, QuestDatabase.FINISHED)
            context.consumeItem(ED_EYE, 1)
            if (!context.hasItemId(ED_FATS_STAFF) && !context.hasItemId(ED_AMULET)) {
                questDatabase.setProgress(Quest.MACGUFFIN, QuestDatabase.FINISHED)
            }
            changed = true
        }
        return changed
    }
}
