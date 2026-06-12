package net.sourceforge.kolmafia.quest

/** Quest step bumps from item acquisition and inventory counts. */
object QuestItemRules {

    val LEGENDARY_WEAPON_IDS = setOf(2556, 2557, 2558, 2559, 2560, 2561)
    const val FIZZING_SPORE_POD_ID = 8427
    const val SCALP_OF_GORGOLOK_ID = 150
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
        }
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
        return advanced
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

    private fun setIfBetter(db: QuestDatabase, quest: Quest, step: String): Boolean {
        val current = db.getProgress(quest)
        if (QuestDatabase.stepOrdinal(step) > QuestDatabase.stepOrdinal(current)) {
            db.setProgress(quest, step)
            return true
        }
        return false
    }
}
