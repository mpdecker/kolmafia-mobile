package net.sourceforge.kolmafia.quest

/**
 * Desktop [ChoiceControl] Hashing with your vice choice 1551.
 */
object HashingChoiceSync {

    const val CHOICE_ID = 1551

    private val IID_FIELD = Regex("""(?:^|[?&])iid=(\d+)""", RegexOption.IGNORE_CASE)

    fun apply(
        choiceId: Int,
        html: String,
        choiceUrl: String = "",
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID) return false
        if (!html.contains("You crush the schematic into little bits of checksum.")) return false
        val itemId = IID_FIELD.find(choiceUrl)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (itemId != null) {
            consumeItem(itemId, 1)
        }
        return true
    }
}
