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
import net.sourceforge.kolmafia.request.DreadsylvaniaRequest
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

    /** DI: resolve familiar id → display string (race or "name, the race"). Default: id only. */
    var familiarDisplayById: (Int) -> String? = { null }

    /** DI: resolve item id → item name. Default: ItemDatabase lookup. */
    var itemNameById: (Int) -> String? = { id -> ItemDatabase.getItemName(id).ifBlank { null } }

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

        // Phase 5751–5758: high-traffic visit messages (with or without query string)
        if (urlString.startsWith("questlog.php", ignoreCase = true)) {
            updateSessionLog("Visiting the Quest Log", sessionLogger)
            wasLastRequestSimple = false
            return true
        }
        if (urlString.startsWith("charsheet.php", ignoreCase = true)) {
            updateSessionLog("Visiting the Character Sheet", sessionLogger)
            wasLastRequestSimple = false
            return true
        }
        if (urlString.startsWith("manageclan.php", ignoreCase = true)) {
            updateSessionLog("Visiting Clan Management", sessionLogger)
            wasLastRequestSimple = false
            return true
        }
        if (urlString.startsWith("clan_hall.php", ignoreCase = true)) {
            // Redirect target from showclan.php — silently claimed
            wasLastRequestSimple = false
            return true
        }
        if (urlString.startsWith("standard.php", ignoreCase = true)) {
            // Standard restriction check — silently claimed (desktop: no session-log)
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
        "airport_spooky" -> if (action == "airport2_radio") "Using the radio on Conspiracy Island" else null
        "airport_spooky_bunker" -> when (action) {
            "si_controlpanel" -> "Manipulating the Control Panel in the Conspiracy Island bunker"
            else -> null
        }
        "airport_stench" -> when (action) {
            "airport3_tunnels" -> "Visiting the Maintenance Tunnels"
            "airport3_kiosk" -> "Visiting the Employee Assignment Kiosk"
            else -> null
        }
        "canadia" -> when (action) {
            "lc_mcd" -> "Visiting the Super-Secret Canadian Mind Control Device"
            "lc_marty" -> "Talking to Marty"
            else -> null
        }
        "crashsite" -> if (action == "crash_ship") "Visiting the Crashed Spaceship" else null
        "crimbo2016" -> when (action) {
            "crimbo16_trailer" -> "Visiting Uncle Crimbo's Mobile Home"
            "crimbo16_tammy" -> "Visiting Tammy's Tent"
            "crimbo16_guy2" -> "Visiting A Ninja Snowman"
            "crimbo16_guy2a" -> "Visiting An Elf Boot-Polisher"
            "crimbo16_guy3" -> "Visiting A Hobo"
            "crimbo16_guy3a" -> "Visiting An Elf Cook"
            "crimbo16_guy4" -> "Visiting A Bugbear"
            "crimbo16_guy4a" -> "Visiting An Elf Reindeerstler"
            "crimbo16_guy5" -> "Visiting A Hippy"
            "crimbo16_guy5a" -> "Visiting An Elf Bearddresser"
            "crimbo16_guy6" -> "Visiting A Frat Boy"
            "crimbo16_guy6a" -> "Visiting An Elf Haberdasher"
            else -> null
        }
        "crimbo17_silentnight" -> when (action) {
            "crimbo17_bossfight" -> "Mime-Head Building"
            "crimbo17_warehouse" -> "The Warehouse"
            else -> null
        }
        "desertbeach" -> when (action) {
            "db_gnasir" -> "Talking to Gnasir"
            "db_nukehouse" -> "Visiting the Ruined House"
            else -> null
        }
        "dinorf" -> when (action) {
            "dinorf_hunter" -> "Visiting the Dino World Game Warden's Shed"
            "dinorf_chaos" -> "Visiting the Dino World Visitor's Center"
            "dinorf_owner" -> "Visiting the Dino World Owner's Trailer"
            else -> null
        }
        "dripfacility" -> when (action) {
            "drip_jeremy" -> "Talking to Jeremy Science"
            else -> null
        }
        "exploathing" -> if (action == "expl_council") "Visiting The Council" else null
        "exploathing_beach" -> if (action == "expl_gnasir") "Talking to Gnasir" else null
        "forestvillage" -> when (action) {
            "fv_scientist" -> "Visiting the Scientist"
            "fv_mystic" -> "Talking to the Crackpot Mystic"
            else -> null
        }
        "greygoo" -> if (action == "goo_prism") "Visiting a Prism of Goo" else null
        "highlands" -> if (action == "highlands_dude") "Talking to the Highland Lord" else null
        "ioty2014_candy" -> if (action == "witch_house") "Visiting the Candy Witch's House" else null
        "ioty2014_rumple" -> if (action == "workshop") "Visiting Rumplestiltskin's Workshop" else null
        "manor1" -> if (action == "manor1_ladys") "Talking to Lady Spookyraven" else null
        "manor2" -> if (action == "manor2_ladys") "Talking to Lady Spookyraven" else null
        "manor3" -> if (action == "manor3_ladys") "Talking to Lady Spookyraven" else null
        "manor4" -> when (action) {
            "manor4_chamber" -> "Visiting the Summoning Chamber"
            else -> if (action.startsWith("manor4_chamberwall")) "Inspecting the Suspicious Masonry" else null
        }
        "mclargehuge" -> when (action) {
            "trappercabin" -> "Visiting the Trapper"
            "cloudypeak" -> "Ascending the Mist-Shrouded Peak"
            else -> null
        }
        "monorail" -> when (action) {
            "monorail_lyle" -> "Visiting Lyle, LyleCo CEO"
            "monorail_downtown" -> "Train to Downtown"
            else -> null
        }
        "mountains" -> when (action) {
            "mts_melvin" -> "Talking to Melvign the Gnome"
            "mts_caveblocked" -> "Entering the Nemesis Cave"
            else -> null
        }
        "nemesiscave" -> when (action) {
            "nmcave_rubble" -> "Examining the rubble in the Nemesis Cave"
            "nmcave_boss" -> "Confronting your Nemesis"
            else -> null
        }
        "northpole" -> when (action) {
            "np_bonfire" -> "Visiting the Bonfire"
            "np_sauna" -> "Entering the Sauna"
            "np_foodlab" -> "Entering the Food Lab"
            "np_boozelab" -> "Entering the Nog Lab"
            "np_spleenlab" -> "Entering the Chem Lab"
            "np_toylab" -> "Entering the Gift Fabrication Lab"
            else -> null
        }
        "palindome" -> when (action) {
            "pal_drlabel", "pal_droffice" -> "Visiting Dr. Awkward's office"
            "pal_mrlabel", "pal_mroffice" -> "Visiting Mr. Alarm's office"
            else -> null
        }
        "plains" -> when (action) {
            "garbage_grounds" -> "Inspecting the Giant Pile of Coffee Grounds"
            else -> null
        }
        "pyramid" -> if (action == "pyramid_control") "Visiting the Pyramid Control Room" else null
        "rabbithole" -> when {
            action == "rabbithole_teaparty" -> "Visiting the Mad Tea Party"
            else -> "Visiting Rabbit Hole"
        }
        "sea_oldman" -> if (action == "oldman_oldman") "Talking to the Old Man" else null
        "snojo" -> if (action == "snojo_controller") "Visiting Snojo Control Console" else null
        "spacegate" -> when (action) {
            "sg_requisition" -> "Visiting Spacegate Equipment Requisition"
            "sg_tech" -> "Visiting Spacegate R&D"
            "sg_Terminal" -> "Visiting the Spacegate Terminal"
            "sg_vaccinator" -> "Visiting the Spacegate Vaccination Machine"
            else -> null
        }
        "spacegate_portable" -> "Visiting your portable Spacegate"
        "speakeasy" -> when (action) {
            "olivers_pooltable" -> "Visiting the Pool Table"
            "olivers_sot" -> "Talking to the Milky-Eyed Sot"
            "olivers_sign" -> "Looking at the conspicuous plaque"
            else -> null
        }
        "thesea" -> if (action == "thesea_left2") "Visiting the Swimmy Little Fishes and Such" else null
        "town" -> if (action == "town_oddjobs") "Visiting the Odd Jobs Board" else null
        "town_market" -> if (action == "town_bookmobile") "Visiting The Bookmobile" else null
        "town_right" -> when (action) {
            "townright_lrr" -> "Visiting The League of Loathing Radio"
            "townright_vote" -> "Voting Booth"
            "town_horsery" -> "Visiting The Horsery"
            else -> null
        }
        "town_wrong" -> when (action) {
            "townwrong_precinct" -> "Visiting the 11th Precinct Headquarters"
            "townwrong_tunnel" -> "Entering the Tunnel of L.O.V.E."
            "townwrong_boxingdaycare" -> "Visiting the Boxing Daycare"
            else -> null
        }
        "twitch" -> when (action) {
            "twitch_votingbooth" -> "Visiting the Voting / Phone Booth"
            "twitch_dancave1" -> "Visiting Caveman Dan's Cave"
            "twitch_shoerepair" -> "Visiting the Shoe Repair Store"
            "twitch_colosseum" -> "Visiting the Chariot-Racing Colosseum"
            "twitch_survivors" -> "Visiting the Post-Apocalyptic Survivor Encampment"
            "twitch_bank" -> "Visiting the Third Four-Fifths Bank of the West"
            "twitch_boat2" -> "Visiting The Pinta"
            "twitch_boat3" -> "Visiting The Santa Claus"
            "" -> "Visiting Time Twitching Tower"
            else -> null
        }
        "wereprof_cottage" -> when (action) {
            "wereprof_bookshelf" -> "Read"
            "wereprof_researchbench" -> "Visiting the Research Bench"
            "wereprof_sleepfree", "wereprof_sleep" -> "Sleep"
            else -> null
        }
        "wildfire_camp" -> null  // silently claimed — actions logged by WildfireCampRequest
        "woods" -> when (action) {
            "woods_smokesignals" -> "Investigating the Smoke Signals"
            "woods_hippy" -> "Talking to that Hippy"
            "woods_dakota_anim", "woods_dakota" -> "Talking to Dakota Fanning"
            else -> null
        }
        "chateau" -> when {
            action.startsWith("chateau_desk1") -> "Collecting Meat from Swiss piggy bank"
            action.startsWith("chateau_desk2") -> "Collecting potions from continental juice bar"
            action.startsWith("chateau_desk3") -> "Collecting pens from fancy stationery set"
            action.startsWith("chateau_desk") -> "Collecting swag from the item on your desk"
            action.startsWith("chateau_rest") || action.startsWith("cheateau_rest") ->
                "Rest in your bed in the Chateau"
            action.startsWith("chateau_nightstand") || action.startsWith("chateau_ceiling") -> null
            action.startsWith("chateau_painting") -> null
            action.isNotEmpty() -> "chateau $action"
            else -> "Visiting Chateau Mantegna"
        }
        "campaway" -> when {
            action == "campaway_sky" -> "Gazing at the Stars"
            action.startsWith("campaway_tent") -> "Rest in your campaway tent"
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
        "edbase" -> when (action) {
            "edbase_book" -> "Visiting The Book of the Undying"
            "edbase_door" -> "Visiting The Servants' Quarters"
            "" -> null // bare visit — claimed by fallback
            else -> null
        }
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
                return registerStorage(url, sessionLogger)
            }

            url.startsWith("familiar.php") -> {
                val action = queryParam(url, "action").orEmpty()
                when {
                    action.contains("newfam") || queryParam(url, "newfam") != null -> {
                        val famId = (queryParam(url, "whichfam") ?: queryParam(url, "newfam"))
                            ?.toIntOrNull()
                        val display = famId?.let { familiarDisplayById(it) }
                        updateSessionLog("", sessionLogger)
                        updateSessionLog(
                            "familiar ${display ?: famId?.toString() ?: "unknown"}",
                            sessionLogger,
                        )
                    }
                    action.contains("putback") -> {
                        updateSessionLog("", sessionLogger)
                        updateSessionLog("familiar none", sessionLogger)
                    }
                    action.contains("unequip") -> {
                        val famId = (queryParam(url, "famid") ?: queryParam(url, "whichfam"))
                            ?.toIntOrNull()
                        val display = famId?.let { familiarDisplayById(it) }
                        updateSessionLog("", sessionLogger)
                        updateSessionLog(
                            "Unequip ${display ?: "familiar"}",
                            sessionLogger,
                        )
                    }
                    action.contains("lockequip") -> {
                        updateSessionLog("", sessionLogger)
                        updateSessionLog("familiar lockequip", sessionLogger)
                    }
                    action.contains("hatseat") -> {
                        val famId = queryParam(url, "famid")?.toIntOrNull()
                        updateSessionLog("", sessionLogger)
                        if (famId == null || famId == 0) {
                            updateSessionLog("enthrone none", sessionLogger)
                        } else {
                            val display = familiarDisplayById(famId)
                            updateSessionLog(
                                "enthrone ${display ?: famId.toString()}",
                                sessionLogger,
                            )
                        }
                    }
                    action.contains("backpack") -> {
                        val famId = queryParam(url, "famid")?.toIntOrNull()
                        updateSessionLog("", sessionLogger)
                        if (famId == null || famId == 0) {
                            updateSessionLog("bjornify none", sessionLogger)
                        } else {
                            val display = familiarDisplayById(famId)
                            updateSessionLog(
                                "bjornify ${display ?: famId.toString()}",
                                sessionLogger,
                            )
                        }
                    }
                    action.contains("equip") -> {
                        val famId = queryParam(url, "whichfam")?.toIntOrNull()
                        val itemId = queryParam(url, "whichitem")?.toIntOrNull()
                        val famDisplay = famId?.let { familiarDisplayById(it) }
                        val itemName = itemId?.let { itemNameById(it) }
                        updateSessionLog("", sessionLogger)
                        if (famDisplay != null && itemName != null) {
                            updateSessionLog(
                                "Equip $famDisplay with $itemName",
                                sessionLogger,
                            )
                        } else {
                            updateSessionLog("familiar equip", sessionLogger)
                        }
                    }
                    action.contains("steal") ->
                        updateSessionLog("familiar steal", sessionLogger)
                    else -> updateSessionLog("Visiting Terrarium", sessionLogger)
                }
                return true
            }

            url.startsWith("clan_stash.php") -> {
                return registerClanStash(url, sessionLogger)
            }
            url.startsWith("clan_rumpus.php") -> {
                return registerClanRumpus(url, sessionLogger)
            }
            url.startsWith("clan_viplounge.php") -> {
                return registerClanLounge(url, sessionLogger)
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
                url.startsWith("desc_skill.php") || url.startsWith("desc_guardian.php") -> {
                // description fetches — silent
                return true
            }

            url.startsWith("raffle.php") -> {
                val qty = queryParam(url, "quantity")?.toIntOrNull()
                val where = queryParam(url, "where")
                if (qty != null && qty > 0 && where != null) {
                    val loc = when (where) { "0" -> "inventory"; "1" -> "storage"; else -> where }
                    updateSessionLog("raffle $qty $loc", sessionLogger)
                }
                return true
            }
            url.startsWith("managecollection.php") -> return registerDisplayCase(url, sessionLogger)
            url.startsWith("spaaace.php") -> return registerSpaaace(url, sessionLogger)
            url.startsWith("volcanomaze.php") -> return registerVolcanoMaze(url, sessionLogger)
            url.startsWith("leaflet.php") -> return registerLeaflet(url, sessionLogger)
            url.startsWith("monkeycastle.php") -> return registerMom(url, sessionLogger)
            url.startsWith("hermit.php") -> { updateSessionLog("Visiting the Hermit", sessionLogger); return true }
            url.startsWith("bigisland.php") || url.startsWith("postwarisland.php") -> return registerIsland(url, sessionLogger)
            url.contains("clan_dreadsylvania.php", ignoreCase = true) -> return DreadsylvaniaRequest.registerRequest(url, sessionLogger, preferences)
            url.contains("action=changedial") || url.contains("tuneradio") -> return registerMindControl(url, sessionLogger)
        }
        return false
    }

    private fun registerCloset(url: String, sessionLogger: SessionLogger?): Boolean {
        when {
            url.contains("action=closetpull") || url.contains("action=takeclosetitem") -> {
                val items = parseTransferItems(url)
                updateSessionLog(formatTransferLog("take from closet", items), sessionLogger)
            }
            url.contains("action=closetpush") || url.contains("action=putclosetitem") -> {
                val items = parseTransferItems(url)
                updateSessionLog(formatTransferLog("add to closet", items), sessionLogger)
            }
            url.contains("action=addtakeclosetmeat") -> {
                val meat = queryParam(url, "quantity")?.toLongOrNull() ?: 0
                if (meat > 0) {
                    if (url.contains("addtake=add")) {
                        updateSessionLog("add to closet: $meat Meat", sessionLogger)
                    } else if (url.contains("addtake=take")) {
                        updateSessionLog("take from closet: $meat Meat", sessionLogger)
                    }
                }
            }
            else -> updateSessionLog("Visiting Closet", sessionLogger)
        }
        return true
    }

    // ── Group B: action-specific clan / collection / spaaace / misc session-log ──

    /** Desktop ClanLoungeRequest.registerRequest — action-specific VIP lounge lines. */
    private fun registerClanLounge(url: String, sessionLogger: SessionLogger?): Boolean {
        val action = queryParam(url, "action")
            ?: queryParam(url, "preaction")
            ?: return true // bare visit — silently claimed
        val message: String? = when {
            action.equals("poolgame", ignoreCase = true) -> {
                val stance = queryParam(url, "stance")?.toIntOrNull()
                if (stance != null) "pool game (stance $stance)" else "pool game"
            }
            action.equals("sendfax", ignoreCase = true) || action.equals("receivefax", ignoreCase = true) -> {
                val faxCmd = queryParam(url, "fax")
                if (faxCmd != null) "fax $faxCmd" else "fax ${action.removePrefix("send").removePrefix("receive").ifEmpty { action }}"
            }
            action.equals("takeshower", ignoreCase = true) -> {
                val temp = queryParam(url, "temperature")?.toIntOrNull()
                if (temp != null) "shower $temp" else "shower"
            }
            action.equals("goswimming", ignoreCase = true) -> {
                val pool = queryParam(url, "subaction")
                if (pool != null) "swimming pool $pool" else "swimming pool"
            }
            action.equals("eathotdog", ignoreCase = true) -> {
                val dog = queryParam(url, "whichdog")?.toIntOrNull()
                if (dog != null) "eat hotdog $dog" else "eat hotdog"
            }
            action.equals("hotdogsupply", ignoreCase = true) -> {
                val dog = queryParam(url, "whichdog")
                val qty = queryParam(url, "quantity") ?: "1"
                "stock Hot Dog Stand with $qty items (dog $dog)"
            }
            action.equals("unlockhotdog", ignoreCase = true) -> {
                val dog = queryParam(url, "whichdog")
                "unlock hotdog $dog"
            }
            action.equals("speakeasydrink", ignoreCase = true) -> {
                val drink = queryParam(url, "drink")
                if (drink != null) "speakeasy drink $drink" else "speakeasy drink"
            }
            action.equals("klaw", ignoreCase = true) -> "Deluxe Mr. Klaw"
            action.equals("lookingglass", ignoreCase = true) -> "looking glass"
            action.equals("crimbotree", ignoreCase = true) -> "Crimbo tree"
            else -> null
        }
        if (message != null) {
            updateSessionLog(message, sessionLogger)
        }
        return true
    }

    /** Desktop ClanRumpusRequest.registerRequest — action-specific rumpus room lines. */
    private fun registerClanRumpus(url: String, sessionLogger: SessionLogger?): Boolean {
        if (!url.startsWith("clan_rumpus.php")) return false
        when {
            url.contains("action=buychips") ->
                updateSessionLog("Buying chips from the Snack Machine in the clan rumpus room", sessionLogger)
            url.contains("preaction=ballpit") ->
                updateSessionLog("Jumping into the Awesome Ball Pit in the clan rumpus room", sessionLogger)
            url.contains("preaction=jukebox") ->
                updateSessionLog("Playing a song on the Jukebox in the clan rumpus room", sessionLogger)
            url.contains("action=click") -> {
                val spot = queryParam(url, "spot")
                val furni = queryParam(url, "furession")
                    ?: queryParam(url, "furniture")
                updateSessionLog("clan rumpus click spot=$spot furniture=$furni", sessionLogger)
            }
            else -> updateSessionLog("Visiting Clan Rumpus Room", sessionLogger)
        }
        return true
    }

    /** Desktop ClanStashRequest.registerRequest — action-specific stash lines. */
    private fun registerClanStash(url: String, sessionLogger: SessionLogger?): Boolean {
        when {
            url.contains("takegoodies") -> {
                val items = parseTransferItems(url)
                updateSessionLog(formatTransferLog("remove from stash", items), sessionLogger)
            }
            url.contains("addgoodies") -> {
                val items = parseTransferItems(url)
                updateSessionLog(formatTransferLog("add to stash", items), sessionLogger)
            }
            url.contains("action=contribute") -> {
                val meat = queryParam(url, "howmuch")?.toLongOrNull() ?: 0
                if (meat > 0) {
                    updateSessionLog("add to stash: $meat Meat", sessionLogger)
                }
            }
            else -> updateSessionLog("Visiting Clan Stash", sessionLogger)
        }
        return true
    }

    /** Desktop StorageRequest.registerRequest — action-specific storage lines. */
    private fun registerStorage(url: String, sessionLogger: SessionLogger?): Boolean {
        when {
            url.contains("action=pullall") ->
                updateSessionLog("Emptying storage", sessionLogger)
            url.contains("action=tossichor") -> {
                val qty = queryParam(url, "icession")?.toIntOrNull()
                    ?: queryParam(url, "qty")?.toIntOrNull()
                if (qty != null && qty > 0) {
                    updateSessionLog("Toss $qty eldritch ichor into the fissure", sessionLogger)
                }
            }
            url.contains("action=takemeat") -> {
                val meat = queryParam(url, "amt")?.toLongOrNull() ?: 0
                if (meat > 0) {
                    updateSessionLog("pull: $meat Meat", sessionLogger)
                }
            }
            url.contains("pull") -> {
                val items = parseTransferItems(url)
                updateSessionLog(formatTransferLog("pull", items), sessionLogger)
            }
            else -> updateSessionLog("Visiting Hagnk's", sessionLogger)
        }
        return true
    }

    /** Desktop DisplayCaseRequest.registerRequest — take/put display case lines. */
    private fun registerDisplayCase(url: String, sessionLogger: SessionLogger?): Boolean {
        when {
            url.contains("action=take") -> {
                val items = parseTransferItems(url)
                updateSessionLog(formatTransferLog("remove from display case", items), sessionLogger)
            }
            url.contains("action=put") -> {
                val items = parseTransferItems(url)
                updateSessionLog(formatTransferLog("put in display case", items), sessionLogger)
            }
            else -> updateSessionLog("Visiting Display Case", sessionLogger)
        }
        return true
    }

    /** Desktop SpaaaceRequest.registerRequest. */
    private fun registerSpaaace(url: String, sessionLogger: SessionLogger?): Boolean {
        if (url.contains("place=shop", ignoreCase = true)) return false
        val action = queryParam(url, "action")
        when {
            action == null && url.contains("place=porko") ->
                updateSessionLog("Visiting The Porko Palace", sessionLogger)
            action.equals("playporko", ignoreCase = true) ->
                updateSessionLog("Porko Game", sessionLogger)
            action == null -> {} // bare grimace/arrive — claimed silently
            else -> return false
        }
        return true
    }

    /** Desktop VolcanoMazeRequest.registerRequest. */
    private fun registerVolcanoMaze(url: String, sessionLogger: SessionLogger?): Boolean {
        when {
            url.contains("jump=1") ->
                updateSessionLog("Swimming back to shore", sessionLogger)
            url.contains("move=") -> {
                val move = queryParam(url, "move") ?: "?"
                updateSessionLog("Hopping to $move", sessionLogger)
            }
            else -> updateSessionLog("Visiting the lava maze", sessionLogger)
        }
        return true
    }

    /** Desktop LeafletRequest.registerRequest. */
    private fun registerLeaflet(url: String, sessionLogger: SessionLogger?): Boolean {
        val command = queryParam(url, "command")
        if (command != null) {
            updateSessionLog("Leaflet $command", sessionLogger)
        }
        return true
    }

    /** Desktop MomRequest.registerRequest. */
    private fun registerMom(url: String, sessionLogger: SessionLogger?): Boolean {
        val id = queryParam(url, "who")
            ?: queryParam(url, "action")?.let { Regex("""\d+""").find(it)?.value }
        if (id != null) {
            updateSessionLog("mom food $id", sessionLogger)
        }
        return true
    }

    /** Desktop MindControlRequest.registerRequest. */
    private fun registerMindControl(url: String, sessionLogger: SessionLogger?): Boolean {
        val level = queryParam(url, "level")
            ?: queryParam(url, "setting")
            ?: queryParam(url, "tuession")
        if (level != null) {
            updateSessionLog("mcd $level", sessionLogger)
            return true
        }
        return false
    }

    /** Desktop IslandRequest.registerRequest — basic claim. */
    private fun registerIsland(url: String, sessionLogger: SessionLogger?): Boolean {
        when {
            url.contains("whichcamp=1") ->
                updateSessionLog("Visiting the Dimemaster", sessionLogger)
            url.contains("whichcamp=2") ->
                updateSessionLog("Visiting the Quartersmaster", sessionLogger)
            queryParam(url, "action").equals("bossfight", ignoreCase = true) ->
                updateSessionLog("Island War Boss Fight", sessionLogger)
            url.startsWith("postwarisland.php") ->
                updateSessionLog("Visiting Post-War Island", sessionLogger)
            else -> updateSessionLog("Visiting the Mysterious Island", sessionLogger)
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
        val name = itemNameById(itemId)?.takeIf { it.isNotBlank() }
            ?: "item #$itemId"
        return if (count <= 1) name else "$count $name"
    }

    /**
     * Desktop TransferItemRequest.registerRequest — parse whichitemN / howmanyN
     * pairs (also single whichitem= / howmany= and qty/quantity fallbacks).
     * Returns list of (itemId, count) pairs.
     */
    internal fun parseTransferItems(url: String): List<Pair<Int, Int>> {
        val result = mutableListOf<Pair<Int, Int>>()
        // Try single whichitem= first (desktop single-item transfer)
        val singleId = queryParam(url, "whichitem")?.toIntOrNull()
        if (singleId != null && singleId > 0) {
            val qty = queryParam(url, "howmany")?.toIntOrNull()
                ?: queryParam(url, "qty")?.toIntOrNull()
                ?: queryParam(url, "quantity")?.toIntOrNull()
                ?: 1
            result.add(singleId to qty.coerceAtLeast(1))
            return result
        }
        // Multi-item: whichitem1= / howmany1=, whichitem2= / howmany2= …
        for (i in 1..100) {
            val itemId = queryParam(url, "whichitem$i")?.toIntOrNull() ?: break
            if (itemId <= 0) continue
            val qty = queryParam(url, "howmany$i")?.toIntOrNull()
                ?: queryParam(url, "qty$i")?.toIntOrNull()
                ?: queryParam(url, "quantity$i")?.toIntOrNull()
                ?: 1
            result.add(itemId to qty.coerceAtLeast(1))
        }
        return result
    }

    /**
     * Desktop TransferItemRequest.transferList — builds `"command: item1, N item2"`.
     * When no items are parsed falls back to plain command.
     */
    internal fun formatTransferLog(
        command: String,
        items: List<Pair<Int, Int>>,
        meat: Long = 0,
    ): String {
        val parts = mutableListOf<String>()
        for ((itemId, count) in items) {
            parts.add(itemLabel(itemId, count))
        }
        if (meat > 0) parts.add("$meat Meat")
        return if (parts.isEmpty()) command else "$command: ${parts.joinToString(", ")}"
    }
}
