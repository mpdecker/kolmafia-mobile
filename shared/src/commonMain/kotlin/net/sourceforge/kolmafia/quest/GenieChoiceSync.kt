package net.sourceforge.kolmafia.quest

import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.GenieRequest

/**
 * Thin adapter wiring [GenieRequest] visit/post into choice 1267.
 */
object GenieChoiceSync {

    const val CHOICE_ID = GenieRequest.CHOICE_ID

    fun applyVisit(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        GenieRequest.visitChoice(html, preferences)
        return true
    }

    fun apply(
        choiceId: Int,
        html: String,
        preferences: Preferences?,
        choiceUrl: String = "",
        inventoryManager: InventoryManager? = null,
    ): Boolean {
        if (choiceId != CHOICE_ID || preferences == null) return false
        val wish = Regex("""(?:^|[&?])wish=([^&]*)""")
            .find(choiceUrl)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { decodeUrlComponent(it) }
            .orEmpty()
        val before = preferences.getInt(GenieRequest.WISHES_USED_PREF, 0)
        GenieRequest.postChoice(
            html = html,
            wish = wish,
            preferences = preferences,
            usedPocketWish = false,
            inventoryManager = inventoryManager,
        )
        return preferences.getInt(GenieRequest.WISHES_USED_PREF, 0) != before ||
            html.contains("You acquire") ||
            html.contains("You gain")
    }

    private fun decodeUrlComponent(raw: String): String =
        raw.replace('+', ' ')
            .replace(Regex("%([0-9A-Fa-f]{2})")) { match ->
                match.groupValues[1].toIntOrNull(16)?.toChar()?.toString() ?: match.value
            }
}
