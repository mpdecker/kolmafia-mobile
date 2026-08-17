package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ResultProcessor] Hidden City stone-sphere / ancient-amulet progress glue.
 * Complements [QuestItemRules] subquest FINISHED / WORSHIP step4 writers with progress prefs.
 */
object HiddenCityItemSync {

    const val MOSS_COVERED_STONE_SPHERE = 6697
    const val DRIPPING_STONE_SPHERE = 6698
    const val CRACKLING_STONE_SPHERE = 6699
    const val SCORCHED_STONE_SPHERE = 6700
    const val ANCIENT_AMULET = 2180
    const val MCCLUSKY_FILE = 6689
    const val STONE_TRIANGLE = 7041

    fun applyItemAcquire(
        itemId: Int,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (questDatabase == null || preferences == null) return false
        return when (itemId) {
            MOSS_COVERED_STONE_SPHERE -> {
                preferences.setInt("hiddenApartmentProgress", 7)
                questDatabase.setProgress(Quest.CURSES, QuestDatabase.FINISHED)
                maybeWorshipStep4(questDatabase)
                true
            }
            DRIPPING_STONE_SPHERE -> {
                preferences.setInt("hiddenHospitalProgress", 7)
                questDatabase.setProgress(Quest.DOCTOR, QuestDatabase.FINISHED)
                maybeWorshipStep4(questDatabase)
                true
            }
            CRACKLING_STONE_SPHERE -> {
                consumeItem(MCCLUSKY_FILE, 1)
                preferences.setInt("hiddenOfficeProgress", 7)
                questDatabase.setProgress(Quest.BUSINESS, QuestDatabase.FINISHED)
                maybeWorshipStep4(questDatabase)
                true
            }
            SCORCHED_STONE_SPHERE -> {
                preferences.setInt("hiddenBowlingAlleyProgress", 7)
                questDatabase.setProgress(Quest.SPARE, QuestDatabase.FINISHED)
                maybeWorshipStep4(questDatabase)
                true
            }
            ANCIENT_AMULET -> {
                consumeItem(STONE_TRIANGLE, 4)
                preferences.setInt("hiddenApartmentProgress", 8)
                preferences.setInt("hiddenHospitalProgress", 8)
                preferences.setInt("hiddenOfficeProgress", 8)
                preferences.setInt("hiddenBowlingAlleyProgress", 8)
                questDatabase.setProgress(Quest.WORSHIP, QuestDatabase.FINISHED)
                true
            }
            else -> false
        }
    }

    fun applyItemName(
        name: String,
        questDatabase: QuestDatabase?,
        preferences: Preferences?,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        val lower = name.lowercase()
        val id = when {
            lower.contains("moss-covered stone sphere") -> MOSS_COVERED_STONE_SPHERE
            lower.contains("dripping stone sphere") -> DRIPPING_STONE_SPHERE
            lower.contains("crackling stone sphere") -> CRACKLING_STONE_SPHERE
            lower.contains("scorched stone sphere") -> SCORCHED_STONE_SPHERE
            lower.contains("ancient amulet") -> ANCIENT_AMULET
            else -> return false
        }
        return applyItemAcquire(id, questDatabase, preferences, consumeItem)
    }

    private fun maybeWorshipStep4(questDatabase: QuestDatabase) {
        if (questDatabase.isQuestFinished(Quest.CURSES) &&
            questDatabase.isQuestFinished(Quest.DOCTOR) &&
            questDatabase.isQuestFinished(Quest.BUSINESS) &&
            questDatabase.isQuestFinished(Quest.SPARE)
        ) {
            questDatabase.setProgress(Quest.WORSHIP, "step4")
        }
    }
}
