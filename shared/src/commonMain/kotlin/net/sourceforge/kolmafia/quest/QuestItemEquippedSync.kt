package net.sourceforge.kolmafia.quest

/**
 * Desktop [QuestManager.updateQuestItemEquipped] Elemental International quest starts.
 */
object QuestItemEquippedSync {

    const val MINI_CASSETTE_RECORDER = 7832
    const val GORE_BUCKET = 7833
    const val GPS_WATCH = 7836
    const val LUBE_SHOES = 8244
    const val TRASH_NET = 8245
    const val MASCOT_MASK = 8246
    const val WALFORDS_BUCKET = 8672

    fun apply(itemId: Int, questDatabase: QuestDatabase?): Boolean {
        if (questDatabase == null) return false
        val quest = when (itemId) {
            GORE_BUCKET -> Quest.GORE
            MINI_CASSETTE_RECORDER -> Quest.JUNGLE_PUN
            GPS_WATCH -> Quest.OUT_OF_ORDER
            TRASH_NET -> Quest.FISH_TRASH
            LUBE_SHOES -> Quest.SUPER_LUBER
            MASCOT_MASK -> Quest.ZIPPITY_DOO_DAH
            WALFORDS_BUCKET -> Quest.BUCKET
            else -> return false
        }
        questDatabase.setQuestIfBetter(quest, "step1")
        return true
    }
}
