package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleSeaChange] — Sea Old Guy / Sea Monkees visit + adventure writers.
 */
object SeaVisitSync {

    const val MERKIN_OUTPOST = 198
    const val CALIGINOUS_ABYSS = 337

    private val seahorsePattern =
        Regex("""atop your trusty seahorse <b>(.*?)</b>""", RegexOption.IGNORE_CASE)

    fun applyFromVisit(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences? = null,
        isMuscleClass: Boolean = false,
        isMysticalityClass: Boolean = false,
        isMoxieClass: Boolean = false,
        includeMaps: Boolean = true,
    ): Boolean {
        if (questDatabase == null) return false
        val location = url.orEmpty()
        val area = Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE)
            .find(location)?.groupValues?.getOrNull(1)?.toIntOrNull()
        return apply(
            location = location,
            html = html,
            area = area,
            questDatabase = questDatabase,
            preferences = preferences,
            isMuscleClass = isMuscleClass,
            isMysticalityClass = isMysticalityClass,
            isMoxieClass = isMoxieClass,
            includeMaps = includeMaps,
        )
    }

    fun applyFromAdventure(
        adventureId: String?,
        html: String,
        questDatabase: QuestDatabase?,
        url: String? = null,
    ): Boolean {
        if (questDatabase == null) return false
        val area = adventureId?.toIntOrNull()
            ?: Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE)
                .find(url.orEmpty())?.groupValues?.getOrNull(1)?.toIntOrNull()
            ?: return false
        return apply(
            location = url.orEmpty(),
            html = html,
            area = area,
            questDatabase = questDatabase,
            preferences = null,
        )
    }

    internal fun apply(
        location: String,
        html: String,
        area: Int?,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
        isMuscleClass: Boolean = false,
        isMysticalityClass: Boolean = false,
        isMoxieClass: Boolean = false,
        includeMaps: Boolean = false,
    ): Boolean {
        if (location.contains("action=oldman_oldman", ignoreCase = true)) {
            return when {
                html.contains("I lost my favorite boot, you see.") ||
                    html.contains("have you found my boot yet?") -> {
                    questDatabase.setProgress(Quest.SEA_OLD_GUY, QuestDatabase.STARTED)
                    true
                }
                html.contains("The old man snores fitfully") -> {
                    questDatabase.setProgress(Quest.SEA_OLD_GUY, QuestDatabase.FINISHED)
                    true
                }
                else -> false
            }
        }
        if (location.contains("who=1")) {
            return when {
                html.contains("wish my big brother was here") -> {
                    questDatabase.setProgress(Quest.SEA_MONKEES, "step1")
                    true
                }
                html.contains("Wanna help me find Grandpa?") -> {
                    questDatabase.setProgress(Quest.SEA_MONKEES, "step4")
                    true
                }
                html.contains("he's been actin' awful weird lately") -> {
                    questDatabase.setProgress(Quest.SEA_MONKEES, "step10")
                    true
                }
                else -> false
            }
        }
        if (location.contains("who=2")) {
            if (html.contains("I found this thing")) {
                questDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step11")
                return true
            }
            return false
        }
        if (location.contains("action=grandpastory", ignoreCase = true)) {
            return when {
                html.contains("bet those lousy Mer-kin up and kidnapped her") -> {
                    questDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step6")
                    true
                }
                html.contains("that note's definitely Grandma Sea Monkee's handwriting") -> {
                    questDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step7")
                    true
                }
                html.contains("Gonna need one of them seahorses") -> {
                    preferences?.setBoolean("corralUnlocked", true)
                    preferences != null
                }
                else -> false
            }
        }
        if (area == MERKIN_OUTPOST && html.contains("Phew, that was a close one")) {
            questDatabase.setProgress(Quest.SEA_MONKEES, "step9")
            return true
        }
        if (area == CALIGINOUS_ABYSS &&
            html.contains("I should get dinner on the table for the boys")
        ) {
            questDatabase.setProgress(Quest.SEA_MONKEES, QuestDatabase.FINISHED)
            return true
        }
        if (!includeMaps) return false
        return applyMaps(
            location = location,
            html = html,
            questDatabase = questDatabase,
            preferences = preferences,
            isMuscleClass = isMuscleClass,
            isMysticalityClass = isMysticalityClass,
            isMoxieClass = isMoxieClass,
        )
    }

    internal fun applyMaps(
        location: String,
        html: String,
        questDatabase: QuestDatabase,
        preferences: Preferences?,
        isMuscleClass: Boolean,
        isMysticalityClass: Boolean,
        isMoxieClass: Boolean,
    ): Boolean {
        var changed = false
        if (location.startsWith("seafloor") || location.contains("seafloor.php", ignoreCase = true)) {
            when {
                html.contains("abyss") -> {
                    questDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step12")
                    changed = true
                }
                html.contains("outpost") -> {
                    questDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step6")
                    changed = true
                }
                html.contains("shipwreck") -> {
                    questDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step1")
                    changed = true
                }
                html.contains("monkeycastle") -> {
                    questDatabase.setQuestIfBetter(Quest.SEA_MONKEES, QuestDatabase.STARTED)
                    changed = true
                }
            }
            if (preferences != null) {
                if (html.contains("mine") &&
                    (!questDatabase.isAtLeast(Quest.SEA_MONKEES, "step4") || !isMuscleClass)
                ) {
                    preferences.setBoolean("mapToAnemoneMinePurchased", true)
                    changed = true
                }
                if (html.contains("trench") &&
                    (!questDatabase.isAtLeast(Quest.SEA_MONKEES, "step4") || !isMysticalityClass)
                ) {
                    preferences.setBoolean("mapToTheMarinaraTrenchPurchased", true)
                    changed = true
                }
                if (html.contains("divebar") &&
                    (!questDatabase.isAtLeast(Quest.SEA_MONKEES, "step4") || !isMoxieClass)
                ) {
                    preferences.setBoolean("mapToTheDiveBarPurchased", true)
                    changed = true
                }
                if (html.contains("reef")) {
                    preferences.setBoolean("mapToMadnessReefPurchased", true)
                    changed = true
                }
                if (html.contains("skatepark")) {
                    preferences.setBoolean("mapToTheSkateParkPurchased", true)
                    changed = true
                }
                if (html.contains("currents")) {
                    preferences.setBoolean("intenseCurrents", true)
                    changed = true
                }
                if (html.contains("corral")) {
                    preferences.setBoolean("corralUnlocked", true)
                    changed = true
                }
            }
            return changed
        }
        if (location.startsWith("monkeycastle") ||
            location.contains("monkeycastle.php", ignoreCase = true)
        ) {
            when {
                html.contains("who=4") -> {
                    questDatabase.setQuestIfBetter(Quest.SEA_MONKEES, QuestDatabase.FINISHED)
                    changed = true
                }
                html.contains("whichshop=grandma") -> {
                    questDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step9")
                    changed = true
                }
                html.contains("who=3") -> {
                    questDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step5")
                    changed = true
                }
                html.contains("who=2") -> {
                    questDatabase.setQuestIfBetter(Quest.SEA_MONKEES, "step2")
                    preferences?.setBoolean("bigBrotherRescued", true)
                    changed = true
                }
                html.contains("who=1") -> {
                    questDatabase.setQuestIfBetter(Quest.SEA_MONKEES, QuestDatabase.STARTED)
                    changed = true
                }
            }
            return changed
        }
        if (location.startsWith("sea_merkin") ||
            location.contains("sea_merkin.php", ignoreCase = true)
        ) {
            val name = seahorsePattern.find(html)?.groupValues?.getOrNull(1) ?: return false
            if (preferences == null) return false
            preferences.setString("seahorseName", name)
            return true
        }
        return false
    }
}
