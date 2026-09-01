package net.sourceforge.kolmafia.quest

/**
 * Desktop [ChoiceControl] Hashing with your vice choice 1551.
 */
object HashingChoiceSync {

    const val CHOICE_ID = 1551

    private val IID_FIELD = Regex("""(?:^|[?&])iid=(\d+)""", RegexOption.IGNORE_CASE)
    private val CHECKSUM_ITEM = Regex(
        """rel=["'][^"']*\bid=(\d+)[^"']*\bn=(\d+)[^"']*["']""",
        RegexOption.IGNORE_CASE,
    )

    fun apply(
        choiceId: Int,
        html: String,
        choiceUrl: String = "",
        consumeItem: (Int, Int) -> Unit = { _, _ -> },
        gainItem: (Int, Int) -> Unit = { _, _ -> },
    ): Boolean {
        if (choiceId != CHOICE_ID) return false
        if (!html.contains("You crush the schematic into little bits of checksum.")) return false
        val itemId = IID_FIELD.find(choiceUrl)
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
            ?.takeIf { it > 0 }
            ?: return false
        consumeItem(itemId, 1)
        CHECKSUM_ITEM.findAll(html).forEach { match ->
            val checksumId = match.groupValues[1].toIntOrNull()
            val quantity = match.groupValues[2].toIntOrNull()
            if (checksumId != null && checksumId > 0 && quantity != null && quantity > 0) {
                gainItem(checksumId, quantity)
            }
        }
        return true
    }
}
