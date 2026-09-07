package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [net.sourceforge.kolmafia.request.concoction.CreateItemRequest] craft.php hub. */
object CreateItemRequest {
    fun parseResponse(
        url: String,
        html: String,
        inventory: InventoryManager? = null,
        preferences: Preferences? = null,
        characterState: CharacterState? = null,
        sessionLogger: SessionLogger? = null,
    ) = CreateItemCraftSync.parseCrafting(
        url, html, inventory, preferences, characterState, sessionLogger,
    )

    fun getAdventuresUsed(url: String): Int = CraftRequest.getAdventuresUsed(url)

    fun registerRequest(url: String): Boolean =
        url.contains("craft.php", ignoreCase = true)
}
