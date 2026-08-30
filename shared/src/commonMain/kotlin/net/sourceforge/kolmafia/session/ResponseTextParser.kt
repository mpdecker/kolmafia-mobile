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
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import net.sourceforge.kolmafia.request.ClanHallRequest
import net.sourceforge.kolmafia.request.ClanMembersRequest
import net.sourceforge.kolmafia.request.ClanLogRequest
import net.sourceforge.kolmafia.request.ClanWarRequest
import net.sourceforge.kolmafia.request.ManageStoreSync
import net.sourceforge.kolmafia.request.PirateSpecialSync
import net.sourceforge.kolmafia.request.PlaceSync
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
            url.contains("actionbar.php", ignoreCase = true) -> "actionbar"
            url.contains("sellstuff_ugly.php", ignoreCase = true) -> "sellstuff_ugly"
            url.contains("sellstuff.php", ignoreCase = true) -> "sellstuff"
            url.contains("backoffice.php", ignoreCase = true) ||
                url.contains("manageprices.php", ignoreCase = true) ||
                url.contains("mallstore.php", ignoreCase = true) -> "mallstore"
            url.contains("clan_stash.php", ignoreCase = true) -> "clan_stash"
            url.contains("clan_viplounge.php", ignoreCase = true) -> "clan_viplounge"
            url.contains("clan_rumpus.php", ignoreCase = true) -> "clan_rumpus"
            url.contains("clan_hall.php", ignoreCase = true) -> "clan_hall"
            url.contains("clan_log.php", ignoreCase = true) -> "clan_log"
            url.contains("clan_attack.php", ignoreCase = true) -> "clan_attack"
            url.contains("clan_war.php", ignoreCase = true) -> "clan_war"
            url.contains("showclan.php", ignoreCase = true) -> "showclan"
            url.contains("clan_members.php", ignoreCase = true) -> "clan_members"
            url.contains("clan_detailedroster.php", ignoreCase = true) -> "clan_detailedroster"
            url.contains("sendmessage.php", ignoreCase = true) -> "sendmessage"
            url.contains("inventory.php", ignoreCase = true) -> "inventory"
            url.contains("campground.php", ignoreCase = true) -> "campground"
            url.contains("cafe.php", ignoreCase = true) -> "cafe"
            url.contains("choice.php", ignoreCase = true) -> "choice"
            url.contains("desc_item.php", ignoreCase = true) ||
                url.contains("desc_effect.php", ignoreCase = true) ||
                url.contains("desc_skill.php", ignoreCase = true) -> "description"
            url.contains("familiar.php", ignoreCase = true) -> "familiar"
            url.contains("guild.php", ignoreCase = true) -> "guild"
            url.contains("questlog.php", ignoreCase = true) -> "questlog"
            url.contains("shop.php", ignoreCase = true) -> "shop"
            url.contains("island.php", ignoreCase = true) -> "island"
            url.contains("place.php", ignoreCase = true) -> "place"
            url.contains("basement.php", ignoreCase = true) -> "basement"
            url.contains("arena.php", ignoreCase = true) -> "arena"
            url.contains("bounty.php", ignoreCase = true) -> "bounty"
            url.contains("leaflet.php", ignoreCase = true) -> "leaflet"
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
            "actionbar" -> ActionBarManager.update(html)
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
                } else if (!u.contains("action=take", true) &&
                    !u.contains("action=contribute", true)
                ) {
                    ClanStashRequest.storeContents(ClanStashRequest.parseContentsStatic(html))
                }
            }
            "clan_viplounge" -> ClanLoungeRequest.parseResponse(u, html, preferences)
            "clan_rumpus" -> ClanRumpusRequest.parseResponse(u, html, preferences)
            "clan_hall" -> ClanHallRequest.parseResponse(u, html)
            "showclan", "clan_members", "clan_detailedroster" ->
                ClanMembersRequest.parseResponse(u, html)
            "clan_log" -> ClanLogRequest.parseResponse(u, html)
            "clan_attack", "clan_war" -> ClanWarRequest.parseResponse(u, html, preferences)
            "sendmessage" -> SendMailSync.parseTransfer(u, html, emptyList(), 0, inventory, character)
            "inventory" -> InventoryActionSync.parse(u, html, inventory, character, preferences)
            "place" -> PlaceSync.parseResponse(u, html, preferences, character, inventory)
            "campground", "cafe", "choice", "guild", "island", "shop" -> {
                // These pages frequently contain acquire/loss and auto-create text. Specialized
                // visit hooks run before this router; this pass only handles generic result text.
                ResultProcessor.processResults(false, html, inventory, character, preferences)
            }
            "basement" -> BasementSync.checkBasement(html, preferences)
            "arena" -> CakeArenaSync.parseResponse(u, html, preferences, character, inventory)
            "bounty" -> BountyHunterSync.parseResponse(u, html, preferences, character, inventory)
            "leaflet" -> LeafletManager.parseLocation(html)
            "beerpong" -> PirateSpecialSync.parseBeerPong(u, html, preferences)
            "shrine" -> PirateSpecialSync.parseShrine(u, html, preferences)
            "api" -> {
                // CharacterStatusRefresh / CharacterRequest own api.php; mark only.
                preferences?.setBoolean("_apiStatusSeen", true)
            }
        }
    }
}
