package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.AccountSync
import net.sourceforge.kolmafia.request.AutosellSync
import net.sourceforge.kolmafia.request.BasementSync
import net.sourceforge.kolmafia.request.BountyHunterSync
import net.sourceforge.kolmafia.request.CakeArenaSync
import net.sourceforge.kolmafia.request.CharSheetSync
import net.sourceforge.kolmafia.request.ClanStashSync
import net.sourceforge.kolmafia.request.ManageStoreSync
import net.sourceforge.kolmafia.request.PirateSpecialSync
import net.sourceforge.kolmafia.request.SendMailSync

/**
 * Selective ResponseTextParser.externalUpdate page routers (Phases 2261–2330).
 * Dispatches high-traffic pages to typed syncs — full desktop switch remains deferred.
 */
object ResponseTextParser {
    fun classify(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return when {
            url.contains("skills.php", ignoreCase = true) ||
                url.contains("skillz.php", ignoreCase = true) -> "skills"
            url.contains("closet.php", ignoreCase = true) -> "closet"
            url.contains("storage.php", ignoreCase = true) -> "storage"
            url.contains("displaycollection.php", ignoreCase = true) ||
                url.contains("managecollection.php", ignoreCase = true) -> "display"
            url.contains("inv_use.php", ignoreCase = true) -> "use"
            url.contains("inv_equip.php", ignoreCase = true) -> "equip"
            url.contains("account.php", ignoreCase = true) -> "account"
            url.contains("charsheet.php", ignoreCase = true) -> "charsheet"
            url.contains("api.php", ignoreCase = true) -> "api"
            url.contains("sellstuff_ugly.php", ignoreCase = true) -> "sellstuff_ugly"
            url.contains("sellstuff.php", ignoreCase = true) -> "sellstuff"
            url.contains("backoffice.php", ignoreCase = true) ||
                url.contains("manageprices.php", ignoreCase = true) ||
                url.contains("mallstore.php", ignoreCase = true) -> "mallstore"
            url.contains("clan_stash.php", ignoreCase = true) -> "clan_stash"
            url.contains("sendmessage.php", ignoreCase = true) -> "sendmessage"
            url.contains("inventory.php", ignoreCase = true) -> "inventory"
            url.contains("basement.php", ignoreCase = true) -> "basement"
            url.contains("arena.php", ignoreCase = true) -> "arena"
            url.contains("bounty.php", ignoreCase = true) -> "bounty"
            url.contains("beerpong", ignoreCase = true) -> "beerpong"
            url.contains("shrine", ignoreCase = true) ||
                url.contains("altarofbones", ignoreCase = true) -> "shrine"
            else -> null
        }
    }

    fun externalUpdate(
        url: String?,
        html: String,
        onRoute: (page: String) -> Unit = {},
        preferences: Preferences? = null,
        character: KoLCharacter? = null,
        inventory: InventoryManager? = null,
    ) {
        val page = classify(url) ?: return
        if (html.isBlank() && page != "sellstuff") return
        onRoute(page)
        val u = url.orEmpty()
        when (page) {
            "account" -> AccountSync.parseAccountData(u, html, preferences, character)
            "charsheet" -> CharSheetSync.parseStatus(html, character, preferences)
            "sellstuff" -> AutosellSync.parseCompact(u, inventory, character)
            "sellstuff_ugly" -> AutosellSync.parseDetailed(u, html, inventory, character)
            "mallstore" -> ManageStoreSync.parseResponse(u, html, inventory, preferences)
            "clan_stash" -> {
                val itemId = Regex("""whichitem=(\d+)""", RegexOption.IGNORE_CASE)
                    .find(u)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val qty = Regex("""qty=(\d+)""", RegexOption.IGNORE_CASE)
                    .find(u)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                if (itemId > 0) {
                    ClanStashSync.parseTransfer(u, html, itemId, qty, inventory)
                }
            }
            "sendmessage" -> SendMailSync.parseTransfer(u, html, emptyList(), 0, inventory, character)
            "inventory" -> InventoryActionSync.parse(u, html, inventory, character, preferences)
            "basement" -> BasementSync.checkBasement(html, preferences)
            "arena" -> CakeArenaSync.parseResponse(u, html, preferences, character, inventory)
            "bounty" -> BountyHunterSync.parseResponse(u, html, preferences, character, inventory)
            "beerpong" -> PirateSpecialSync.parseBeerPong(u, html, preferences)
            "shrine" -> PirateSpecialSync.parseShrine(u, html, preferences)
            "api" -> {
                // CharacterStatusRefresh / CharacterRequest own api.php; mark only.
                preferences?.setBoolean("_apiStatusSeen", true)
            }
        }
    }
}
