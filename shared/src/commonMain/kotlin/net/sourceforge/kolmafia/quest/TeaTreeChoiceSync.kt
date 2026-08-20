package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [ChoiceControl] potted tea tree choices 1104–1105.
 */
object TeaTreeChoiceSync {

    const val TREE_TEA = 1104
    const val SPECIFICI_TEA = 1105

    fun apply(
        choiceId: Int,
        decision: Int,
        preferences: Preferences?,
        choiceUrl: String = "",
    ): Boolean {
        if (preferences == null) return false
        return when (choiceId) {
            TREE_TEA -> {
                if (decision != 1) return false
                preferences.setBoolean("_pottedTeaTreeUsed", true)
                true
            }
            SPECIFICI_TEA -> {
                if (!choiceUrl.contains("itemid", ignoreCase = true)) return false
                preferences.setBoolean("_pottedTeaTreeUsed", true)
                true
            }
            else -> false
        }
    }
}
