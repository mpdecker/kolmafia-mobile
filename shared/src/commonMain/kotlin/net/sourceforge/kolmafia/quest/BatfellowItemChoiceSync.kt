package net.sourceforge.kolmafia.quest

/**
 * Desktop [ChoiceControl] Batfellow zone item consumes 1140–1148.
 */
object BatfellowItemChoiceSync {

    const val CONSERVATORY = 1140
    const val RESERVOIR = 1141
    const val CEMETERY = 1142
    const val SEWERS = 1143
    const val ASYLUM = 1144
    const val LIBRARY = 1145
    const val CLOCK_FACTORY = 1146
    const val FOUNDRY = 1147
    const val TRIVIA_COMPANY = 1148

    const val BAT_OOMERANG = 8797
    const val BAT_JUTE = 8798
    const val BAT_O_MITE = 8799
    const val ULTRACOAGULATOR = 8808
    const val FINGERPRINT_DUSTING_KIT = 8810
    const val EXPLODING_KICKBALL = 8812
    const val GLOB_OF_BAT_GLUE = 8813
    const val BAT_AID_BANDAGE = 8814
    const val BAT_BEARING = 8815

    fun apply(
        choiceId: Int,
        decision: Int,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        val consumes = when (choiceId) {
            CONSERVATORY -> when (decision) {
                4 -> listOf(GLOB_OF_BAT_GLUE to 1)
                5 -> listOf(FINGERPRINT_DUSTING_KIT to 3)
                else -> emptyList()
            }
            RESERVOIR -> when (decision) {
                4 -> listOf(BAT_AID_BANDAGE to 1)
                5 -> listOf(ULTRACOAGULATOR to 3)
                else -> emptyList()
            }
            CEMETERY -> when (decision) {
                4 -> listOf(BAT_BEARING to 1)
                5 -> listOf(EXPLODING_KICKBALL to 3)
                else -> emptyList()
            }
            SEWERS -> if (decision == 4) listOf(BAT_OOMERANG to 1) else emptyList()
            ASYLUM -> if (decision == 4) listOf(BAT_O_MITE to 1) else emptyList()
            LIBRARY -> if (decision == 4) listOf(BAT_JUTE to 1) else emptyList()
            CLOCK_FACTORY -> if (decision == 4) listOf(EXPLODING_KICKBALL to 1) else emptyList()
            FOUNDRY -> if (decision == 4) listOf(ULTRACOAGULATOR to 1) else emptyList()
            TRIVIA_COMPANY -> if (decision == 4) listOf(FINGERPRINT_DUSTING_KIT to 1) else emptyList()
            else -> return false
        }
        if (consumes.isEmpty()) return false
        consumes.forEach { (itemId, qty) -> consumeItem(itemId, qty) }
        return true
    }
}
