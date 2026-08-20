package net.sourceforge.kolmafia.quest

/**
 * Desktop [ChoiceControl] Madness Bakery machines 1080–1081 / 1084.
 */
object MadnessBakeryChoiceSync {

    const val BAGELMAT = 1080
    const val ASSAULT_AND_BAGUETTERY = 1081
    const val POPULAR_MACHINE = 1084
    const val WAD_OF_DOUGH = 159
    const val MAGICAL_BAGUETTE = 8167
    const val STRAWBERRY = 786
    const val ENCHANTED_ICING = 8168

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        return when (choiceId) {
            BAGELMAT -> {
                if (!html.contains("shove a wad of dough into the slot")) return false
                consumeItem(WAD_OF_DOUGH, 1)
                true
            }
            ASSAULT_AND_BAGUETTERY -> {
                if (decision !in 1..3) return false
                consumeItem(MAGICAL_BAGUETTE, 1)
                true
            }
            POPULAR_MACHINE -> {
                if (!html.contains("popular tart springs")) return false
                consumeItem(WAD_OF_DOUGH, 1)
                consumeItem(STRAWBERRY, 1)
                consumeItem(ENCHANTED_ICING, 1)
                true
            }
            else -> false
        }
    }
}
