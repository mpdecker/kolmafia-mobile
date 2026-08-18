package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [QuestManager.handleTrapperChange] — trapper cabin ore/cheese/yeti finish.
 */
object TrapperCabinSync {

    const val LINOLEUM_ORE = 363
    const val ASBESTOS_ORE = 364
    const val CHROME_ORE = 365
    const val GOAT_CHEESE = 322
    const val GROARS_FUR = 5571
    const val WINGED_YETI_FUR = 8135

    private val orePattern = Regex("""(asbestos|linoleum|chrome) ore[\\. ]""", RegexOption.IGNORE_CASE)

    private val oreItemIds = mapOf(
        "asbestos ore" to ASBESTOS_ORE,
        "linoleum ore" to LINOLEUM_ORE,
        "chrome ore" to CHROME_ORE,
    )

    fun applyFromVisit(
        url: String?,
        html: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        ascensionNumber: Int = 0,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        val location = url.orEmpty()
        if (!location.contains("action=trappercabin", ignoreCase = true)) return false
        val ore = orePattern.find(html)
        if (ore != null) {
            preferences.setString("trapperOre", "${ore.groupValues[1].lowercase()} ore")
            questDatabase.setQuestIfBetter(Quest.TRAPPER, "step1")
            return true
        }
        if (html.contains("He takes the load of cheese and ore") ||
            html.contains("haul your load of ore and cheese")
        ) {
            oreItemIds[preferences.getString("trapperOre", "")]?.let { consumeItem(it, 3) }
            consumeItem(GOAT_CHEESE, 3)
            questDatabase.setQuestIfBetter(Quest.TRAPPER, "step2")
            return true
        }
        if (html.contains("Yeehaw!  I heard the noise")) {
            preferences.setInt("lastTr4pz0rQuest", ascensionNumber)
            consumeItem(GROARS_FUR, 1)
            questDatabase.setProgress(Quest.TRAPPER, QuestDatabase.FINISHED)
            return true
        }
        if (html.contains("drag the huge yeti pelt into his shack")) {
            preferences.setInt("lastTr4pz0rQuest", ascensionNumber)
            consumeItem(WINGED_YETI_FUR, 1)
            questDatabase.setProgress(Quest.TRAPPER, QuestDatabase.FINISHED)
            return true
        }
        return false
    }
}
