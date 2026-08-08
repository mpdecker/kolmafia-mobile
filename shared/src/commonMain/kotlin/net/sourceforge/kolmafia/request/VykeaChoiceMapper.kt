package net.sourceforge.kolmafia.request

/** Desktop [VYKEARequest] choice 1120–1123 option mapping. */
object VykeaChoiceMapper {
    const val SKIP_OPTION = 6

    const val INSTRUCTIONS_ID = 8730
    const val HEX_KEY_ID = 8729
    const val PLANK_ID = 8725
    const val RAIL_ID = 8726
    const val BRACKET_ID = 8727
    const val DOWEL_ID = 8728
    const val FRENZY_RUNE_ID = 8722
    const val BLOOD_RUNE_ID = 8723
    const val LIGHTNING_RUNE_ID = 8724

    fun optionFor(choiceId: Int, itemId: Int, count: Int): Int = when (choiceId) {
        1120 -> when (itemId) {
            PLANK_ID -> 1
            RAIL_ID -> 2
            else -> 0
        }
        1121 -> when (itemId) {
            FRENZY_RUNE_ID -> 1
            BLOOD_RUNE_ID -> 2
            LIGHTNING_RUNE_ID -> 3
            else -> SKIP_OPTION
        }
        1122 -> when {
            itemId == DOWEL_ID -> when (count) {
                1 -> 1
                11 -> 2
                23 -> 3
                37 -> 4
                else -> 0
            }
            else -> SKIP_OPTION
        }
        1123 -> when (itemId) {
            PLANK_ID -> 1
            RAIL_ID -> 2
            BRACKET_ID -> 3
            else -> 0
        }
        else -> 0
    }

    fun consumesIngredient(option: Int): Boolean = option != 0 && option != SKIP_OPTION
}
