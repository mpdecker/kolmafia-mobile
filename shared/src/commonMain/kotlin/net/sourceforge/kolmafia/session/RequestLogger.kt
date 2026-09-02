package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.FamTeamSync
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ArcadeRequest
import net.sourceforge.kolmafia.request.BatFellowRequest
import net.sourceforge.kolmafia.request.DwarfContraptionRequest
import net.sourceforge.kolmafia.request.DwarfFactoryRequest
import net.sourceforge.kolmafia.request.ElvmachineRequest
import net.sourceforge.kolmafia.request.PeeVPeeRequest
import net.sourceforge.kolmafia.request.AutoMallRequest
import net.sourceforge.kolmafia.request.ManageStoreRequest
import net.sourceforge.kolmafia.request.MonsterManuelRequest
import net.sourceforge.kolmafia.request.MushroomRequest
import net.sourceforge.kolmafia.request.ScrapheapRequest
import net.sourceforge.kolmafia.request.UneffectRequest
import net.sourceforge.kolmafia.request.CakeArenaRequest
import net.sourceforge.kolmafia.request.BountyHunterHunterRequest
import net.sourceforge.kolmafia.request.BeachCombRequest
import net.sourceforge.kolmafia.request.SkateParkRequest
import net.sourceforge.kolmafia.request.NemesisRequest
import net.sourceforge.kolmafia.request.TavernRequest
import net.sourceforge.kolmafia.request.GourdRequest
import net.sourceforge.kolmafia.request.GuildRequest
import net.sourceforge.kolmafia.request.FleaMarketRequest
import net.sourceforge.kolmafia.request.FleaMarketSellRequest
import net.sourceforge.kolmafia.session.DvorakManager
import net.sourceforge.kolmafia.mall.MallPurchaseRequest
import net.sourceforge.kolmafia.request.TrophyHutRequest
import net.sourceforge.kolmafia.request.VolcanoIslandRequest
import net.sourceforge.kolmafia.quest.SorceressLairSync
import net.sourceforge.kolmafia.shop.SwaggerShopSync

/**
 * Desktop [RequestLogger.doRegister] / [RequestLogger.updateSessionLog] hub
 * (Phases 1731–1790). Classifies URLs and emits human-readable session-log lines.
 *
 * Not a full debug/trace port — high-traffic action lines only.
 */
object RequestLogger {

    var lastURLString: String = ""
        private set
    var wasLastRequestSimple: Boolean = true
        private set

    /** Injected round counter (defaults to [ChoiceCombatAshState.currentRound]). */
    var currentRound: () -> Int = { ChoiceCombatAshState.currentRound }

    fun updateSessionLog(message: String, sessionLogger: SessionLogger?) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        sessionLogger?.appendRawLine(trimmed)
    }

    /**
     * Desktop [RequestLogger.registerRequest] — returns true when a specialized
     * handler claimed the URL (wasLastRequestSimple = false).
     */
    fun registerRequest(
        urlString: String,
        sessionLogger: SessionLogger?,
        preferences: Preferences? = null,
        formFields: Map<String, String> = emptyMap(),
    ): Boolean {
        return try {
            doRegister(urlString, sessionLogger, preferences, formFields)
        } catch (_: Exception) {
            false
        }
    }

    private fun doRegister(
        rawUrl: String,
        sessionLogger: SessionLogger?,
        preferences: Preferences?,
        formFields: Map<String, String>,
    ): Boolean {
        val urlString = stripBase(rawUrl)
        // Mid-fight: ignore non-fight redirects
        if (currentRound() != 0 &&
            !urlString.startsWith("fight.php") &&
            !urlString.startsWith("fambattle.php")
        ) {
            return false
        }

        lastURLString = urlString

        if (urlString.startsWith("api") ||
            urlString.startsWith("charpane") ||
            urlString.startsWith("account") ||
            urlString.startsWith("login") ||
            urlString.startsWith("logout")
        ) {
            return false
        }

        if (NemesisRequest.registerRequest(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }
        if (DvorakManager.registerRequest(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }
        if (TavernRequest.registerRequest(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }
        if (GourdRequest.registerRequest(urlString, preferences = preferences, logger = sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }
        if (urlString.startsWith("messages.php", ignoreCase = true) ||
            urlString.startsWith("mail.php", ignoreCase = true)
        ) {
            wasLastRequestSimple = false
            return true
        }
        if (urlString.startsWith("account_contactlist.php", ignoreCase = true)) {
            wasLastRequestSimple = false
            return true
        }

        if (GuildRequest.registerRequest(urlString, logger = sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }
        if (SkateParkRequest.registerRequest(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }

        // Adventure snarfblat / location
        if (registerAdventure(urlString, sessionLogger, preferences)) {
            wasLastRequestSimple = false
            return true
        }

        if (urlString.startsWith("shop.php") && registerShop(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }

        if (urlString.startsWith("choice.php") &&
            BeachCombRequest.registerRequest(urlString, preferences, sessionLogger)
        ) {
            wasLastRequestSimple = false
            return true
        }

        if (urlString.startsWith("choice.php") &&
            registerChoice(urlString, sessionLogger, preferences, formFields)
        ) {
            wasLastRequestSimple = false
            return true
        }

        if (urlString.contains("ocean.php") && sessionLogger != null) {
            OceanManager.registerRequest(urlString, sessionLogger)
            wasLastRequestSimple = false
            return true
        }

        if (ElvmachineRequest.registerRequest(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }

        if (FamTeamSync.registerRequest(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }

        if (PeeVPeeRequest.registerRequest(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }

        if (SwaggerShopSync.registerRequest(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }

        MallPurchaseRequest.registerRequest(urlString) { ItemDatabase.getItemName(it) }.let { message ->
            if (message != null) {
                updateSessionLog(message, sessionLogger)
                wasLastRequestSimple = false
                return true
            }
        }
        ManageStoreRequest.registerRequest(urlString) { ItemDatabase.getItemName(it) }.let { message ->
            if (message != null) {
                updateSessionLog(message, sessionLogger)
                wasLastRequestSimple = false
                return true
            }
        }
        AutoMallRequest.registerRequest(urlString)?.let {
            updateSessionLog(it, sessionLogger)
            wasLastRequestSimple = false
            return true
        }
        if (urlString.startsWith("sellstuff.php") || urlString.startsWith("sellstuff_ugly.php")) {
            updateSessionLog("autosell", sessionLogger)
            wasLastRequestSimple = false
            return true
        }

        if (WereProfessorResearchSync.registerRequest(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }

        if (MonsterManuelRequest.registerRequest(urlString)) {
            wasLastRequestSimple = false
            return true
        }

        if (MushroomRequest.registerRequest(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }

        if (urlString.startsWith("arena.php") &&
            CakeArenaRequest.registerRequest(urlString, sessionLogger = sessionLogger)
        ) {
            wasLastRequestSimple = false
            return true
        }

        if (urlString.startsWith("bounty.php") &&
            BountyHunterHunterRequest.registerRequest(urlString, preferences, sessionLogger)
        ) {
            wasLastRequestSimple = false
            return true
        }

        if (UneffectRequest.registerRequest(urlString, sessionLogger = sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }

        // Cargo shorts inspect (inventory.php?action=pocket)
        if (urlString.contains("inventory.php") && urlString.contains("action=pocket")) {
            updateSessionLog("Inspecting Cargo Cultist Shorts", sessionLogger)
            wasLastRequestSimple = false
            return true
        }

        // No query → skip (except claimed above)
        if (!urlString.contains("?")) {
            return false
        }

        if (registerCampground(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }

        if (registerSkill(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }

        if (registerEquipment(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }

        if (urlString.startsWith("campground") ||
            urlString.startsWith("doc.php") ||
            urlString.startsWith("inventory.php?ajax") ||
            urlString.startsWith("inventory.php?which=") ||
            urlString.startsWith("inventory.php?action=message") ||
            urlString.startsWith("mining")
        ) {
            return false
        }

        if (registerCreate(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }

        if (registerUseItem(urlString, sessionLogger)) {
            wasLastRequestSimple = false
            return true
        }

        if (urlString.startsWith("place.php") && registerPlace(urlString, sessionLogger, preferences)) {
            wasLastRequestSimple = false
            return true
        }

        if (registerLongTail(urlString, sessionLogger, preferences)) {
            wasLastRequestSimple = false
            return true
        }

        // Simple fallback for unrecognized query URLs
        wasLastRequestSimple = true
        return false
    }

    // ── Track A: adventure / shop / place / choice ───────────────────────────

    private fun registerAdventure(
        url: String,
        sessionLogger: SessionLogger?,
        preferences: Preferences?,
    ): Boolean {
        if (!url.startsWith("adventure.php") && !url.contains("snarfblat=")) return false
        val snarf = queryParam(url, "snarfblat") ?: return false
        val name = preferences?.getString(Preferences.LAST_LOCATION, "").orEmpty()
            .ifBlank { "snarfblat $snarf" }
        updateSessionLog("[$name]", sessionLogger)
        return true
    }

    private fun registerShop(url: String, sessionLogger: SessionLogger?): Boolean {
        val whichshop = queryParam(url, "whichshop") ?: return true // bare shop.php visit
        val action = queryParam(url, "action")
        val row = queryParam(url, "row") ?: queryParam(url, "whichrow")
        val qty = queryParam(url, "quantity") ?: "1"
        when {
            action.equals("buyitem", ignoreCase = true) ||
                action.equals("buying", ignoreCase = true) -> {
                updateSessionLog(
                    "buy $qty from $whichshop" + (row?.let { " row $it" } ?: ""),
                    sessionLogger,
                )
            }
            else -> updateSessionLog(coinmasterVisitMessage(whichshop), sessionLogger)
        }
        return true
    }

    /** Track C — friendlier coinmaster/shop visit lines. */
    private fun coinmasterVisitMessage(whichshop: String): String = when (whichshop.lowercase()) {
        "hermit" -> "Visiting the Hermit"
        "bountyhunterhunter", "bhh" -> "Visiting the Bounty Hunter Hunter"
        "dimemaster" -> "Visiting the Dimemaster"
        "quartersmaster" -> "Visiting the Quartersmaster"
        "swagger" -> "Visiting The Swagger Shop"
        "fdkol" -> "Visiting the F.D.K.O.L. Quartermaster"
        "driparmory" -> "Visiting the Drip Armory"
        "mrstore", "mrreplica" -> "Visiting Mr. Store"
        "september" -> "Visiting Sept-Ember Censer"
        "spinmasterlathe" -> "Visiting SpinMaster Lathe"
        "trapper" -> "Visiting the Trapper"
        "junkmagazine" -> "Visiting Junk Magazine"
        "flowertradein" -> "Visiting Flower Trade-In"
        "armoryandleggery", "armory" -> "Visiting Armory and Leggery"
        "conmerch" -> "Visiting the Merch Table"
        "crimbo25_sammy" -> "Visiting Crimbo Sammy"
        "bacon", "arcade", "kiwi", "mystic", "shore", "5dprinter",
        "piraterealm", "blackmarket", "alliedhq", "chroner" ->
            "Visiting $whichshop"
        else -> "Visiting $whichshop"
    }

    private fun registerPlace(
        url: String,
        sessionLogger: SessionLogger?,
        preferences: Preferences?,
    ): Boolean {
        if (!url.startsWith("place.php")) return false
        if (SorceressLairSync.registerRequest(url, preferences, sessionLogger)) return true
        val place = queryParam(url, "whichplace") ?: return true
        if (place == "scrapheap" && ScrapheapRequest.registerRequest(url, sessionLogger)) {
            return true
        }
        if (place == "spelunky") {
            val action = queryParam(url, "action").orEmpty()
            updateSessionLog(
                if (action.isNotEmpty()) "spelunky $action" else "Visiting Spelunky",
                sessionLogger,
            )
            return true
        }
        if (place.startsWith("batman")) {
            return BatFellowRequest.registerRequest(url, preferences, sessionLogger)
        }
        val action = queryParam(url, "action").orEmpty()
        val message = placeVisitMessage(place, action)
        if (message != null) {
            updateSessionLog(message, sessionLogger)
            return true
        }
        if (action.isNotEmpty()) {
            updateSessionLog("place.php?whichplace=$place&action=$action", sessionLogger)
        } else {
            updateSessionLog("Visiting $place", sessionLogger)
        }
        return true
    }

    private fun placeVisitMessage(place: String, action: String): String? = when (place) {
        "8bit" -> if (action == "8treasure") "Visiting The Treasure House" else null
        "airport_hot" -> when (action) {
            "airport4_zone1" -> "Visiting The Towering Inferno Discotheque"
            "airport4_questhub" -> "Visiting The WLF Bunker"
            else -> null
        }
        "airport_sleaze" -> when (action) {
            "airport1_npc1" -> "Talking to Buff Jimmy"
            "airport1_npc2" -> "Talking to Taco Dan"
            "airport1_npc3" -> "Talking to Broden"
            else -> null
        }
        "mountains" -> if (action == "mts_melvin") "Talking to Melvin" else null
        "town_right" -> when (action) {
            "townright_lrr" -> "Visiting The League of Loathing Radio"
            "townright_vote" -> "Voting Booth"
            else -> null
        }
        "town_wrong" -> when (action) {
            "townwrong_precinct" -> "Visiting the 11th Precinct Headquarters"
            "townwrong_tunnel" -> "Entering the Tunnel of L.O.V.E."
            else -> null
        }
        "twitch" -> "Visiting Time Twitching Tower"
        "forestvillage" -> if (action == "fv_scientist") "Visiting the Scientist" else null
        "manor4" -> if (action == "manor4_chamber") "Visiting the Summoning Chamber" else null
        "chateau" -> when {
            action.contains("rest") -> "rest (chateau)"
            action.contains("painting") -> "chateau painting"
            action.isNotEmpty() -> "chateau $action"
            else -> "Visiting Chateau Mantegna"
        }
        "campaway" -> when {
            action.contains("tent") || action.contains("rest") -> "rest (campaway)"
            action.contains("cloud") || action.contains("sky") -> "campaway cloud"
            action.isNotEmpty() -> "campaway $action"
            else -> "Visiting Getaway Campsite"
        }
        "falloutshelter" -> when {
            action.contains("vault1") -> "fallout vault1"
            action.contains("vault3") -> "fallout vault3"
            action.contains("vault_term") -> "fallout terminal"
            action.isNotEmpty() -> "falloutshelter $action"
            else -> "Visiting Fallout Shelter"
        }
        "scrapheap" -> when {
            action.contains("chronolith") -> "scrapheap chronolith"
            action.contains("scavenge") -> "scrapheap scavenge"
            action.isNotEmpty() -> "scrapheap $action"
            else -> "Visiting Scrapheap"
        }
        "rabbithole" -> "Visiting Rabbit Hole"
        "arcade" -> "Visiting Game Grid Arcade"
        "kgb" -> if (action.isNotEmpty()) "kgb $action" else "Visiting KGB"
        else -> null
    }

    private fun registerChoice(
        url: String,
        sessionLogger: SessionLogger?,
        preferences: Preferences?,
        formFields: Map<String, String>,
    ): Boolean {
        if (!url.startsWith("choice.php")) return false
        val choice = queryParam(url, "whichchoice")
            ?: formFields["whichchoice"]
            ?: return true
        val option = queryParam(url, "option")
            ?: formFields["option"]
            ?: "0"
        val choiceId = choice.toIntOrNull() ?: 0
        val optionId = option.toIntOrNull() ?: 0

        if (SorceressLairSync.registerChoice(choiceId, optionId, preferences, sessionLogger)) {
            return true
        }

        if (YouRobotManager.registerRequest(url, sessionLogger, preferences)) {
            return true
        }

        // Track C — high-traffic IoTM choice logs
        val iotm = choiceIotmMessage(choiceId, option, url, formFields)
        if (iotm != null) {
            updateSessionLog(iotm, sessionLogger)
            return true
        }

        if (option == "0" || !url.contains("option=")) {
            updateSessionLog("choice $choice", sessionLogger)
        } else {
            updateSessionLog("choice $choice/$option", sessionLogger)
        }
        return true
    }

    // ── Track C: choice IoTM messages ────────────────────────────────────────

    private fun choiceIotmMessage(
        choice: Int,
        option: String,
        url: String,
        formFields: Map<String, String>,
    ): String? = when (choice) {
        585 -> "Clan swimming pool" // ClanLoungeSwimmingPoolRequest
        720 -> { // Florist Friar
            when (option) {
                "1" -> {
                    val plant = queryParam(url, "plant") ?: formFields["plant"]
                    if (plant != null) "Planting florist plant #$plant" else "Florist Friar plant"
                }
                "2" -> {
                    val dig = queryParam(url, "plnti") ?: formFields["plnti"]
                    if (dig != null) "Digging up plant # ${dig.toIntOrNull()?.plus(1) ?: dig}"
                    else "Florist Friar dig"
                }
                else -> "Visiting Florist Friar"
            }
        }
        1069 -> { // Numberology
            val seed = queryParam(url, "num") ?: formFields["num"]
            if (seed != null) "Calculate $seed with Calculate the Universe" else "Calculate the Universe"
        }
        1074 -> "Sausage grinder"
        1104, 1105 -> "Tea Tree"
        1079 -> { // Tea Tree
            if (option != "0") "Potted Tea Tree: option $option" else "Visiting Potted Tea Tree"
        }
        1195, 1196 -> "Time-Spinner"
        1217 -> { // Sweet Synthesis (desktop SweetSynthesisRequest)
            val id1 = queryParam(url, "itemid1") ?: formFields["itemid1"]
            val id2 = queryParam(url, "itemid2") ?: formFields["itemid2"]
            val count = queryParam(url, "qty") ?: formFields["qty"] ?: "1"
            if (id1 != null && id2 != null) {
                val n1 = id1.toIntOrNull()?.let { ItemDatabase.getById(it)?.name } ?: "item #$id1"
                val n2 = id2.toIntOrNull()?.let { ItemDatabase.getById(it)?.name } ?: "item #$id2"
                "synthesize $count $n1, $n2"
            } else {
                "Sweet Synthesis"
            }
        }
        1222 -> "Tunnel of L.O.V.E."
        1256, 1257, 1258, 1259 -> "Burning Leaves"
        1260, 1262 -> "Villain Lair"
        1310 -> "God Lobster boon"
        1331, 1332 -> "Sweet Synthesis"
        1340 -> {
            val cmd = queryParam(url, "input") ?: formFields["input"]
            if (cmd != null) "Source Terminal: $cmd" else "Source Terminal"
        }
        1388, 1389, 1390, 1391 -> "Beach Comb"
        1399 -> { // Deck of Every Card
            val card = queryParam(url, "which") ?: formFields["which"]
            if (card != null) "Draw $card from Deck of Every Card" else "Visiting Deck of Every Card"
        }
        1410, 1420 -> { // Cargo Cultist Shorts (desktop choice 1420)
            val pocket = queryParam(url, "pocket") ?: formFields["pocket"]
            if (pocket != null && pocket != "0") "picking pocket $pocket"
            else "Inspecting Cargo Cultist Shorts"
        }
        1435, 1436 -> "Sausage O-Matic"
        1448, 1449 -> "Hashing Vise"
        1463 -> "Reminisce with Combat Lover's Locket"
        1489, 1490, 1491 -> "Mayam Calendar"
        1510, 1511 -> "Autumnaton"
        1523 -> "WereProfessor research"
        1466 -> "Umbrella"
        1551 -> "Hashing Vise"
        1558 -> "Foresee"
        // Wax / meteoroid / newspaper / wool creation choices
        1002 -> "Burning Newspaper"
        1018, 1019 -> "Metal Meteoroid"
        1054, 1055 -> "Wax Glob"
        1116 -> "Walford"
        1493, 1494 -> "Grubby Wool"
        else -> null
    }

    // ── Track B: use / equip / skill / camp / create ─────────────────────────

    private fun registerUseItem(url: String, sessionLogger: SessionLogger?): Boolean {
        when {
            url.startsWith("inv_eat.php") -> {
                val id = whichItem(url) ?: return true
                updateSessionLog("eat ${itemLabel(id, quantity(url))}", sessionLogger)
                return true
            }
            url.startsWith("inv_booze.php") -> {
                val id = whichItem(url) ?: return true
                updateSessionLog("drink ${itemLabel(id, quantity(url))}", sessionLogger)
                return true
            }
            url.startsWith("inv_spleen.php") -> {
                val id = whichItem(url) ?: return true
                updateSessionLog("chew ${itemLabel(id, quantity(url))}", sessionLogger)
                return true
            }
            url.startsWith("inv_use.php") ||
                url.startsWith("multiuse.php") ||
                (url.startsWith("inventory.php") && url.contains("action=use")) -> {
                if (url.contains("action=closetpull") || url.contains("action=closetpush")) {
                    return registerCloset(url, sessionLogger)
                }
                val id = whichItem(url) ?: return false
                updateSessionLog("use ${itemLabel(id, quantity(url))}", sessionLogger)
                return true
            }
        }
        return false
    }

    private fun registerEquipment(url: String, sessionLogger: SessionLogger?): Boolean {
        if (url.startsWith("bedazzle.php")) {
            updateSessionLog("bedazzle", sessionLogger)
            return true
        }
        if (url.contains("action=holster")) {
            updateSessionLog("holster", sessionLogger)
            return true
        }
        if (!url.startsWith("inv_equip.php")) return false
        val outfit = queryParam(url, "whichoutfit")
        if (outfit != null) {
            updateSessionLog(
                if (outfit == "last") "outfit last" else "outfit $outfit",
                sessionLogger,
            )
            return true
        }
        val action = queryParam(url, "action").orEmpty()
        val id = whichItem(url)
        when {
            action.contains("unequip", ignoreCase = true) ->
                updateSessionLog("unequip ${id?.let { itemLabel(it, 1) } ?: "item"}", sessionLogger)
            id != null ->
                updateSessionLog("equip ${itemLabel(id, 1)}", sessionLogger)
            else ->
                updateSessionLog("equip", sessionLogger)
        }
        return true
    }

    private fun registerSkill(url: String, sessionLogger: SessionLogger?): Boolean {
        val isSkillsCast = url.startsWith("skills.php") && (
            url.contains("action=Skillz", ignoreCase = true) ||
                url.contains("action=useskill", ignoreCase = true)
            )
        if (!url.startsWith("runskillz.php") && !isSkillsCast) {
            // campground.php?action=bookshelf also casts
            if (!(url.startsWith("campground.php") && url.contains("preaction=summon"))) {
                return false
            }
        }
        val skillId = queryParam(url, "whichskill")?.toIntOrNull()
            ?: queryParam(url, "skillid")?.toIntOrNull()
            ?: return url.startsWith("runskillz.php")
        val name = SkillDefinitionDatabase.getById(skillId)?.name ?: "skill #$skillId"
        val qty = queryParam(url, "quantity") ?: queryParam(url, "bufftimes") ?: "1"
        updateSessionLog("cast $qty $name", sessionLogger)
        return true
    }

    private fun registerCampground(url: String, sessionLogger: SessionLogger?): Boolean {
        if (!url.startsWith("campground.php")) return false
        val action = queryParam(url, "action") ?: queryParam(url, "preaction") ?: return true
        when (action) {
            "rest", "take tent" -> updateSessionLog("rest", sessionLogger)
            "garden" -> updateSessionLog("harvest garden", sessionLogger)
            "workshed" -> updateSessionLog("visit workshed", sessionLogger)
            "telescope", "telescopehigh", "telescopelow" ->
                updateSessionLog("telescope", sessionLogger)
            "portal", "portalvisit" -> updateSessionLog("el vibrato portal", sessionLogger)
            "bookshelf", "bookshelf_adv" -> updateSessionLog("bookshelf", sessionLogger)
            "dripfaucet" -> updateSessionLog("drip faucet", sessionLogger)
            "pizza", "makepizza" -> updateSessionLog("pizza", sessionLogger)
            else -> updateSessionLog("campground $action", sessionLogger)
        }
        return true
    }

    private fun registerCreate(url: String, sessionLogger: SessionLogger?): Boolean {
        if (!url.contains("craft.php", ignoreCase = true) &&
            !url.contains("mode=cook") &&
            !url.contains("mode=cocktail") &&
            !url.contains("mode=smith") &&
            !url.contains("mode=jewelry") &&
            !url.contains("mode=combine")
        ) {
            return false
        }
        val mode = queryParam(url, "mode") ?: "craft"
        val qty = queryParam(url, "qty") ?: queryParam(url, "quantity") ?: "1"
        val a = queryParam(url, "a")?.toIntOrNull()
        val b = queryParam(url, "b")?.toIntOrNull()
        val target = queryParam(url, "target")
        val command = when {
            a != null && b != null && a > 0 && b > 0 ->
                "create $qty ${itemLabel(a, 1)} + ${itemLabel(b, 1)} ($mode)"
            a != null && a > 0 ->
                "create $qty ${itemLabel(a, qty.toIntOrNull() ?: 1)} ($mode)"
            target != null ->
                "create $qty ${target.toIntOrNull()?.let { itemLabel(it, qty.toIntOrNull() ?: 1) } ?: target}"
            else -> "create $qty $mode"
        }
        updateSessionLog(command, sessionLogger)
        return true
    }

    /** Desktop CreateItemRequest.getCreationCommand-style label for ASH/CLI. */
    fun getCreationCommand(itemName: String, quantity: Int = 1): String =
        "create $quantity $itemName"

    // ── Track D: long-tail ───────────────────────────────────────────────────

    private fun registerLongTail(
        url: String,
        sessionLogger: SessionLogger?,
        preferences: Preferences?,
    ): Boolean {
        when {
            url.startsWith("closet.php") ||
                (url.startsWith("inventory.php") &&
                    (url.contains("action=closetpull") || url.contains("action=closetpush"))) ->
                return registerCloset(url, sessionLogger)

            url.startsWith("storage.php") -> {
                val action = queryParam(url, "action").orEmpty()
                when {
                    action.contains("take", ignoreCase = true) ||
                        action.contains("pull", ignoreCase = true) ->
                        updateSessionLog("pull from storage", sessionLogger)
                    else -> updateSessionLog("Visiting Hagnk's", sessionLogger)
                }
                return true
            }

            url.startsWith("familiar.php") -> {
                val action = queryParam(url, "action").orEmpty()
                when {
                    action.contains("newfam") || queryParam(url, "newfam") != null ->
                        updateSessionLog(
                            "familiar ${queryParam(url, "whichfam") ?: queryParam(url, "newfam")}",
                            sessionLogger,
                        )
                    action.contains("putback") ->
                        updateSessionLog("familiar putback", sessionLogger)
                    action.contains("unequip") ->
                        updateSessionLog("familiar unequip", sessionLogger)
                    action.contains("lockequip") ->
                        updateSessionLog("familiar lockequip", sessionLogger)
                    action.contains("hatseat") ->
                        updateSessionLog("familiar enthrone ${queryParam(url, "famid")}", sessionLogger)
                    action.contains("backpack") ->
                        updateSessionLog("familiar bjornify ${queryParam(url, "famid")}", sessionLogger)
                    action.contains("equip") ->
                        updateSessionLog("familiar equip", sessionLogger)
                    action.contains("steal") ->
                        updateSessionLog("familiar steal", sessionLogger)
                    else -> updateSessionLog("Visiting Terrarium", sessionLogger)
                }
                return true
            }

            url.startsWith("clan_stash.php") -> {
                updateSessionLog("clan stash", sessionLogger)
                return true
            }
            url.startsWith("clan_rumpus.php") || url.startsWith("clan_viplounge.php") -> {
                updateSessionLog("clan lounge", sessionLogger)
                return true
            }

            url.startsWith("town_fleamarket.php") &&
                FleaMarketRequest.registerRequest(url, sessionLogger) -> {
                return true
            }
            url.startsWith("town_sellflea.php") &&
                FleaMarketSellRequest.registerRequest(url, sessionLogger) -> {
                return true
            }

            url.startsWith("mallstore.php") || url.startsWith("mall.php") ||
                url.startsWith("managestore.php") -> {
                updateSessionLog("mall", sessionLogger)
                return true
            }

            url.startsWith("sendmessage.php") || url.startsWith("sendkmail.php") -> {
                updateSessionLog("send message", sessionLogger)
                return true
            }

            url.startsWith("uneffect.php") ||
                (url.startsWith("skills.php") && url.contains("action=uneffect")) -> {
                val effect = queryParam(url, "whicheffect") ?: "?"
                updateSessionLog("uneffect $effect", sessionLogger)
                return true
            }

            url.startsWith("wand.php") || url.contains("action=zap") -> {
                val id = whichItem(url)
                updateSessionLog("zap ${id?.let { itemLabel(it, 1) } ?: "item"}", sessionLogger)
                return true
            }

            url.startsWith("cook.php") || url.startsWith("cocktail.php") ||
                url.startsWith("smith.php") -> {
                updateSessionLog("craft station", sessionLogger)
                return true
            }

            url.startsWith("ascensionhistory.php") -> {
                updateSessionLog("ascension history", sessionLogger)
                return true
            }

            url.startsWith("ascend.php") || url.startsWith("afterlife.php") -> {
                updateSessionLog("ascension", sessionLogger)
                return true
            }

            url.startsWith("guild.php") -> {
                val action = queryParam(url, "action").orEmpty()
                updateSessionLog(
                    if (action.isNotEmpty()) "guild $action" else "Visiting Guild",
                    sessionLogger,
                )
                return true
            }

            url.startsWith("mrstore.php") -> {
                updateSessionLog("Mr. Store", sessionLogger)
                return true
            }

            url.startsWith("volcanoisland.php") -> {
                val adventureCount = preferences?.getInt("turnsPlayed", 0) ?: 0
                if (!VolcanoIslandRequest.registerRequest(url, sessionLogger, adventureCount = adventureCount)) {
                    updateSessionLog("volcano island", sessionLogger)
                }
                return true
            }

            url.startsWith("trophy.php") -> {
                if (!TrophyHutRequest.registerRequest(url, sessionLogger)) {
                    updateSessionLog("trophy hut", sessionLogger)
                }
                return true
            }

            url.startsWith("cafe.php") -> {
                updateSessionLog("cafe", sessionLogger)
                return true
            }

            url.startsWith("arcade.php") || url.contains("whichplace=arcade") -> {
                if (!ArcadeRequest.registerRequest(url, sessionLogger)) {
                    updateSessionLog("arcade", sessionLogger)
                }
                return true
            }

            url.startsWith("dwarffactory.php") -> {
                DwarfFactoryRequest.registerRequest(url, sessionLogger)
                return true
            }

            url.startsWith("dwarfcontraption.php") -> {
                DwarfContraptionRequest.registerRequest(url, sessionLogger)
                return true
            }

            url.startsWith("desc_item.php") || url.startsWith("desc_effect.php") ||
                url.startsWith("desc_skill.php") -> {
                // description fetches — silent
                return true
            }
        }
        return false
    }

    private fun registerCloset(url: String, sessionLogger: SessionLogger?): Boolean {
        when {
            url.contains("action=closetpull") || url.contains("action=takeclosetitem") ->
                updateSessionLog("closet pull", sessionLogger)
            url.contains("action=closetpush") || url.contains("action=putclosetitem") ->
                updateSessionLog("closet push", sessionLogger)
            else -> updateSessionLog("Visiting Closet", sessionLogger)
        }
        return true
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private fun stripBase(url: String): String {
        var s = url.trim()
        val markers = listOf("://www.kingdomofloathing.com/", "://kingdomofloathing.com/")
        for (m in markers) {
            val i = s.indexOf(m)
            if (i >= 0) {
                s = s.substring(i + m.length)
                break
            }
        }
        if (s.startsWith("/")) s = s.drop(1)
        return s
    }

    private fun queryParam(url: String, key: String): String? {
        val qIndex = url.indexOf('?')
        val query = if (qIndex >= 0) url.substring(qIndex + 1) else return null
        for (part in query.split('&')) {
            val eq = part.indexOf('=')
            if (eq < 0) continue
            if (part.substring(0, eq).equals(key, ignoreCase = true)) {
                return decode(part.substring(eq + 1))
            }
        }
        return null
    }

    private fun decode(s: String): String =
        s.replace('+', ' ').replace(Regex("%([0-9A-Fa-f]{2})")) {
            it.groupValues[1].toInt(16).toChar().toString()
        }

    private fun whichItem(url: String): Int? =
        queryParam(url, "whichitem")?.toIntOrNull()
            ?: queryParam(url, "itemid")?.toIntOrNull()

    private fun quantity(url: String): Int =
        queryParam(url, "quantity")?.toIntOrNull()
            ?: queryParam(url, "qty")?.toIntOrNull()
            ?: queryParam(url, "ajax")?.let { 1 }
            ?: 1

    private fun itemLabel(itemId: Int, count: Int): String {
        val name = ItemDatabase.getById(itemId)?.name ?: "item #$itemId"
        return if (count <= 1) name else "$count $name"
    }
}
