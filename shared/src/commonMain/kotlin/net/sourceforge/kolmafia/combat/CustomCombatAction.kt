package net.sourceforge.kolmafia.combat

/**
 * Desktop [CustomCombatAction] — one CCS line (round action or macro directive).
 */
class CustomCombatAction(
    val index: Int,
    val indent: String,
    rawAction: String,
    val isMacro: Boolean,
) {
    val action: String = when {
        isMacro -> {
            if (CombatActionManager.isMacroAction(rawAction)) rawAction
            else "\"$rawAction\""
        }
        else -> CombatActionManager.getLongCombatOptionName(rawAction)
    }

    val sectionReference: String? = when {
        action.equals("default", ignoreCase = true) -> "default"
        action.startsWith("section", ignoreCase = true) ->
            CombatActionManager.encounterKey(action.substring(7).trim().lowercase())
        else -> null
    }

    fun storeLine(): String = "$indent$action"
}
