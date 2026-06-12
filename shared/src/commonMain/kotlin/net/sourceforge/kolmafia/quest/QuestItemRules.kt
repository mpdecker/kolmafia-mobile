package net.sourceforge.kolmafia.quest

/** Quest step bumps from item acquisition and inventory counts. */
object QuestItemRules {

    val LEGENDARY_WEAPON_IDS = setOf(2556, 2557, 2558, 2559, 2560, 2561)
    val LEGENDARY_PANTS_IDS = setOf(4267, 4268, 4269, 4270, 4271, 4272)
    const val FIZZING_SPORE_POD_ID = 8427
    const val SCALP_OF_GORGOLOK_ID = 150
    const val BELT_BUCKLE_OF_LOPEZ_ID = 4327
    const val NO_HANDED_PIE_ID = 8201
    const val POPULAR_PART_ID = 8383
    const val BIG_KNOB_SAUSAGE_ID = 5193
    const val EXORCISED_SANDWICH_ID = 5194
    const val FRAUDWORT_ID = 1670
    const val SHYSTERWEED_ID = 1671
    const val SWINDLEBLOSSOM_ID = 1672
    const val MOSS_COVERED_STONE_SPHERE_ID = 6697
    const val DRIPPING_STONE_SPHERE_ID = 6698
    const val CRACKLING_STONE_SPHERE_ID = 6699
    const val SCORCHED_STONE_SPHERE_ID = 6700
    const val DOC_HERB_COUNT = 3
    const val SPORE_POD_COUNT = 6

    fun applyItemsGained(itemsGained: List<String>, questDatabase: QuestDatabase): Boolean {
        var advanced = false
        for (name in itemsGained) {
            if (isLegendaryWeaponName(name)) {
                advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step8") || advanced
            }
            if (name.contains("Scalp of Gorgolok", ignoreCase = true)) {
                advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step16") || advanced
            }
            if (name.contains("volcano map", ignoreCase = true)) {
                advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step25") || advanced
            }
            if (isLegendaryPantsName(name)) {
                advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step27") || advanced
            }
            if (name.contains("Belt Buckle of Lopez", ignoreCase = true) ||
                name.contains("Hebilla del Cinturón", ignoreCase = true)
            ) {
                advanced = setIfBetter(questDatabase, Quest.NEMESIS, QuestDatabase.FINISHED) || advanced
            }
            if (name.contains("no-handed pie", ignoreCase = true)) {
                advanced = setIfBetter(questDatabase, Quest.ARMORER, "step4") || advanced
            }
            if (name.contains("popular part", ignoreCase = true)) {
                advanced = setIfBetter(questDatabase, Quest.ARMORER, QuestDatabase.FINISHED) || advanced
            }
            if (name.contains("big knob sausage", ignoreCase = true)) {
                advanced = setIfBetter(questDatabase, Quest.MUSCLE, "step1") || advanced
            }
            if (name.contains("exorcised sandwich", ignoreCase = true)) {
                advanced = setIfBetter(questDatabase, Quest.MYST, "step1") || advanced
            }
            advanced = applyHiddenHospitalItemName(name, questDatabase) || advanced
        }
        advanced = maybeAdvanceWorshipStep4(questDatabase) || advanced
        return advanced
    }

    fun applyInventory(itemCount: (Int) -> Int, questDatabase: QuestDatabase): Boolean {
        var advanced = false
        for (weaponId in LEGENDARY_WEAPON_IDS) {
            if (itemCount(weaponId) >= 1) {
                advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step8") || advanced
                break
            }
        }
        if (itemCount(FIZZING_SPORE_POD_ID) >= SPORE_POD_COUNT) {
            advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step14") || advanced
        }
        if (itemCount(SCALP_OF_GORGOLOK_ID) >= 1) {
            advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step16") || advanced
        }
        if (itemCount(QuestFightRules.VOLCANO_MAP_ID) >= 1) {
            advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step25") || advanced
        }
        for (pantsId in LEGENDARY_PANTS_IDS) {
            if (itemCount(pantsId) >= 1) {
                advanced = setIfBetter(questDatabase, Quest.NEMESIS, "step27") || advanced
                break
            }
        }
        if (itemCount(BELT_BUCKLE_OF_LOPEZ_ID) >= 1) {
            advanced = setIfBetter(questDatabase, Quest.NEMESIS, QuestDatabase.FINISHED) || advanced
        }
        if (itemCount(NO_HANDED_PIE_ID) >= 1) {
            advanced = setIfBetter(questDatabase, Quest.ARMORER, "step4") || advanced
        }
        if (itemCount(POPULAR_PART_ID) >= 1) {
            advanced = setIfBetter(questDatabase, Quest.ARMORER, QuestDatabase.FINISHED) || advanced
        }
        if (itemCount(BIG_KNOB_SAUSAGE_ID) >= 1) {
            advanced = setIfBetter(questDatabase, Quest.MUSCLE, "step1") || advanced
        }
        if (itemCount(EXORCISED_SANDWICH_ID) >= 1) {
            advanced = setIfBetter(questDatabase, Quest.MYST, "step1") || advanced
        }
        if (itemCount(FRAUDWORT_ID) >= DOC_HERB_COUNT &&
            itemCount(SHYSTERWEED_ID) >= DOC_HERB_COUNT &&
            itemCount(SWINDLEBLOSSOM_ID) >= DOC_HERB_COUNT
        ) {
            advanced = setIfBetter(questDatabase, Quest.DOC, "step1") || advanced
        }
        if (itemCount(MOSS_COVERED_STONE_SPHERE_ID) >= 1) {
            advanced = setIfBetter(questDatabase, Quest.CURSES, QuestDatabase.FINISHED) || advanced
        }
        if (itemCount(DRIPPING_STONE_SPHERE_ID) >= 1) {
            advanced = setIfBetter(questDatabase, Quest.DOCTOR, QuestDatabase.FINISHED) || advanced
        }
        if (itemCount(CRACKLING_STONE_SPHERE_ID) >= 1) {
            advanced = setIfBetter(questDatabase, Quest.BUSINESS, QuestDatabase.FINISHED) || advanced
        }
        if (itemCount(SCORCHED_STONE_SPHERE_ID) >= 1) {
            advanced = setIfBetter(questDatabase, Quest.SPARE, QuestDatabase.FINISHED) || advanced
        }
        advanced = maybeAdvanceWorshipStep4(questDatabase) || advanced
        return advanced
    }

    private fun applyHiddenHospitalItemName(name: String, questDatabase: QuestDatabase): Boolean {
        val lower = name.lowercase()
        return when {
            lower.contains("moss-covered stone sphere") ->
                setIfBetter(questDatabase, Quest.CURSES, QuestDatabase.FINISHED)
            lower.contains("dripping stone sphere") ->
                setIfBetter(questDatabase, Quest.DOCTOR, QuestDatabase.FINISHED)
            lower.contains("crackling stone sphere") ->
                setIfBetter(questDatabase, Quest.BUSINESS, QuestDatabase.FINISHED)
            lower.contains("scorched stone sphere") ->
                setIfBetter(questDatabase, Quest.SPARE, QuestDatabase.FINISHED)
            else -> false
        }
    }

    private fun maybeAdvanceWorshipStep4(questDatabase: QuestDatabase): Boolean {
        if (!questDatabase.isQuestFinished(Quest.CURSES) ||
            !questDatabase.isQuestFinished(Quest.DOCTOR) ||
            !questDatabase.isQuestFinished(Quest.BUSINESS) ||
            !questDatabase.isQuestFinished(Quest.SPARE)
        ) {
            return false
        }
        return setIfBetter(questDatabase, Quest.WORSHIP, "step4")
    }

    private fun isLegendaryWeaponName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("hammer of smiting") ||
            lower.contains("shagadelic") ||
            lower.contains("squeezebox of the ages") ||
            lower.contains("chelonian morningstar") ||
            lower.contains("17-alarm saucepan") ||
            lower.contains("greek pasta spoon")
    }

    private fun isLegendaryPantsName(name: String): Boolean {
        val lower = name.lowercase()
        return lower.contains("loincloth") ||
            lower.contains("cuisses") ||
            lower.contains("culottes") ||
            lower.contains("trousers") ||
            lower.contains("bellbottoms") ||
            lower.contains("lederhosen")
    }

    private fun setIfBetter(db: QuestDatabase, quest: Quest, step: String): Boolean {
        val current = db.getProgress(quest)
        if (QuestDatabase.stepOrdinal(step) > QuestDatabase.stepOrdinal(current)) {
            db.setProgress(quest, step)
            return true
        }
        return false
    }
}
