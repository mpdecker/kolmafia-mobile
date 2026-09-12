package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.shop.BaconShopSync
import net.sourceforge.kolmafia.shop.CoinmasterResponseSync

/**
 * Phases 6771–6790 — named alias hubs for desktop Request class names
 * that were consolidated into residual hubs (Behavioral Deepen XLVIII Track C).
 *
 * Shop aliases delegate to existing Phase 5410/5470/5290 hubs. Sibling hubs remain
 * registered in [GameRuntimeLibrary.processVisitResponseHooks]; only residual
 * create/visit paths without a prior hub (Sushi / TakerSpace / VYKEA) are wired
 * there to avoid duplicate session-log lines.
 */

/** Desktop [SHAWARMARequest] → [ShawarmaInitiativeRequestHub]. */
object SHAWARMARequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean =
        ShawarmaInitiativeRequestHub.registerRequest(url, sessionLogger)
}

/** Desktop SI [ArmoryRequest] (si_shop3) — not Armory & Leggery. */
object ArmoryRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean =
        SpacegateArmoryRequestHub.registerRequest(url, sessionLogger)
}

/** Desktop [LTTRequest]. */
object LTTRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean =
        LtTRequestHub.registerRequest(url, sessionLogger)
}

/** Desktop [MemeShopRequest] / Internet Meme Shop. */
object MemeShopRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean =
        InternetMemeShopRequestHub.registerRequest(url, sessionLogger)

    fun parseResponse(url: String, html: String, preferences: Preferences?): Boolean {
        if (!url.contains("whichshop=bacon", ignoreCase = true)) return false
        preferences?.let { BaconShopSync.syncFromShopHtml(html, it) }
        return true
    }
}

/** Desktop [NinjaStoreRequest] → Nina Store. */
object NinjaStoreRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean =
        NinaStoreRequestHub.registerRequest(url, sessionLogger)
}

/** Desktop [BoutiqueRequest] → Paul's Boutique / cindy. */
object BoutiqueRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean =
        CindyRequestHub.registerRequest(url, sessionLogger)
}

/** Desktop [CRIMBCOGiftShopRequest] → Phase 5290 CrimboCartel legacy hub. */
object CRIMBCOGiftShopRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean =
        CrimboCartelLegacyRequestHub.registerRequest(url, sessionLogger)
}

/**
 * Desktop [SushiRequest] residual — create/consume lives in [SushiCreateRequest] /
 * [SushiConsumptionSync]; this hub covers sushi.php session-log registration.
 */
object SushiRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("sushi.php", ignoreCase = true)) return false
        val fields = SushiChoiceMapper.formFieldsFromUrl(url)
        if (fields != null) {
            SushiConsumptionSync.registerRequest(fields, sessionLogger)
        } else {
            sessionLogger?.appendRawLine("Visiting The Sushi Bar")
        }
        return true
    }
}

/**
 * Desktop [TakerSpaceRequest] — create posts choice.php?whichchoice=1537;
 * visit supplies sync lives in TakerSpaceChoiceSync (no shops.txt whichshop).
 */
object TakerSpaceRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("whichchoice=1537", ignoreCase = true) &&
            !url.contains("takerspace", ignoreCase = true)
        ) {
            return false
        }
        sessionLogger?.appendRawLine("Creating at TakerSpace")
        return true
    }
}

/**
 * Desktop [VYKEARequest] — companion create lives in [VykeaCreateRequest]
 * (inv_use instructions → choice 1120–1123).
 */
object VYKEARequest {
    private val VYKEA_CHOICE = Regex("""whichchoice=112[0-3]""", RegexOption.IGNORE_CASE)

    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        val isInstructions =
            url.contains("inv_use.php", ignoreCase = true) &&
                url.contains("whichitem=${VykeaChoiceMapper.INSTRUCTIONS_ID}")
        if (!isInstructions && !VYKEA_CHOICE.containsMatchIn(url)) return false
        sessionLogger?.appendRawLine("Building a VYKEA companion")
        return true
    }
}

/**
 * Desktop [CoinMasterRequest] base — balance/buy/sell routed through
 * [CoinmasterResponseSync] (coinmasters consolidated; no per-class HTTP).
 */
object CoinMasterRequest {
    /** Naming stub — visit registration is per-shop residual hubs. */
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean = false

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
    ): Boolean =
        CoinmasterResponseSync.apply(url, html, preferences, inventory, character)

    fun parseBalance(
        url: String,
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
    ): Boolean = parseResponse(url, html, preferences, inventory, character)
}

/** Desktop [CoinMasterShopRequest] shop.php subclass of [CoinMasterRequest]. */
object CoinMasterShopRequest {
    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean =
        CoinMasterRequest.registerRequest(url, sessionLogger)

    fun parseResponse(
        url: String,
        html: String,
        preferences: Preferences?,
        inventory: InventoryManager? = null,
        character: KoLCharacter? = null,
    ): Boolean = CoinMasterRequest.parseResponse(url, html, preferences, inventory, character)
}
