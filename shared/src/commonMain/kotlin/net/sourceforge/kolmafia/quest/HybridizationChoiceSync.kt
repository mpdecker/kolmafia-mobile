package net.sourceforge.kolmafia.quest

/**
 * Desktop [ChoiceControl] Hybridization Chamber choice 1553.
 */
object HybridizationChoiceSync {

    const val CHOICE_ID = 1553

    private val FAM_FIELD = Regex("""(?:^|[?&])fam=(\d+)""", RegexOption.IGNORE_CASE)

    fun apply(
        choiceId: Int,
        decision: Int,
        html: String,
        choiceUrl: String = "",
        currentFamiliarId: () -> Int? = { null },
        clearActiveFamiliar: () -> Unit = {},
        refreshStatus: () -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID) return false
        if (decision != 1) return false
        if (!html.contains("<span class='guts'>Grafting")) return false
        val famId = FAM_FIELD.find(choiceUrl)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (famId != null && famId == currentFamiliarId()) {
            clearActiveFamiliar()
        }
        refreshStatus()
        return true
    }
}
