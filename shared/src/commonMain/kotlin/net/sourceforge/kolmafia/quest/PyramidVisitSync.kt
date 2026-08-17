package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handlePyramidChange] place/adventure visit hooks for pyramid_state,
 * chamber image prefs, and bomb usage.
 */
object PyramidVisitSync {

    const val UPPER_CHAMBER = 406
    const val MIDDLE_CHAMBER = 407
    const val ANCIENT_BOMB = 2318

    private val LOWER_CHAMBER_PATTERN = Regex("""action=pyramid_state(\d+)""", RegexOption.IGNORE_CASE)

    data class PyramidVisitContext(
        val consumeItem: (Int, Int) -> Unit = { _, _ -> },
    )

    fun parseSnarfblat(url: String?): Int? =
        url?.let { Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE).find(it)?.groupValues?.getOrNull(1)?.toIntOrNull() }

    fun applyFromVisit(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        context: PyramidVisitContext = PyramidVisitContext(),
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        val location = url.orEmpty()
        val area = parseSnarfblat(url)
        val touchesPyramid =
            location.contains("whichplace=pyramid", ignoreCase = true) ||
                location.contains("action=db_pyramid1", ignoreCase = true) ||
                location.contains("action=expl_pyramidpre", ignoreCase = true) ||
                location.contains("action=pyramid_state", ignoreCase = true) ||
                area == UPPER_CHAMBER ||
                area == MIDDLE_CHAMBER
        if (!touchesPyramid) return false
        return applyPyramidChange(location, html, area, questDatabase, preferences, context)
    }

    fun applyPyramidChange(
        location: String,
        html: String,
        area: Int?,
        questDatabase: QuestDatabase,
        preferences: Preferences,
        context: PyramidVisitContext = PyramidVisitContext(),
    ): Boolean {
        var changed = false
        when {
            location.contains("action=db_pyramid1", ignoreCase = true) ||
                location.contains("action=expl_pyramidpre", ignoreCase = true) -> {
                if (html.contains("the model bursts into flames and is quickly consumed")) {
                    questDatabase.setProgress(Quest.PYRAMID, QuestDatabase.STARTED)
                    changed = true
                }
            }
            area == UPPER_CHAMBER -> {
                changed = applyUpperChamber(html, questDatabase, preferences) || changed
            }
            area == MIDDLE_CHAMBER -> {
                changed = applyMiddleChamber(html, questDatabase, preferences) || changed
            }
            location.contains("whichplace=pyramid", ignoreCase = true) &&
                !html.contains("No, that isn't a place yet.") -> {
                changed = applyPyramidPlace(location, html, questDatabase, preferences, context) || changed
            }
        }
        return changed
    }

    fun applyUpperChamber(
        html: String,
        questDatabase: QuestDatabase,
        preferences: Preferences,
    ): Boolean {
        if (!html.contains("Down Dooby-Doo Down Down")) return false
        preferences.setBoolean("middleChamberUnlock", true)
        questDatabase.setProgress(Quest.PYRAMID, "step1")
        return true
    }

    fun applyMiddleChamber(
        html: String,
        questDatabase: QuestDatabase,
        preferences: Preferences,
    ): Boolean {
        var changed = false
        if (html.contains("Further Down Dooby-Doo Down Down")) {
            preferences.setBoolean("lowerChamberUnlock", true)
            questDatabase.setProgress(Quest.PYRAMID, "step2")
            changed = true
        } else if (html.contains("Under Control")) {
            preferences.setBoolean("controlRoomUnlock", true)
            preferences.setInt("pyramidPosition", 1)
            questDatabase.setProgress(Quest.PYRAMID, "step3")
            changed = true
        } else if (html.contains("Don't You Know Who I Am?")) {
            questDatabase.setProgress(Quest.CLANCY, QuestDatabase.FINISHED)
            changed = true
        }
        preferences.setBoolean("middleChamberUnlock", true)
        questDatabase.setQuestIfBetter(Quest.PYRAMID, "step1")
        return true
    }

    fun applyPyramidPlace(
        location: String,
        html: String,
        questDatabase: QuestDatabase,
        preferences: Preferences,
        context: PyramidVisitContext = PyramidVisitContext(),
    ): Boolean {
        questDatabase.setQuestIfBetter(Quest.PYRAMID, QuestDatabase.STARTED)
        if (html.contains("pyramid_middle.gif")) {
            preferences.setBoolean("middleChamberUnlock", true)
            questDatabase.setQuestIfBetter(Quest.PYRAMID, "step1")
        }
        if (html.contains("pyramid_bottom")) {
            preferences.setBoolean("lowerChamberUnlock", true)
            questDatabase.setQuestIfBetter(Quest.PYRAMID, "step2")
        }
        if (html.contains("pyramid_controlroom.gif")) {
            preferences.setBoolean("controlRoomUnlock", true)
            questDatabase.setQuestIfBetter(Quest.PYRAMID, "step3")
        }
        LOWER_CHAMBER_PATTERN.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull()?.let { pos ->
            preferences.setInt("pyramidPosition", pos)
        }
        if (html.contains("action=pyramid_state1a")) {
            preferences.setBoolean("pyramidBombUsed", true)
        }
        if (location.contains("action=pyramid_state", ignoreCase = true) &&
            html.contains("the rubble is gone")
        ) {
            preferences.setBoolean("pyramidBombUsed", true)
            context.consumeItem(ANCIENT_BOMB, 1)
        }
        return true
    }
}
