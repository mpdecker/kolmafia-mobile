package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/** Desktop [net.sourceforge.kolmafia.request.OrcChasmRequest]. */
object OrcChasmRequest {
    const val BRIDGE = 535
    const val MINIATURE_SUSPENSION_BRIDGE = 6661
    const val SNOW_BOARDS = 7076
    const val FANCY_OIL_PAINTING = 7148
    const val BRIDGE_TRUSS = 9556

    fun getChasmProgress(preferences: Preferences, character: KoLCharacter?): Int {
        ensureUpdated(preferences, character)
        return preferences.getInt("chasmBridgeProgress", 0)
    }

    fun setChasmProgress(preferences: Preferences, character: KoLCharacter?, value: Int) {
        ensureUpdated(preferences, character)
        preferences.setInt("chasmBridgeProgress", value.coerceIn(0, 30))
    }

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        character: KoLCharacter?,
        inventoryManager: InventoryManager?,
    ) {
        if (!url.contains("orc_chasm", ignoreCase = true) &&
            !html.contains("action=bridge")
        ) {
            if (!url.contains("place.php", ignoreCase = true)) return
        }
        if (preferences == null) return
        val action = Regex("""action=bridge([^"'>\s]*)""").find(html)?.groupValues?.getOrNull(1)
        if (action != null) {
            val previous = getChasmProgress(preferences, character)
            if (action == "_done") {
                setChasmProgress(preferences, character, 30)
            } else {
                action.toIntOrNull()?.let { setChasmProgress(preferences, character, it) }
            }
        }
        when {
            html.contains("You disassemble it into usable lumber and fasteners.") ->
                inventoryManager?.consumeItemLocally(BRIDGE, 1)
            html.contains("miniature suspension bridge") ->
                inventoryManager?.consumeItemLocally(MINIATURE_SUSPENSION_BRIDGE, 1)
            html.contains("snow boards") ->
                inventoryManager?.consumeItemLocally(
                    SNOW_BOARDS,
                    inventoryManager.state.value.items[SNOW_BOARDS]?.quantity ?: 1,
                )
            html.contains("oil painting") ->
                inventoryManager?.consumeItemLocally(
                    FANCY_OIL_PAINTING,
                    inventoryManager.state.value.items[FANCY_OIL_PAINTING]?.quantity ?: 1,
                )
            html.contains("bridge truss") ->
                inventoryManager?.consumeItemLocally(BRIDGE_TRUSS, 1)
        }
    }

    fun registerRequest(url: String): Boolean =
        url.contains("orc_chasm", ignoreCase = true)

    private fun ensureUpdated(preferences: Preferences, character: KoLCharacter?) {
        val last = preferences.getInt("lastChasmReset", -1)
        val ascension = character?.state?.value?.ascensionNumber ?: 0
        if (last < ascension) {
            preferences.setInt("lastChasmReset", ascension)
            preferences.setInt("chasmBridgeProgress", 0)
        }
    }
}
