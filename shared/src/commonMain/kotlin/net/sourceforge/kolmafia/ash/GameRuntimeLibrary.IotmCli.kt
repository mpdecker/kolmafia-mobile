package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.quest.AutumnatonChoiceSync
import net.sourceforge.kolmafia.quest.BurningLeavesChoiceSync
import net.sourceforge.kolmafia.quest.CatBurglarChoiceSync
import net.sourceforge.kolmafia.quest.ColdMedicineChoiceSync
import net.sourceforge.kolmafia.quest.FloristFriarChoiceSync
import net.sourceforge.kolmafia.quest.LeprecondoChoiceSync
import net.sourceforge.kolmafia.quest.MummeryChoiceSync
import net.sourceforge.kolmafia.quest.PerilChoiceSync
import net.sourceforge.kolmafia.quest.TimeSpinnerChoiceSync
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.session.HeistManager

private fun GameRuntimeLibrary.invQty(itemId: Int): Int =
    inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

/** Phases 1011–1022 — daily IoTM facility CLIs. */
internal fun GameRuntimeLibrary.cliAutumnaton(parameters: String, print: (String) -> Unit) {
    val prefs = preferences
    if (prefs?.getBoolean("hasAutumnaton", false) != true && invQty(AutumnatonChoiceSync.AUTUMNATON_ITEM_ID) <= 0) {
        print("You need an autumn-aton.")
        return
    }
    val params = parameters.trim().split(Regex("\\s+"), limit = 2).filter { it.isNotEmpty() }
    when (params.getOrNull(0)?.lowercase().orEmpty()) {
        "" -> autumnatonStatus(print)
        "locations" -> autumnatonLocations(print)
        "upgrade" -> autumnatonUpgrade(print)
        "send" -> autumnatonSend(params.getOrNull(1).orEmpty(), print)
        else -> print("Usage: autumnaton [send <location> | upgrade | locations]")
    }
}

private fun GameRuntimeLibrary.autumnatonStatus(print: (String) -> Unit) {
    val prefs = preferences ?: return
    val location = prefs.getString("autumnatonQuestLocation", "")
    if (location.isBlank()) {
        if (invQty(AutumnatonChoiceSync.AUTUMNATON_ITEM_ID) <= 0) {
            print("Your autumn-aton is in an unknown location.")
        } else {
            print("Your autumn-aton is ready to be sent somewhere.")
            val upgrades = prefs.getString("autumnatonUpgrades", "")
            if (upgrades.isNotBlank()) print("Known upgrades: $upgrades")
        }
    } else {
        print("Your autumn-aton is plundering in $location.")
        val questTurn = prefs.getInt("autumnatonQuestTurn", 0)
        val turnsPlayed = character?.state?.value?.turnsPlayed ?: 0
        val remaining = questTurn - turnsPlayed
        if (remaining > 0) {
            val s = if (remaining == 1) "" else "s"
            print("Your autumn-aton will return after $remaining turn$s.")
        } else {
            print("Your autumn-aton will return after your next combat.")
        }
    }
}

private fun GameRuntimeLibrary.autumnatonLocations(print: (String) -> Unit) {
    val upgrades = preferences?.getString("autumnatonUpgrades", "").orEmpty()
    print("Autumn-aton upgrades: ${upgrades.ifBlank { "(none recorded)" }}")
    print("Use: autumnaton send <location>")
}

private fun GameRuntimeLibrary.autumnatonUpgrade(print: (String) -> Unit) {
    if (invQty(AutumnatonChoiceSync.AUTUMNATON_ITEM_ID) <= 0) {
        print("Your autumn-aton is away.")
        return
    }
    val useReq = useItemRequest ?: run {
        print("Use item request is not available.")
        return
    }
    val choice = choiceRequest ?: run {
        print("Choice request is not available.")
        return
    }
    runBlocking {
        useReq.use(AutumnatonChoiceSync.AUTUMNATON_ITEM_ID, 1).exceptionOrNull()?.let {
            print(it.message ?: "Failed to use autumn-aton.")
            return@runBlocking
        }
        choice.choose(AutumnatonChoiceSync.AUTUMNATON_CHOICE, 1)
            .onSuccess { (html, _) ->
                AutumnatonChoiceSync.apply(
                    AutumnatonChoiceSync.AUTUMNATON_CHOICE,
                    1,
                    html,
                    preferences,
                )
                print("Attempted autumn-aton upgrade.")
            }
            .onFailure { print(it.message ?: "Upgrade failed.") }
    }
}

private fun GameRuntimeLibrary.autumnatonSend(locationQuery: String, print: (String) -> Unit) {
    if (locationQuery.isBlank()) {
        print("Where do you want to send the little guy?")
        return
    }
    if (invQty(AutumnatonChoiceSync.AUTUMNATON_ITEM_ID) <= 0) {
        print("Your autumn-aton is away.")
        return
    }
    val zone = AdventureDatabase.getByName(locationQuery)
        ?: AdventureDatabase.search(locationQuery).singleOrNull()
    if (zone == null) {
        print("I don't understand where $locationQuery is.")
        return
    }
    val snarf = zone.snarfblat
    if (snarf.isNullOrBlank()) {
        print("${zone.locationName} is not a valid location")
        return
    }
    val useReq = useItemRequest ?: run {
        print("Use item request is not available.")
        return
    }
    val choice = choiceRequest ?: run {
        print("Choice request is not available.")
        return
    }
    print("Sending autumn-aton to ${zone.locationName}")
    runBlocking {
        useReq.use(AutumnatonChoiceSync.AUTUMNATON_ITEM_ID, 1).exceptionOrNull()?.let {
            print(it.message ?: "Failed to use autumn-aton.")
            return@runBlocking
        }
        choice.choose(
            AutumnatonChoiceSync.AUTUMNATON_CHOICE,
            2,
            mapOf("heythereprogrammer" to snarf),
        ).onSuccess { (html, url) ->
            AutumnatonChoiceSync.apply(
                AutumnatonChoiceSync.AUTUMNATON_CHOICE,
                2,
                html,
                preferences,
                choiceUrl = url,
                turnsPlayed = character?.state?.value?.turnsPlayed ?: 0,
            )
            val sentTo = preferences?.getString("autumnatonQuestLocation", "").orEmpty()
            if (sentTo.isBlank()) {
                print("Failed to send autumnaton to ${zone.locationName}. Is it accessible?")
            } else {
                print("Sent autumn-aton to $sentTo.")
            }
        }.onFailure { print(it.message ?: "Send failed.") }
    }
}

internal fun GameRuntimeLibrary.cliCmc(parameters: String, print: (String) -> Unit) {
    val prefs = preferences
    val workshed = CampgroundItemSync.currentWorkshedItemId(prefs)
    if (workshed != ColdMedicineChoiceSync.CABINET_ITEM_ID &&
        invQty(ColdMedicineChoiceSync.CABINET_ITEM_ID) <= 0
    ) {
        print("Your Cold Medicine Cabinet is not installed as your workshed.")
        return
    }
    val arg = parameters.trim().lowercase()
    when {
        arg.isEmpty() || arg == "status" -> {
            val consults = prefs?.getInt(ColdMedicineChoiceSync.CONSULTS_PREF, 0) ?: 0
            val remaining = (ColdMedicineChoiceSync.MAX_CONSULTS - consults).coerceAtLeast(0)
            val equipment = prefs?.getInt(ColdMedicineChoiceSync.EQUIPMENT_PREF, 0) ?: 0
            val next = prefs?.getInt(ColdMedicineChoiceSync.NEXT_CONSULT_PREF, 0) ?: 0
            print("Cold Medicine Cabinet consults used: $consults / ${ColdMedicineChoiceSync.MAX_CONSULTS} ($remaining left)")
            print("Equipment taken: $equipment / ${ColdMedicineChoiceSync.MAX_EQUIPMENT}")
            if (next > 0) print("Next consult available at turn $next")
            print("Usage: cmc [equipment|food|booze|potion|pill|plan]")
        }
        arg == "plan" -> {
            val env = prefs?.getString("lastCombatEnvironments", "").orEmpty()
            print("Recent combat environments: ${env.ifBlank { "(none)" }}")
            print("Collect with: cmc equipment|food|booze|potion|pill")
        }
        arg in CMC_OPTIONS -> cmcCollect(CMC_OPTIONS.indexOf(arg) + 1, arg, print)
        else -> print("Usage: cmc [equipment|food|booze|potion|pill|plan]")
    }
}

private val CMC_OPTIONS = listOf("equipment", "food", "booze", "potion", "pill")

private fun GameRuntimeLibrary.cmcCollect(option: Int, label: String, print: (String) -> Unit) {
    val prefs = preferences ?: run {
        print("Preferences are not available.")
        return
    }
    val consults = prefs.getInt(ColdMedicineChoiceSync.CONSULTS_PREF, 0)
    if (consults >= ColdMedicineChoiceSync.MAX_CONSULTS) {
        print("No Cold Medicine Cabinet consults remaining today.")
        return
    }
    val client = httpClient ?: run {
        print("HTTP client is not available.")
        return
    }
    val choice = choiceRequest ?: run {
        print("Choice request is not available.")
        return
    }
    runBlocking {
        CampgroundRequest(client).visitAction("workshed").exceptionOrNull()?.let {
            print(it.message ?: "Failed to visit workshed.")
            return@runBlocking
        }
        choice.choose(ColdMedicineChoiceSync.CHOICE_ID, option)
            .onSuccess { (html, _) ->
                ColdMedicineChoiceSync.applyVisit(ColdMedicineChoiceSync.CHOICE_ID, html, prefs)
                ColdMedicineChoiceSync.apply(
                    ColdMedicineChoiceSync.CHOICE_ID,
                    option,
                    prefs,
                    turnsPlayed = character?.state?.value?.turnsPlayed ?: 0,
                )
                print("Collected CMC $label.")
            }
            .onFailure { print(it.message ?: "CMC collect failed.") }
    }
}

internal fun GameRuntimeLibrary.cliLeaves(parameters: String, print: (String) -> Unit) {
    val guideOk = invQty(GUIDE_TO_BURNING_LEAVES_ID) > 0 ||
        preferences?.getBoolean(CampgroundItemSync.CAMPGROUND_HAS_BURNING_LEAVES_PREF, false) == true
    if (!guideOk && invQty(BurningLeavesChoiceSync.INFLAMMABLE_LEAF_ID) <= 0) {
        print("You must have a Pile of Burning Leaves to have a pile in which you can burn leaves.")
        return
    }
    val trimmed = parameters.trim()
    if (trimmed.isEmpty()) {
        val burned = preferences?.getInt("_leavesBurned", 0) ?: 0
        val jumped = preferences?.getBoolean("_leavesJumped", false) == true
        print("Leaves burned today: $burned")
        print("Jumped in flames: $jumped")
        print("Usage: leaves <count> | <item|monster name>")
        return
    }
    val leaves = trimmed.toIntOrNull() ?: LEAF_OUTCOME_NAMES.entries
        .firstOrNull { it.key.contains(trimmed, ignoreCase = true) || trimmed.contains(it.key, ignoreCase = true) }
        ?.value
    if (leaves == null || leaves <= 0) {
        print("What is a $trimmed? Try a number of leaves, or a relevant item or monster.")
        return
    }
    if (invQty(BurningLeavesChoiceSync.INFLAMMABLE_LEAF_ID) < leaves) {
        print("You don't have that many leaves.")
        return
    }
    val choice = choiceRequest ?: run {
        print("Choice request is not available.")
        return
    }
    val client = httpClient
    runBlocking {
        if (client != null) {
            CampgroundRequest(client).visitAction("leaves").exceptionOrNull()
        }
        choice.choose(
            BurningLeavesChoiceSync.CHOICE_ID,
            1,
            mapOf("leaves" to leaves.toString()),
        ).onSuccess { (html, url) ->
            BurningLeavesChoiceSync.apply(
                BurningLeavesChoiceSync.CHOICE_ID,
                html,
                preferences,
                choiceUrl = url,
                consumeItem = { id, qty -> inventoryManager?.consumeItemLocally(id, qty) },
            )
            print("Burned $leaves leaves.")
        }.onFailure { print(it.message ?: "Burning leaves failed.") }
    }
}

private const val GUIDE_TO_BURNING_LEAVES_ID = 11340

private val LEAF_OUTCOME_NAMES = mapOf(
    "flaming leaflet" to 11,
    "flaming monstera" to 111,
    "leaviathan" to 666,
    "day shortener" to 222,
    "leaf lasso" to 69,
    "leafcutter ant egg" to 6666,
    "leaf tattoo" to 11111,
)

internal fun GameRuntimeLibrary.cliTeatree(parameters: String, print: (String) -> Unit) {
    val trimmed = parameters.trim()
    if (trimmed.isEmpty()) {
        val used = preferences?.getBoolean("_pottedTeaTreeUsed", false) == true
        print("Potted tea tree used today: $used")
        print("Usage: teatree shake | random | <tea name>")
        return
    }
    if (preferences?.getBoolean("_pottedTeaTreeUsed", false) == true) {
        print("You have already harvested tea from your potted tea tree today.")
        return
    }
    val lower = trimmed.lowercase()
    val shake = lower.startsWith("shake") || lower.startsWith("random")
    val teaId = if (shake) null else resolveTeaItemId(trimmed)
    if (!shake && teaId == null) {
        print("I don't know how to harvest $trimmed")
        return
    }
    val request = pottedTeaTreeRequest ?: run {
        print("Tea tree HTTP is not available.")
        return
    }
    runBlocking {
        if (shake) {
            request.shake()
                .onSuccess { print("Shook the potted tea tree.") }
                .onFailure { print(it.message ?: "Tea harvest failed.") }
        } else {
            request.select(teaId!!)
                .onSuccess { print("Harvested tea item #$teaId.") }
                .onFailure { print(it.message ?: "Tea harvest failed.") }
        }
    }
}

internal fun GameRuntimeLibrary.cliForesee(parameters: String, print: (String) -> Unit) {
    val trimmed = parameters.trim()
    val used = preferences?.getInt("_perilsForeseen", 0) ?: 0
    if (trimmed.isEmpty()) {
        print("Perils foreseen today: $used / ${PerilChoiceSync.MAX_PERILS}")
        print("Usage: foresee <player id>")
        return
    }
    if (used >= PerilChoiceSync.MAX_PERILS) {
        print("You can only foresee peril thrice daily.")
        return
    }
    val perilId = trimmed.toIntOrNull()
    if (perilId == null || perilId <= 0) {
        print("Usage: foresee <player id>")
        return
    }
    val request = foreseeRequest ?: run {
        print("Foresee HTTP is not available.")
        return
    }
    runBlocking {
        request.foresee(perilId)
            .onSuccess { print("Foreseeing peril for $perilId.") }
            .onFailure { print(it.message ?: "Foresee failed.") }
    }
}

private fun resolveTeaItemId(query: String): Int? {
    val q = query.trim().lowercase()
    ItemDatabase.getByName(q)?.id?.let { return it }
    ItemDatabase.getByName("$q tea")?.id?.let { return it }
    val matches = ItemDatabase.all()
        .filter { it.name.contains("tea", ignoreCase = true) }
        .filter { it.name.contains(q, ignoreCase = true) }
    return matches.singleOrNull()?.id
}

internal fun GameRuntimeLibrary.cliMummery(parameters: String, print: (String) -> Unit) {
    if (invQty(MUMMING_TRUNK_ID) <= 0) {
        print("You need a mumming trunk first.")
        return
    }
    val familiarName = character?.state?.value?.familiarName.orEmpty()
    if (familiarName.isBlank()) {
        print("You need to have a familiar to put a costume on.")
        return
    }
    val trimmed = parameters.trim().lowercase()
    if (trimmed.isEmpty()) {
        val uses = preferences?.getString("_mummeryUses", "").orEmpty()
        print("Mummery uses today: ${uses.ifBlank { "(none)" }}")
        print("Usage: mummery muscle|myst|moxie|hp|mp|item|meat|#")
        return
    }
    val choiceNum = when {
        trimmed.toIntOrNull() in 1..7 -> trimmed.toInt()
        "meat" in trimmed -> 1
        trimmed == "mp" || trimmed.startsWith("mp ") -> 2
        "mus" in trimmed -> 3
        "item" in trimmed -> 4
        "mys" in trimmed -> 5
        trimmed == "hp" || trimmed.startsWith("hp ") -> 6
        "mox" in trimmed -> 7
        else -> 0
    }
    if (choiceNum !in 1..7) {
        print("$parameters is not a valid option.")
        return
    }
    val used = preferences?.getString("_mummeryUses", "").orEmpty()
    if (used.contains(choiceNum.toString())) {
        print("You have already applied the $parameters costume today.")
        return
    }
    val useReq = useItemRequest ?: run {
        print("Use item request is not available.")
        return
    }
    val choice = choiceRequest ?: run {
        print("Choice request is not available.")
        return
    }
    runBlocking {
        useReq.use(MUMMING_TRUNK_ID, 1).exceptionOrNull()?.let {
            print(it.message ?: "Failed to use mumming trunk.")
            return@runBlocking
        }
        choice.choose(MummeryChoiceSync.CHOICE_ID, choiceNum)
            .onSuccess { (html, _) ->
                MummeryChoiceSync.apply(
                    MummeryChoiceSync.CHOICE_ID,
                    choiceNum,
                    html,
                    preferences,
                    familiarRace = familiarName,
                )
                print("Applied mummery costume $choiceNum to $familiarName.")
            }
            .onFailure { print(it.message ?: "Mummery failed.") }
    }
}

private const val MUMMING_TRUNK_ID = 9592

internal fun GameRuntimeLibrary.cliTimespinner(parameters: String, print: (String) -> Unit) {
    if (invQty(TIME_SPINNER_ID) <= 0) {
        print("You don't have a Time-Spinner.")
        return
    }
    val prefs = preferences
    val minutes = prefs?.getInt(TimeSpinnerChoiceSync.MINUTES_PREF, 0) ?: 0
    val parts = parameters.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    when (parts.getOrNull(0)?.lowercase().orEmpty()) {
        "" -> {
            print("Time-Spinner minutes used: $minutes")
            print("Usage: timespinner list food | list monsters | eat <food> | prank <target>")
        }
        "list" -> {
            val kind = parts.getOrNull(1)?.lowercase().orEmpty()
            when (kind) {
                "food", "" -> {
                    val foods = prefs?.getString("_timeSpinnerFoodAvailable", "").orEmpty()
                    if (foods.isBlank()) print("No Time-Spinner foods recorded today.")
                    else foods.split(",").filter { it.isNotBlank() }.forEach { id ->
                        val name = ItemDatabase.getItemName(id.toIntOrNull() ?: 0).ifBlank { id }
                        print(name)
                    }
                }
                "monsters", "monster" -> {
                    val filter = parts.drop(2).joinToString(" ")
                    val monsters = prefs?.getString("_timeSpinnerRecentMonsters", "").orEmpty()
                    monsters.split(",").filter { it.isNotBlank() }
                        .filter { filter.isBlank() || it.contains(filter, ignoreCase = true) }
                        .forEach(print)
                    if (monsters.isBlank()) print("No recent Time-Spinner monsters recorded.")
                }
                else -> print("Usage: timespinner list food | list monsters [<filter>]")
            }
        }
        "eat" -> {
            if (minutes > 7) {
                print("You don't have enough time to eat a past meal.")
                return
            }
            val foodQuery = parts.drop(1).joinToString(" ")
            if (foodQuery.isBlank()) {
                print("What food do you want to eat?")
                return
            }
            val food = ItemDatabase.getByName(foodQuery)
                ?: ItemDatabase.all().firstOrNull { it.name.equals(foodQuery, ignoreCase = true) }
                ?: ItemDatabase.all().firstOrNull { it.name.contains(foodQuery, ignoreCase = true) }
            if (food == null) {
                print("That isn't a valid food.")
                return
            }
            val available = prefs?.getString("_timeSpinnerFoodAvailable", "").orEmpty()
                .split(",").map { it.trim() }.filter { it.isNotBlank() }
            if (food.id.toString() !in available) {
                print("You haven't eaten this yet today.")
                return
            }
            val useReq = useItemRequest ?: run {
                print("Use item request is not available.")
                return
            }
            val choice = choiceRequest ?: run {
                print("Choice request is not available.")
                return
            }
            runBlocking {
                useReq.use(TIME_SPINNER_ID, 1).exceptionOrNull()?.let {
                    print(it.message ?: "Failed to use Time-Spinner.")
                    return@runBlocking
                }
                // Desktop: choice 1195 option 2 (meditate) then food pick — thin path via option 2 + id
                choice.choose(TimeSpinnerChoiceSync.SPINNING, 2).exceptionOrNull()?.let {
                    print(it.message ?: "Time-Spinner meditate failed.")
                    return@runBlocking
                }
                choice.choose(
                    TimeSpinnerChoiceSync.SPINNING,
                    1,
                    mapOf("foodid" to food.id.toString()),
                ).onSuccess { (_, url) ->
                    TimeSpinnerChoiceSync.apply(
                        TimeSpinnerChoiceSync.SPINNING,
                        4,
                        preferences,
                        url,
                    )
                    print("Attempted Time-Spinner eat of ${food.name}.")
                }.onFailure { print(it.message ?: "Time-Spinner eat failed.") }
            }
        }
        "prank" -> {
            val target = parts.getOrNull(1).orEmpty()
            if (target.isBlank()) {
                print("Who do you want to prank?")
                return
            }
            val useReq = useItemRequest ?: run {
                print("Use item request is not available.")
                return
            }
            val choice = choiceRequest ?: run {
                print("Choice request is not available.")
                return
            }
            val msg = parameters.substringAfter("msg=", "").trim().ifBlank { "Hi" }
            runBlocking {
                useReq.use(TIME_SPINNER_ID, 1).exceptionOrNull()?.let {
                    print(it.message ?: "Failed to use Time-Spinner.")
                    return@runBlocking
                }
                choice.choose(TimeSpinnerChoiceSync.SPINNING, 5).exceptionOrNull()
                choice.choose(
                    TimeSpinnerChoiceSync.TIME_PRANK,
                    1,
                    mapOf("pwd" to "", "toucher" to target, "message" to msg),
                ).onSuccess { (html, url) ->
                    TimeSpinnerChoiceSync.apply(
                        TimeSpinnerChoiceSync.TIME_PRANK,
                        1,
                        preferences,
                        url,
                        html,
                    )
                    print("Pranked $target.")
                }.onFailure { print(it.message ?: "Prank failed.") }
            }
        }
        else -> print("Usage: timespinner list food | list monsters | eat <food> | prank <target>")
    }
}

private const val TIME_SPINNER_ID = 9104

internal fun GameRuntimeLibrary.cliFlorist(parameters: String, print: (String) -> Unit) {
    if (preferences?.getBoolean("floristFriarAvailable", false) != true) {
        print("You don't have a Florist Friar")
        return
    }
    val trimmed = parameters.trim()
    if (trimmed.isEmpty()) {
        print("Usage: florist plant <plant name>")
        return
    }
    if (!trimmed.lowercase().startsWith("plant ")) {
        print("Usage: florist plant <plant name>")
        return
    }
    val plantName = trimmed.substring(6).trim()
    val plantId = FLORIST_PLANTS.entries
        .firstOrNull { it.key.equals(plantName, ignoreCase = true) }
        ?.value
        ?: FLORIST_PLANTS.entries.firstOrNull { it.key.contains(plantName, ignoreCase = true) }?.value
    if (plantId == null) {
        print("Unrecognized plant: $plantName")
        return
    }
    val choice = choiceRequest ?: run {
        print("Choice request is not available.")
        return
    }
    val client = httpClient
    runBlocking {
        client?.let { CampgroundRequest(it).visitAction("floristfriar").exceptionOrNull() }
        // Desktop posts choice 720 option 1 with plant=
        choice.choose(
            FloristFriarChoiceSync.CHOICE_ID,
            1,
            mapOf("plant" to plantId.toString()),
        ).onSuccess { (html, url) ->
            FloristFriarChoiceSync.apply(FloristFriarChoiceSync.CHOICE_ID, url, html, preferences)
            print("Planted $plantName.")
        }.onFailure { print(it.message ?: "Florist plant failed.") }
    }
}

private val FLORIST_PLANTS = mapOf(
    "Rabid Dogwood" to 1, "Rutabeggar" to 2, "Rad-ish Radish" to 3, "Artichoker" to 4,
    "Smoke-ra" to 5, "Skunk Cabbage" to 6, "Deadly Cinnamon" to 7, "Celery Stalker" to 8,
    "Lettuce Spray" to 9, "Seltzer Watercress" to 10, "War Lily" to 11, "Stealing Magnolia" to 12,
    "Canned Spinach" to 13, "Impatiens" to 14, "Spider Plant" to 15, "Red Fern" to 16,
    "BamBOO!" to 17, "Arctic Moss" to 18, "Aloe Guv'nor" to 19, "Pitcher Plant" to 20,
    "Blustery Puffball" to 21, "Horn of Plenty" to 22, "Wizard's Wig" to 23, "Shuffle Truffle" to 24,
    "Dis Lichen" to 25, "Loose Morels" to 26, "Foul Toadstool" to 27, "Chillterelle" to 28,
    "Portlybella" to 29, "Max Headshroom" to 30, "Spankton" to 31, "Kelptomaniac" to 32,
    "Crookweed" to 33, "Electric Eelgrass" to 34, "Duckweed" to 35, "Orca Orchid" to 36,
    "Sargassum" to 37, "Sub-Sea Rose" to 38, "Snori" to 39, "Up Sea Daisy" to 40,
)

internal fun GameRuntimeLibrary.cliLeprecondo(parameters: String, print: (String) -> Unit) {
    if (invQty(LEPRECONDO_ID) <= 0) {
        print("You need a Leprecondo.")
        return
    }
    val prefs = preferences
    val params = parameters.trim().split(Regex("\\s+"), limit = 2).filter { it.isNotEmpty() }
    when (params.getOrNull(0)?.lowercase().orEmpty()) {
        "" -> {
            val installed = prefs?.getString("leprecondoInstalled", "").orEmpty()
            print("Installed furniture ids: ${installed.ifBlank { "(none)" }}")
            val rearrangements = prefs?.getInt("_leprecondoRearrangements", 0) ?: 0
            print("Rearrangements used today: $rearrangements / 3")
        }
        "available" -> {
            val discovered = prefs?.getString("leprecondoDiscovered", "").orEmpty()
            if (discovered.isBlank()) print("No discovered furniture recorded.")
            else discovered.split(",").filter { it.isNotBlank() }.forEach { id ->
                print(LEPRECONDO_FURNITURE[id.toIntOrNull()] ?: "furniture #$id")
            }
        }
        "missing" -> {
            val discovered = prefs?.getString("leprecondoDiscovered", "").orEmpty()
                .split(",").mapNotNull { it.toIntOrNull() }.toSet()
            LEPRECONDO_FURNITURE.filterKeys { it !in discovered }.values.forEach(print)
        }
        "furnish" -> {
            if ((prefs?.getInt("_leprecondoRearrangements", 0) ?: 0) >= 3) {
                print("All leprecondo rearrangements used today")
                return
            }
            val furnitureArg = params.getOrNull(1).orEmpty()
            val names = furnitureArg.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            if (names.size != 4) {
                print("Usage: leprecondo furnish a,b,c,d")
                return
            }
            val ids = names.map { name ->
                LEPRECONDO_FURNITURE.entries.firstOrNull {
                    it.value.equals(name, ignoreCase = true) ||
                        it.value.contains(name, ignoreCase = true)
                }?.key
            }
            if (ids.any { it == null }) {
                print("Unrecognised furniture name in: $furnitureArg")
                return
            }
            val useReq = useItemRequest ?: run {
                print("Use item request is not available.")
                return
            }
            val choice = choiceRequest ?: run {
                print("Choice request is not available.")
                return
            }
            print("Furnishing Leprecondo with ${names.joinToString(", ")}")
            runBlocking {
                useReq.use(LEPRECONDO_ID, 1).exceptionOrNull()?.let {
                    print(it.message ?: "Failed to use Leprecondo.")
                    return@runBlocking
                }
                choice.choose(
                    LeprecondoChoiceSync.CHOICE_ID,
                    1,
                    mapOf(
                        "r0" to ids[0]!!.toString(),
                        "r1" to ids[1]!!.toString(),
                        "r2" to ids[2]!!.toString(),
                        "r3" to ids[3]!!.toString(),
                    ),
                ).onSuccess {
                    prefs?.setInt(
                        "_leprecondoRearrangements",
                        (prefs.getInt("_leprecondoRearrangements", 0) + 1).coerceAtMost(3),
                    )
                    prefs?.setString("leprecondoInstalled", ids.joinToString(",") { it!!.toString() })
                    print("Leprecondo furnished.")
                }.onFailure { print(it.message ?: "Furnish failed.") }
            }
        }
        else -> print("Usage: leprecondo [furnish a,b,c,d | available | missing]")
    }
}

private const val LEPRECONDO_ID = 11861

private val LEPRECONDO_FURNITURE = mapOf(
    1 to "buckets of concrete", 2 to "thrift store oil painting", 3 to "boxes of old comic books",
    4 to "second-hand hot plate", 5 to "beer cooler", 6 to "free mattress",
    7 to "gigantic chess set", 8 to "UltraDance karaoke machine", 9 to "cupcake treadmill",
    10 to "beer pong table", 11 to "padded weight bench", 12 to "internet-connected laptop",
    13 to "sous vide laboratory", 14 to "programmable blender", 15 to "sensory deprivation tank",
    16 to "fruit-smashing robot", 17 to "ManCave™ sports bar set", 18 to "couch and flatscreen",
    19 to "kegerator", 20 to "fine upholstered dining table set", 21 to "whiskeybed",
    22 to "high-end home workout system", 23 to "complete classics library",
    24 to "ultimate retro game console", 25 to "Omnipot", 26 to "fully-stocked wet bar",
    27 to "four-poster bed",
)

internal fun GameRuntimeLibrary.cliHeist(parameters: String, print: (String) -> Unit) {
    val famState = familiarManager?.state?.value
    val hasCatBurglar = famState?.ownedFamiliars?.any {
        it.race.equals("Cat Burglar", ignoreCase = true) || it.id == CAT_BURGLAR_FAMILIAR_ID
    } == true ||
        famState?.activeFamiliar?.let {
            it.race.equals("Cat Burglar", ignoreCase = true) || it.id == CAT_BURGLAR_FAMILIAR_ID
        } == true ||
        character?.state?.value?.familiarId == CAT_BURGLAR_FAMILIAR_ID ||
        character?.state?.value?.familiarName?.equals("Cat Burglar", ignoreCase = true) == true
    if (!hasCatBurglar) {
        val complete = preferences?.getInt(CatBurglarChoiceSync.HEISTS_PREF, 0) ?: 0
        if (complete <= 0 && parameters.isBlank()) {
            print("You don't have a Cat Burglar")
            return
        }
    }
    val client = httpClient
    val trimmed = parameters.trim()
    if (client == null) {
        val complete = preferences?.getInt(CatBurglarChoiceSync.HEISTS_PREF, 0) ?: 0
        print("Cat Burglar heists completed today: $complete")
        if (trimmed.isNotEmpty()) print("HTTP client is not available for heisting.")
        return
    }
    runBlocking {
        val manager = HeistManager(client)
        if (trimmed.isEmpty()) {
            manager.getHeistTargets()
                .onSuccess { data ->
                    print("You have ${data.heists} heists.")
                    for ((monster, items) in data.heistables) {
                        print("From ${monster.pronoun} ${monster.name}:")
                        items.forEach { print("  ${it.name}") }
                    }
                }
                .onFailure { print(it.message ?: "Failed to load heist targets.") }
        } else {
            val countMatch = Regex("""^(\d+)\s+(.+)$""").find(trimmed)
            val count = countMatch?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
            val itemQuery = countMatch?.groupValues?.getOrNull(2) ?: trimmed
            val item = ItemDatabase.getByName(itemQuery)
                ?: ItemDatabase.all().firstOrNull { it.name.equals(itemQuery, ignoreCase = true) }
                ?: ItemDatabase.all().firstOrNull { it.name.contains(itemQuery, ignoreCase = true) }
            if (item == null) {
                print("What item is $itemQuery?")
                return@runBlocking
            }
            manager.heist(count, item.id)
                .onSuccess {
                    CatBurglarChoiceSync.apply(
                        CatBurglarChoiceSync.CHOICE_ID,
                        1,
                        preferences,
                    )
                    print("Heisted ${if (count > 1) "$count " else ""}${item.name}")
                }
                .onFailure { print(it.message ?: "Could not heist ${item.name}") }
        }
    }
}

private const val CAT_BURGLAR_FAMILIAR_ID = 267
