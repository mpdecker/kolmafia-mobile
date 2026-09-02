package net.sourceforge.kolmafia.ash

import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.campground.CampgroundAvailability
import net.sourceforge.kolmafia.campground.CampgroundItemSync
import net.sourceforge.kolmafia.campground.GardenCropAvailability
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.BountyDatabase
import net.sourceforge.kolmafia.data.CombatDatabase
import net.sourceforge.kolmafia.data.CafeAccessibility
import net.sourceforge.kolmafia.data.ChezSnooteeDatabase
import net.sourceforge.kolmafia.data.ConcoctionConsumptionType
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConcoctionMayoQueue
import net.sourceforge.kolmafia.data.ConsumableType
import net.sourceforge.kolmafia.data.DefaultsDatabase
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.FactDatabase
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.data.HellKitchenDatabase
import net.sourceforge.kolmafia.data.HolidayNames
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.NpcStoreDatabase
import net.sourceforge.kolmafia.data.NpcStoreItem
import net.sourceforge.kolmafia.data.OceanDatabase
import net.sourceforge.kolmafia.npc.NpcBuyRequest
import net.sourceforge.kolmafia.request.StoragePullRules
import net.sourceforge.kolmafia.shop.FolderHolderAccessibility
import net.sourceforge.kolmafia.shop.NpcPurchaseAccessibility
import net.sourceforge.kolmafia.data.ZapGroupDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.KolGameHolidayCalendar
import net.sourceforge.kolmafia.data.MicroBreweryDatabase
import net.sourceforge.kolmafia.data.RestoreData
import net.sourceforge.kolmafia.data.RestoreDatabase
import net.sourceforge.kolmafia.data.RestoreType
import net.sourceforge.kolmafia.inventory.CollectionCacheSync
import net.sourceforge.kolmafia.item.RetrieveItemSimulator
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionProxy
import net.sourceforge.kolmafia.data.craftTypeDescription
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.skill.SkillState
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.maximizer.MaximizerContinuation
import net.sourceforge.kolmafia.equipment.OutfitCheckpoint
import net.sourceforge.kolmafia.mood.MoodRemovalKnownSources
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import net.sourceforge.kolmafia.request.FalloutShelterRequest
import net.sourceforge.kolmafia.request.FoldItemRequest
import net.sourceforge.kolmafia.request.LocketRequest
import net.sourceforge.kolmafia.request.RaffleRequest
import net.sourceforge.kolmafia.shop.DesertBeachAccessibility
import net.sourceforge.kolmafia.modifiers.BooleanModifier
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import kotlin.math.abs
import net.sourceforge.kolmafia.session.EventHistory
import net.sourceforge.kolmafia.session.GoalConditionParser
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.session.NumberologyManager
import net.sourceforge.kolmafia.session.GreyYouManager
import net.sourceforge.kolmafia.session.PirateInsults
import net.sourceforge.kolmafia.session.TurnCounter
import net.sourceforge.kolmafia.skill.SkillType

internal fun GameRuntimeLibrary.foldItemRequestOrNull(): FoldItemRequest? {
    val client = httpClient ?: return null
    return FoldItemRequest(
        client = client,
        useItemRequest = useItemRequest,
        choiceRequest = choiceRequest,
        equipmentRequest = equipmentRequest,
        inventoryManager = inventoryManager,
        retrieveItemService = retrieveItemService,
        recoveryManager = recoveryManager,
        character = character,
        skillManager = skillManager,
        preferences = preferences,
        gameDatabase = gameDatabase,
    )
}

internal suspend fun GameRuntimeLibrary.runFoldCli(itemName: String, rt: AshRuntimeContext) {
    val resolved = resolveCliItemName(itemName) ?: run {
        rt.print("That's not a transformable item!")
        return
    }
    val request = foldItemRequestOrNull() ?: run {
        rt.print("Fold unavailable")
        return
    }
    request.fold(resolved).fold(
        onSuccess = { body ->
            if (body == "have") {
                rt.print("Already have $itemName")
            } else {
                rt.print("Folded $itemName")
            }
        },
        onFailure = { rt.print(it.message ?: "Fold failed") },
    )
}

private fun GameRuntimeLibrary.resolveCliItemName(raw: String): Int? {
    val trimmed = raw.trim().removePrefix("\u00B6")
    trimmed.toIntOrNull()?.let { return it }
    return gameDatabase?.item(trimmed)?.id ?: ItemDatabase.getByName(trimmed)?.id
}

internal fun GameRuntimeLibrary.runWaitCli(parameters: String, quiet: Boolean, rt: AshRuntimeContext) {
    val seconds = LongTailCli.waitSeconds(parameters)
    kotlinx.coroutines.runBlocking { GameRuntimeLibrary.waitMillis(seconds * 1000L) }
    if (!quiet) rt.print("Waiting completed.")
}

internal fun GameRuntimeLibrary.runBanishesCli(rt: AshRuntimeContext) {
    val mgr = banishManager ?: run {
        rt.print("No current banishes")
        return
    }
    val turn = character?.state?.value?.currentRun ?: 0
    rt.print(mgr.formatStatus(turn))
}

internal fun GameRuntimeLibrary.runRecipeCli(cmd: String, params: String, rt: AshRuntimeContext) {
    val names = params.split(',').map { it.trim() }.filter { it.isNotBlank() }
    if (names.isEmpty()) return
    names.forEachIndexed { index, raw ->
        val prefix = if (names.size > 1) "${index + 1}. " else ""
        val itemId = resolveCliItemName(raw)
        val name = itemId?.let { gameDatabase?.item(it)?.name ?: ItemDatabase.getById(it)?.name } ?: raw
        val concoction = ConcoctionDatabase.getByResult(name)
        if (concoction == null) {
            rt.print("${prefix}This item cannot be created: $name")
            return@forEachIndexed
        }
        if (cmd.equals("ingredients", ignoreCase = true)) {
            val parts = concoction.ingredients.joinToString(", ") { ing ->
                val have = inventoryCountNamed(ing.name)
                val missing = (ing.quantity - have).coerceAtLeast(0)
                "${ing.quantity} ${ing.name}" + if (missing > 0) " (need $missing)" else ""
            }
            rt.print("$prefix$name: $parts")
        } else {
            val type = concoction.craftTypeDescription()
            val parts = concoction.ingredients.joinToString(" + ") { "${it.quantity} ${it.name}" }
            rt.print("$prefix$name ($type): $parts")
        }
    }
}

private fun GameRuntimeLibrary.inventoryCountNamed(name: String): Int {
    val id = gameDatabase?.item(name)?.id ?: ItemDatabase.getByName(name)?.id ?: return 0
    return inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
}

internal fun GameRuntimeLibrary.runOlfactCli(cmd: String, parameters: String, rt: AshRuntimeContext) {
    val prefs = preferences ?: return
    rt.print(LongTailCli.applyOlfact(cmd, parameters, prefs, ::resolveCliItemName) { id ->
        gameDatabase?.item(id)?.name ?: ItemDatabase.getById(id)?.name
    })
}

internal fun GameRuntimeLibrary.runHolidayCli(parameters: String, rt: AshRuntimeContext) {
    val trimmed = parameters.trim()
    if (trimmed.isNotEmpty()) {
        HolidayNames.setHoliday(trimmed)
    }
    val holiday = HolidayNames.getHoliday()
    rt.print(if (holiday.isBlank()) "No holiday today." else holiday)
}

internal fun GameRuntimeLibrary.runAshCli(parameters: String, quiet: Boolean, rt: AshRuntimeContext) {
    var source = parameters.trim()
    if (source.isEmpty()) return
    if (!source.endsWith(";") && !source.endsWith("}")) {
        source += ";"
    }
    val nested = AshRuntime(this)
    try {
        val nodes = AshParser().parse(source)
        val last = nodes.lastOrNull()
        val result = if (last is ExprStatement) {
            nested.execute(nodes.dropLast(1))
            nested.evalExpr(last.expr, nested.globalScope.child())
        } else {
            nested.execute(nodes)
        }
        nested.output.toString().lines().filter { it.isNotEmpty() }.forEach { rt.print(it) }
        if (!quiet) rt.print("Returned: $result")
    } catch (e: ScriptException) {
        rt.print("Script error: ${e.message}")
    }
}

internal fun GameRuntimeLibrary.runCondRefCli(rt: AshRuntimeContext) {
    rt.print("today | tomorrow is mus | mys | mox day")
    rt.print("class is [not] sauceror | etc.")
    rt.print("skill list contains | lacks <skill>")
    rt.print("level")
    rt.print("health")
    rt.print("mana")
    rt.print("meat")
    rt.print("adventures")
    rt.print("inebriety | drunkenness")
    rt.print("muscle")
    rt.print("mysticality")
    rt.print("moxie")
    rt.print("worthless item")
    rt.print("stickers")
    rt.print("<item>")
    rt.print("<effect>")
    rt.print("= == <> != < <= > >=")
    rt.print("<number>")
    rt.print("<number>% (health/mana only)")
    rt.print("<item> (qty in inventory)")
    rt.print("<effect> (turns remaining)")
}

internal fun GameRuntimeLibrary.runRepeatCli(parameters: String, rt: AshRuntimeContext) {
    val previous = previousLine ?: return
    val trimmed = parameters.trim()
    val repeatCount = if (trimmed.isEmpty()) 1 else trimmed.toIntOrNull() ?: 0
    var i = 0
    while (i < repeatCount && MaximizerContinuation.permitsContinue()) {
        rt.print("Repetition ${i + 1} of $repeatCount...")
        dispatchCli(previous, rt)
        i++
    }
}

internal fun GameRuntimeLibrary.runNumberologyCli(
    parameters: String,
    checkOnly: Boolean,
    rt: AshRuntimeContext,
) {
    val params = parameters.trim()
    val cs = character?.state?.value ?: CharacterState()
    val numeric = params.toIntOrNull() != null
    // Bare / non-numeric list (including `numberology?` with no N).
    if (!numeric) {
        val results = NumberologyManager.reverseNumberology(cs)
        var found = false
        for ((result, seed) in results) {
            val prize = NumberologyManager.numberologyPrize(result)
            if (prize != NumberologyManager.TRY_AGAIN) {
                rt.print("[$result] Calculate the Universe with $seed to get: $prize")
                found = true
            }
        }
        if (!found) rt.print("No valid results!")
        return
    }
    val result = abs(params.toInt()) % 100
    val prize = NumberologyManager.numberologyPrize(result)
    if (prize == NumberologyManager.TRY_AGAIN) {
        rt.print("Result $result is $prize")
        return
    }
    var results: Map<Int, Int>? = null
    var adventureDelta = 0
    while (adventureDelta < 100) {
        results = NumberologyManager.reverseNumberology(cs, adventureDelta, 0)
        if (results.containsKey(result)) break
        adventureDelta++
    }
    if (adventureDelta == 0) {
        if (checkOnly) {
            rt.print("\"numberology $result\" ($prize) is currently available.")
            return
        }
        val seed = results?.get(result) ?: return
        val request = numberologyRequest
            ?: httpClient?.let { net.sourceforge.kolmafia.request.NumberologyRequest(it) }
        if (request == null) {
            rt.print(prize)
            return
        }
        val outcome = kotlinx.coroutines.runBlocking {
            NumberologyManager.calculateTheUniverse(seed, request, preferences, cs)
        }
        outcome.exceptionOrNull()?.message?.let { rt.print(it) }
        return
    }
    val spleenMin = cs.spleenUsed
    val spleenMax = cs.spleenLimit
    var spleenDelta = spleenMin + 1
    while (spleenDelta <= spleenMax) {
        results = NumberologyManager.reverseNumberology(cs, 0, spleenDelta - spleenMin)
        if (results.containsKey(result)) break
        spleenDelta++
    }
    if (adventureDelta == 100 && spleenDelta >= spleenMax) {
        rt.print("Result $result not found!")
        return
    }
    val buffer = StringBuilder("\"numberology ")
    buffer.append(result)
    buffer.append("\" (")
    buffer.append(prize)
    buffer.append(") is not currently available but will be in")
    if (adventureDelta != 100) {
        buffer.append(' ')
        buffer.append(adventureDelta)
        buffer.append(" turn")
        if (adventureDelta != 1) buffer.append('s')
    }
    if (spleenDelta <= spleenMax) {
        if (adventureDelta != 100) buffer.append(" or")
        buffer.append(' ')
        buffer.append(spleenDelta - spleenMin)
        buffer.append(" spleen")
    }
    buffer.append('.')
    rt.print(buffer.toString())
}

internal fun GameRuntimeLibrary.runRaffleCli(parameters: String, rt: AshRuntimeContext) {
    val state = character?.state?.value
    if (state?.inZombiecore == true ||
        !DesertBeachAccessibility.isAvailable(state ?: CharacterState(), preferences)
    ) {
        rt.print("You can't make it to the raffle house")
        return
    }
    val tokens = parameters.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val count = tokens.firstOrNull()?.toIntOrNull() ?: 0
    val source = when {
        tokens.size <= 1 -> {
            if (state != null && (state.isHardcore || state.isInRonin)) {
                RaffleRequest.RaffleSource.STORAGE
            } else {
                RaffleRequest.RaffleSource.INVENTORY
            }
        }
        tokens[1].equals("inventory", ignoreCase = true) -> RaffleRequest.RaffleSource.INVENTORY
        tokens[1].equals("storage", ignoreCase = true) -> RaffleRequest.RaffleSource.STORAGE
        else -> {
            rt.print("You can only get meat from inventory or storage.")
            return
        }
    }
    val request = raffleRequest
        ?: httpClient?.let { RaffleRequest(it) }
        ?: return
    rt.print("Visiting the Raffle House...")
    val outcome = kotlinx.coroutines.runBlocking { request.buy(count, source) }
    outcome.exceptionOrNull()?.message?.let { rt.print(it) }
}

internal fun GameRuntimeLibrary.runGrandpaCli(parameters: String, rt: AshRuntimeContext) {
    val topic = parameters.trim()
    if (topic.isEmpty()) {
        rt.print("What do you want to ask Grandpa about?")
        return
    }
    val request = grandpaRequest
        ?: httpClient?.let { net.sourceforge.kolmafia.request.GrandpaRequest(it) }
        ?: return
    val outcome = kotlinx.coroutines.runBlocking {
        request.ask(topic, preferences, questDatabase)
    }
    outcome.exceptionOrNull()?.message?.let { rt.print(it) }
}

internal fun GameRuntimeLibrary.runDonateCli(parameters: String, rt: AshRuntimeContext) {
    val params = parameters.trim()
    if (params.isEmpty()) {
        rt.print("$params is not a statue.")
        return
    }
    val tokens = params.split(Regex("\\s+"))
    val statue = tokens[0].lowercase()
    val heroId = when {
        statue.startsWith("boris") || statue.startsWith("mus") ->
            net.sourceforge.kolmafia.request.ShrineRequest.BORIS
        statue.startsWith("jarl") || statue.startsWith("mys") ->
            net.sourceforge.kolmafia.request.ShrineRequest.JARLSBERG
        statue.startsWith("pete") || statue.startsWith("mox") ->
            net.sourceforge.kolmafia.request.ShrineRequest.PETE
        else -> {
            rt.print("$params is not a statue.")
            return
        }
    }
    val amount = tokens.getOrNull(1)?.toIntOrNull() ?: 0
    rt.print("Donating $amount to the shrine...")
    val request = shrineRequest
        ?: httpClient?.let { net.sourceforge.kolmafia.request.ShrineRequest(it) }
        ?: return
    val outcome = kotlinx.coroutines.runBlocking {
        request.donate(heroId, amount, preferences)
    }
    outcome.exceptionOrNull()?.message?.let { rt.print(it) }
        ?: rt.print("Donation complete.")
}

internal fun GameRuntimeLibrary.runSearchMallCli(parameters: String, rt: AshRuntimeContext) {
    var params = parameters.trim()
    if (params.isEmpty()) return
    var desiredLimit = 0
    if (params.contains("with limit", ignoreCase = true)) {
        val parts = params.split(Regex("(?i)with limit"), limit = 2)
        params = parts[0].trim()
        desiredLimit = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
    }
    if (params.isEmpty()) return
    val manager = mallManager ?: return
    val listings = kotlinx.coroutines.runBlocking {
        manager.searchListings(params, desiredLimit)
    }
    if (listings.isEmpty()) return
    val itemName = gameDatabase?.item(params)?.name ?: params
    val aggregated = linkedMapOf<Long, Int>()
    for (listing in listings) {
        aggregated[listing.price] = (aggregated[listing.price] ?: 0) + listing.quantity
    }
    for ((price, qty) in aggregated) {
        rt.print("  $qty @ $price meat")
    }
    // Also emit per-shop style lines when shop names exist (tests may assert price/qty).
    for (listing in listings) {
        if (listing.shopName.isNotBlank()) {
            rt.print("$itemName (${listing.quantity} @ ${listing.price}): ${listing.shopName}")
        }
    }
}

internal fun GameRuntimeLibrary.runAliasCli(parameters: String, rt: AshRuntimeContext) {
    val params = parameters.trim()
    val arrow = params.indexOf(" => ")
    if (params.isEmpty() || arrow == -1) {
        val filter = params.lowercase()
        for ((name, command) in listCliAliases()) {
            if (filter.isNotEmpty() &&
                !name.contains(filter, ignoreCase = true) &&
                !command.contains(filter, ignoreCase = true)
            ) {
                continue
            }
            rt.print("$name => $command")
        }
        return
    }
    val aliasString = params.substring(0, arrow).trim()
    val aliasCommand = params.substring(arrow + 4).trim()
    setCliAlias(aliasString, aliasCommand)
    rt.print("String successfully aliased.")
    rt.print("$aliasString => $aliasCommand")
}

internal fun GameRuntimeLibrary.runSetGetCli(command: String, parameters: String, rt: AshRuntimeContext) {
    val params = parameters.trim()
    if (command == "set" && params.startsWith("location ", ignoreCase = true)) {
        val zoneName = params.substring("location ".length).trim()
        val location = resolveLocation(zoneName) ?: return
        preferences?.setString(Preferences.LAST_LOCATION, location.name)
        kotlinx.coroutines.runBlocking { adventureRequest?.travel(location.id) }
        return
    }
    val eq = params.indexOf('=')
    if (eq == -1) {
        if (command == "set") {
            val tokens = params.split(Regex("\\s+"), limit = 2)
            if (tokens.size == 2) {
                preferences?.setString(tokens[0], tokens[1])
                return
            }
        }
        rt.print(preferences?.getString(params, "") ?: "")
        return
    }
    val name = params.substring(0, eq).trim()
    var value = params.substring(eq + 1).trim()
    if (value.startsWith("\"")) {
        value = value.substring(1, if (value.endsWith("\"")) value.length - 1 else value.length)
    }
    while (value.endsWith(";")) {
        value = value.dropLast(1).trim()
    }
    preferences?.setString(name, value)
    rt.print("$name => $value")
}

internal fun GameRuntimeLibrary.runAbortCli(message: String, rt: AshRuntimeContext) {
    val text = message.trim().ifBlank { "Script abort." }
    rt.print(text)
    MaximizerContinuation.abort()
    adventureManager?.stop()
    throw ScriptException(text)
}

internal fun GameRuntimeLibrary.runComparisonShopCli(command: String, parameters: String, rt: AshRuntimeContext) {
    val expensive = command.lowercase().removeSuffix("?") == "expensive"
    val checkOnly = command.endsWith("?")
    var params = parameters
    var commands: String? = null
    val semi = params.indexOf(';')
    if (semi != -1) {
        commands = params.substring(semi + 1).trim()
        params = params.substring(0, semi).trim()
    }
    val names = linkedSetOf<String>()
    for (raw in params.split(',')) {
        val piece = raw.trim()
        if (piece.isEmpty()) continue
        when {
            piece.startsWith("+") -> {
                val resolved = matchingItemNames(piece.substring(1).trim()).firstOrNull() ?: return
                val group = ZapGroupDatabase.groupFor(resolved)
                if (group != null) names.addAll(group) else names.add(resolved)
            }
            piece.startsWith("-") -> names.removeAll(matchingItemNames(piece.substring(1).trim()).toSet())
            else -> names.addAll(matchingItemNames(piece))
        }
    }
    if (names.isEmpty()) {
        rt.print("No matching items!")
        return
    }
    if (checkOnly) {
        names.sorted().forEach { rt.print(it) }
        return
    }
    val priced = mutableListOf<Pair<String, Long>>()
    for (name in names) {
        if (!MaximizerContinuation.permitsContinue()) return
        val item = ItemDatabase.getByName(name) ?: continue
        val price = mallPriceManager?.getMallPrice(item.id) ?: 0L
        if (!ItemDatabase.isTradeable(item.id) || price <= 0L) continue
        priced += item.name to price
    }
    if (priced.isEmpty()) {
        rt.print("No tradeable items!")
        return
    }
    val sorted = if (expensive) priced.sortedByDescending { it.second } else priced.sortedBy { it.second }
    if (commands != null) {
        dispatchCli(commands.replace(Regex("\\bit\\b"), sorted.first().first), rt)
        return
    }
    for ((name, price) in sorted) {
        rt.print("$name @ $price")
    }
}

private fun matchingItemNames(query: String): List<String> {
    if (query.isBlank()) return emptyList()
    ItemDatabase.getByName(query)?.name?.let { return listOf(it) }
    val lower = query.lowercase()
    return ItemDatabase.all().map { it.name }.filter { it.lowercase().contains(lower) }
}

internal fun GameRuntimeLibrary.runAshWikiCli(parameters: String, rt: AshRuntimeContext) {
    rt.print("https://wiki.kolmafia.us/index.php?search=${cliUrlEncode(parameters.trim())}")
}

internal fun GameRuntimeLibrary.runSafeCli(parameters: String, rt: AshRuntimeContext) {
    val zone = resolveCliLocation(parameters) ?: return
    val data = CombatDatabase.getByLocation(zone.locationName) ?: return
    val weighted = data.monsters.filter { it.weight > 0 }
    val cs = character?.state?.value
    val hitStat = cs?.buffedMusc ?: 0
    val moxie = cs?.buffedMoxie ?: 0
    val defs = weighted.map { CombatAdjustment.monsterDefense(MonsterDatabase.getByName(it.name), 0) }
    val atks = weighted.map { CombatAdjustment.monsterAttack(MonsterDatabase.getByName(it.name), 0) }
    val minHit = CombatAdjustment.hitPercent(hitStat, defs.minOrNull() ?: 0)
    val maxHit = CombatAdjustment.hitPercent(hitStat, defs.maxOrNull() ?: 0)
    val minEvade = CombatAdjustment.hitPercent(moxie, atks.minOrNull() ?: 0)
    val maxEvade = CombatAdjustment.hitPercent(moxie, atks.maxOrNull() ?: 0)
    val jump = CombatAdjustment.locationJumpChance(
        locationName = zone.locationName,
        initBonus = 0,
        initMl = 0,
        attackMl = 0,
        baseMainstat = cs?.buffedMusc ?: 0,
        resolveMonster = { MonsterDatabase.getByName(it) },
    )
    rt.print("Hit: ${minHit.toInt()}%/${maxHit.toInt()}%")
    rt.print("Evade: ${minEvade.toInt()}%/${maxEvade.toInt()}%")
    rt.print("Jump Chance: $jump%")
    if (data.combatPercent < 0) {
        rt.print("Combat Rate: No data")
    } else {
        rt.print("Combat Rate: ${data.combatPercent}%")
    }
    if (data.combatPercent > 0 && weighted.isNotEmpty()) {
        val totalWeight = weighted.sumOf { it.weight }.coerceAtLeast(1)
        val avgXp = weighted.sumOf { mw ->
            (MonsterDatabase.getByName(mw.name)?.experience ?: 0) * mw.weight
        }.toDouble() / totalWeight
        rt.print("Combat XP: ${if (avgXp == avgXp.toInt().toDouble()) avgXp.toInt().toString() else avgXp.toString()}")
    }
}

internal fun GameRuntimeLibrary.runMonstersCli(parameters: String, rt: AshRuntimeContext) {
    val zone = resolveCliLocation(parameters) ?: return
    val data = CombatDatabase.getByLocation(zone.locationName) ?: return
    val weighted = data.monsters.filter { it.weight > 0 }
    val totalWeight = weighted.sumOf { it.weight }.coerceAtLeast(1)
    val cs = character?.state?.value
    val hitStat = cs?.buffedMusc ?: 0
    val moxie = cs?.buffedMoxie ?: 0
    for (mw in weighted) {
        val monster = MonsterDatabase.getByName(mw.name)
        val appearance = mw.weight.toDouble() / totalWeight * data.combatPercent.coerceAtLeast(0)
        val atk = CombatAdjustment.monsterAttack(monster, 0)
        val def = CombatAdjustment.monsterDefense(monster, 0)
        val hp = CombatAdjustment.monsterHp(monster, 0)
        val xp = CombatAdjustment.monsterExperience(monster, 0).toInt()
        val hit = CombatAdjustment.hitPercent(hitStat, def).toInt()
        val evade = CombatAdjustment.hitPercent(moxie, atk).toInt()
        val jump = CombatAdjustment.jumpChance(
            monster,
            initBonus = 0,
            initMl = 0,
            attackMl = 0,
            baseMainstat = cs?.buffedMusc ?: 0,
        )
        rt.print("${mw.name} (${appearance.toInt()}%)")
        rt.print("Hit: $hit%, Evade: $evade%, Jump Chance: $jump%")
        rt.print("Atk: $atk, Def: $def, HP: $hp, XP: $xp")
    }
}

internal fun GameRuntimeLibrary.runAutosellCli(parameters: String) {
    val request = autosellRequest ?: return
    for (raw in parameters.split(',')) {
        val piece = raw.trim()
        if (piece.isEmpty()) continue
        val tokens = piece.split(Regex("\\s+"), limit = 2)
        val leading = tokens.firstOrNull()?.toIntOrNull()
        val itemQuery: String
        val explicitQty: Int?
        if (leading != null && tokens.size == 2) {
            explicitQty = leading
            itemQuery = tokens[1]
        } else {
            explicitQty = null
            itemQuery = piece
        }
        val itemId = resolveMallBuyItemId(itemQuery) ?: continue
        val qty = explicitQty ?: (inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0)
        if (qty <= 0) continue
        kotlinx.coroutines.runBlocking { request.autosell(itemId, qty) }
    }
}

internal fun GameRuntimeLibrary.runStashCli(direction: String, parameters: String) {
    val request = clanStashRequest ?: return
    val isTake = direction.equals("take", ignoreCase = true)
    for (raw in parameters.split(',')) {
        val piece = raw.trim()
        if (piece.isEmpty()) continue
        val tokens = piece.split(Regex("\\s+"), limit = 2)
        val leading = tokens.firstOrNull()?.toIntOrNull()
        val itemQuery: String
        val explicitQty: Int?
        if (leading != null && tokens.size == 2) {
            explicitQty = leading
            itemQuery = tokens[1]
        } else {
            explicitQty = null
            itemQuery = piece
        }
        val itemId = resolveMallBuyItemId(itemQuery) ?: continue
        val qty = explicitQty
            ?: if (isTake) {
                1
            } else {
                inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
            }
        if (qty <= 0) continue
        kotlinx.coroutines.runBlocking {
            val result = if (isTake) request.takeOut(itemId, qty) else request.putIn(itemId, qty)
            refreshStashCacheAfter(result)
        }
    }
}

internal fun GameRuntimeLibrary.runClosetMoveCli(direction: String, parameters: String) {
    val request = closetRequest ?: return
    val isTake = direction.equals("take", ignoreCase = true)
    val closetCounts = if (isTake) {
        kotlinx.coroutines.runBlocking { request.fetchContents() }
    } else {
        emptyMap()
    }
    for (raw in parameters.split(',')) {
        val piece = raw.trim()
        if (piece.isEmpty()) continue
        val tokens = piece.split(Regex("\\s+"), limit = 2)
        val leading = tokens.firstOrNull()?.toIntOrNull()
        val itemQuery: String
        val explicitQty: Int?
        if (leading != null && tokens.size == 2) {
            explicitQty = leading
            itemQuery = tokens[1]
        } else {
            explicitQty = null
            itemQuery = piece
        }
        val itemId = resolveMallBuyItemId(itemQuery) ?: continue
        val qty = explicitQty
            ?: if (isTake) {
                closetCounts[itemId] ?: 1
            } else {
                inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
            }
        if (qty <= 0) continue
        kotlinx.coroutines.runBlocking {
            val result = if (isTake) request.takeOut(itemId, qty) else request.putIn(itemId, qty)
            refreshClosetCacheAfter(result)
        }
    }
}

internal fun GameRuntimeLibrary.runDisplayMoveCli(direction: String, parameters: String) {
    val request = displayCaseRequest ?: return
    val isTake = direction.equals("take", ignoreCase = true)
    val displayCounts = if (isTake) {
        kotlinx.coroutines.runBlocking { request.fetchContents() }
    } else {
        emptyMap()
    }
    for (raw in parameters.split(',')) {
        val piece = raw.trim()
        if (piece.isEmpty()) continue
        val tokens = piece.split(Regex("\\s+"), limit = 2)
        val leading = tokens.firstOrNull()?.toIntOrNull()
        val itemQuery: String
        val explicitQty: Int?
        if (leading != null && tokens.size == 2) {
            explicitQty = leading
            itemQuery = tokens[1]
        } else {
            explicitQty = null
            itemQuery = piece
        }
        val itemId = resolveMallBuyItemId(itemQuery) ?: continue
        val qty = explicitQty
            ?: if (isTake) {
                displayCounts[itemId] ?: 1
            } else {
                inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
            }
        if (qty <= 0) continue
        kotlinx.coroutines.runBlocking {
            val result = if (isTake) request.takeOut(itemId, qty) else request.putIn(itemId, qty)
            refreshDisplayCacheAfter(result)
        }
    }
}

internal fun GameRuntimeLibrary.runStorageMoveCli(direction: String, parameters: String) {
    val request = storageRequest ?: return
    val isTake = direction.equals("take", ignoreCase = true)
    val storageCounts = if (isTake) {
        kotlinx.coroutines.runBlocking {
            request.fetchContents(character?.state?.value)
        }
    } else {
        emptyMap()
    }
    for (raw in parameters.split(',')) {
        val piece = raw.trim()
        if (piece.isEmpty()) continue
        val tokens = piece.split(Regex("\\s+"), limit = 2)
        val leading = tokens.firstOrNull()?.toIntOrNull()
        val itemQuery: String
        val explicitQty: Int?
        if (leading != null && tokens.size == 2) {
            explicitQty = leading
            itemQuery = tokens[1]
        } else {
            explicitQty = null
            itemQuery = piece
        }
        val itemId = resolveMallBuyItemId(itemQuery) ?: continue
        val qty = explicitQty
            ?: if (isTake) {
                storageCounts[itemId] ?: 1
            } else {
                inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
            }
        if (qty <= 0) continue
        kotlinx.coroutines.runBlocking {
            val result = if (isTake) request.withdraw(itemId, qty) else request.deposit(itemId, qty)
            refreshStorageCacheAfter(result)
        }
    }
}

internal fun GameRuntimeLibrary.runBuyCli(
    command: String,
    parameters: String,
    rt: AshRuntimeContext,
) {
    var params = parameters.trim()
    var usingStorage = false
    if (params.startsWith("using storage", ignoreCase = true)) {
        usingStorage = true
        params = params.substring("using storage".length).trim()
    }
    var forceMall = command.equals("mallbuy", ignoreCase = true) || usingStorage
    if (params.startsWith("from mall", ignoreCase = true)) {
        forceMall = true
        params = params.substring("from mall".length).trim()
    }
    val canInteract = StoragePullRules.canInteract(character?.state?.value)
    if (usingStorage && canInteract) {
        rt.print("You cannot purchase using storage unless you are in Hardcore or Ronin")
        return
    }
    val npcOnly = !canInteract && !forceMall && !usingStorage
    for (raw in params.split(',')) {
        val piece = raw.trim()
        if (piece.isEmpty()) continue
        val atParts = piece.split("@", limit = 2)
        val left = atParts[0].trim()
        val maxPrice = atParts.getOrNull(1)?.trim()?.toIntOrNull() ?: Int.MAX_VALUE
        val tokens = left.split(Regex("\\s+"), limit = 2)
        val leading = tokens.firstOrNull()?.toIntOrNull()
        val qty: Int
        val itemQuery: String
        if (leading != null && tokens.size == 2) {
            qty = leading
            itemQuery = tokens[1]
        } else {
            qty = 1
            itemQuery = left
        }
        if (qty == 0) {
            rt.print(
                "Purchasing 0 of an item produces surprising results, if deliberate, purchase number in inventory or don't buy!",
            )
            return
        }
        val itemId = resolveMallBuyItemId(itemQuery) ?: return
        val itemName = gameDatabase?.item(itemId)?.name
            ?: ItemDatabase.getById(itemId)?.name
            ?: itemQuery
        kotlinx.coroutines.runBlocking {
            buyOneCliItem(
                itemId = itemId,
                itemName = itemName,
                qty = qty,
                maxPrice = maxPrice,
                forceMall = forceMall,
                npcOnly = npcOnly,
                canInteract = canInteract,
            )
        }
    }
}

/** @deprecated Use [runBuyCli]; kept for call-site clarity in older tests. */
internal fun GameRuntimeLibrary.runMallBuyCli(parameters: String, rt: AshRuntimeContext) =
    runBuyCli("buy", parameters, rt)

private suspend fun GameRuntimeLibrary.buyOneCliItem(
    itemId: Int,
    itemName: String,
    qty: Int,
    maxPrice: Int,
    forceMall: Boolean,
    npcOnly: Boolean,
    canInteract: Boolean,
) {
    val entry = NpcStoreDatabase.itemEntry(itemId)
        ?: NpcStoreDatabase.storeForItem(itemName)?.let { store ->
            val price = NpcStoreDatabase.npcPrice(itemName)
            if (price <= 0) null else store to NpcStoreItem(itemName, price)
        }
    val state = character?.state?.value ?: CharacterState()
    val npcAccessible = entry != null && NpcPurchaseAccessibility.canPurchaseIgnoringMeat(
        itemId,
        entry.first,
        state,
        preferences,
    )
    val npcPrice = if (npcAccessible) entry!!.second.price else Int.MAX_VALUE
    val mallPrice = if (!npcOnly) {
        mallManager?.cheapestPrice(itemName)?.takeIf { it >= 0 }?.toInt() ?: -1
    } else {
        -1
    }
    val preferNpc = when {
        forceMall -> false
        npcOnly -> npcAccessible && npcPrice <= maxPrice
        canInteract && npcAccessible && npcPrice <= maxPrice ->
            mallPrice < 0 || npcPrice <= mallPrice
        else -> false
    }
    if (preferNpc) {
        val npc = npcBuyRequest ?: httpClient?.let { NpcBuyRequest(it) }
        if (npc != null) {
            npc.buy(entry!!.first.storeKey, itemId, qty, preferences)
            return
        }
    }
    if (npcOnly) return
    val mall = mallManager ?: return
    if (mallPrice >= 0 && mallPrice > maxPrice) return
    val char = character
    val equip = equipmentRequest
    val db = gameDatabase
    if (char != null && equip != null && db != null) {
        val checkpoint = OutfitCheckpoint.snapshot(char, equip, db)
        checkpoint.use { mall.buy(itemId, qty, maxPrice) }
    } else {
        mall.buy(itemId, qty, maxPrice)
    }
}

internal fun GameRuntimeLibrary.resolveMallBuyItemId(query: String): Int? {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return null
    gameDatabase?.item(trimmed)?.id?.let { return it }
    val name = matchingItemNames(trimmed).firstOrNull() ?: return null
    return gameDatabase?.item(name)?.id ?: ItemDatabase.getByName(name)?.id
}

internal fun GameRuntimeLibrary.runAdventureCli(
    parameters: String,
    checkOnly: Boolean,
    rt: AshRuntimeContext,
) {
    val params = parameters.trim()
    val lastName = {
        preferences?.getString("lastAdventure", "")?.takeIf { it.isNotBlank() }
            ?: preferences?.getString(Preferences.LAST_LOCATION, "").orEmpty()
    }
    val queryForLast = { name: String ->
        if (name.equals("last", ignoreCase = true)) lastName() else name
    }
    var location = resolveAdventureCliLocation(queryForLast(params))
    var adventureCount = 1
    if (location == null) {
        val tokens = params.split(Regex("\\s+"), limit = 2)
        val countToken = tokens.firstOrNull().orEmpty()
        adventureCount = if (countToken == "*") 0 else countToken.toIntOrNull() ?: 0
        if (adventureCount == 0 && countToken != "0" && countToken != "*") {
            rt.print("$params does not exist in the adventure database.")
            return
        }
        val rest = tokens.getOrNull(1)?.trim().orEmpty()
        location = resolveAdventureCliLocation(queryForLast(rest))
        if (location == null) {
            rt.print("$params does not exist in the adventure database.")
            return
        }
        val left = character?.state?.value?.adventuresLeft ?: 0
        if (adventureCount <= 0 && location.id == "355") {
            adventureCount += left / 3
        } else if (adventureCount <= 0) {
            adventureCount += left
        }
    }
    if (checkOnly) {
        rt.print(location.name)
        return
    }
    val manager = adventureManager ?: return
    kotlinx.coroutines.runBlocking {
        manager.runAdventures(location, adventureCount, this).join()
    }
}

private fun GameRuntimeLibrary.resolveAdventureCliLocation(query: String): AdventureLocation? {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return null
    resolveLocation(trimmed)?.let { return it }
    val zone = AdventureDatabase.getByName(trimmed) ?: AdventureDatabase.search(trimmed).firstOrNull()
    return zone?.let { it.toLocation() }
}

internal fun GameRuntimeLibrary.cliLocations(rt: AshRuntimeContext) {
    rt.print("Visited Locations:")
    val visited = adventureSpentTracker?.visited().orEmpty()
    for ((name, count) in visited.entries.sortedBy { it.key.lowercase() }) {
        rt.print("$name ($count)")
    }
}

private fun resolveCliLocation(query: String): net.sourceforge.kolmafia.data.AdventureZone? {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return null
    AdventureDatabase.getByName(trimmed)?.let { return it }
    return AdventureDatabase.search(trimmed).firstOrNull()
}

private fun cliUrlEncode(value: String): String = buildString {
    for (ch in value) {
        when {
            ch.isLetterOrDigit() || ch in "-_.*" -> append(ch)
            ch == ' ' -> append('+')
            else -> {
                val utf8 = ch.toString().encodeToByteArray()
                for (b in utf8) {
                    append('%')
                    append((b.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
                }
            }
        }
    }
}

internal fun GameRuntimeLibrary.runAutoAttackCli(parameters: String, rt: AshRuntimeContext) {
    val trimmed = parameters.trim()
    if (trimmed.isEmpty()) {
        rt.print(LongTailCli.formatAutoAttack(character?.state?.value?.autoAttackAction ?: 0))
        return
    }
    val skillId = LongTailCli.resolveAutoAttack(trimmed) { raw ->
        raw.toIntOrNull()
            ?: skillManager?.state?.value?.skills?.firstOrNull {
                it.name.equals(raw, ignoreCase = true) && it.type == SkillType.COMBAT
            }?.id
            ?: SkillDefinitionDatabase.getByName(raw)?.takeIf { SkillDefinitionProxy.isCombat(it.id) }?.id
    }
    if (skillId == null) {
        rt.print("No matching autoattack found!")
        return
    }
    val known = skillId <= 3 ||
        skillManager?.state?.value?.skills?.any { it.id == skillId } == true ||
        SkillDefinitionDatabase.getById(skillId) != null
    if (!known) {
        rt.print("No matching autoattack found!")
        return
    }
    val current = character?.state?.value?.autoAttackAction ?: 0
    if (skillId != current) {
        val client = httpClient
        if (client != null) {
            runBlocking {
                client.submitForm(
                    url = "$KOL_BASE_URL/account.php",
                    formParameters = Parameters.build {
                        append("action", "autoattack")
                        append("ajax", "1")
                        append("value", skillId.toString())
                    },
                )
            }
        }
        character?.setAutoAttackAction(skillId)
    }
    rt.print(LongTailCli.formatAutoAttack(skillId) { id ->
        skillManager?.state?.value?.skills?.firstOrNull { it.id == id }?.name
            ?: SkillDefinitionDatabase.getById(id)?.name
    })
}

internal fun GameRuntimeLibrary.runBountyCli(parameters: String, rt: AshRuntimeContext) {
    val cmd = parameters.trim().lowercase()
    when (cmd) {
        "", "list" -> {
            visitKolPage("bounty.php")
            rt.print(LongTailCli.formatBountyStatus(preferences))
        }
        "easy", "hard", "special" -> {
            val action = when (cmd) {
                "easy" -> "takelow"
                "hard" -> "takehigh"
                else -> "takespecial"
            }
            visitKolPage("bounty.php?action=$action")
            rt.print("Accepted $cmd bounty.")
        }
        else -> rt.print("bounty (easy|hard|special) - List or optionally accept bounties")
    }
}

internal suspend fun GameRuntimeLibrary.runSaberCli(parameters: String, rt: AshRuntimeContext) {
    val saberId = gameDatabase?.item("Fourth of May Cosplay Saber")?.id
        ?: ItemDatabase.getByName("Fourth of May Cosplay Saber")?.id
    val replicaId = gameDatabase?.item("replica Fourth of May Cosplay Saber")?.id
        ?: ItemDatabase.getByName("replica Fourth of May Cosplay Saber")?.id
    val hasSaber = (saberId != null && inventoryCount(saberId) > 0) ||
        (replicaId != null && inventoryCount(replicaId) > 0)
    if (!hasSaber) {
        rt.print("You need a Fourth of May Cosplay Saber first.")
        return
    }
    val prefs = preferences
    if (prefs != null && prefs.getInt("_saberMod", 0) != 0) {
        rt.print("You have already upgraded your saber today.")
        return
    }
    val option = LongTailCli.saberUpgradeOption(parameters)
    if (option == null) {
        rt.print("Which upgrade do you want to make?")
        return
    }
    visitKolPage("main.php?action=may4")
    choiceRequest?.choose(1386, option)?.onSuccess {
        prefs?.setInt("_saberMod", option)
        rt.print("Upgrading saber")
    } ?: rt.print("Saber upgrade unavailable")
}

internal suspend fun GameRuntimeLibrary.runSnapperCli(parameters: String, rt: AshRuntimeContext) {
    val snapperId = FamiliarDefinitionDatabase.getByName("Red-Nosed Snapper")?.id ?: 275
    if ((character?.state?.value?.familiarId ?: 0) != snapperId) {
        rt.print("You need to take your Red-Nosed Snapper with you")
        return
    }
    val phylumName = parameters.trim()
    if (phylumName.isEmpty()) {
        rt.print("Which monster phylum do you want?")
        return
    }
    val phylum = FactDatabase.MonsterPhylum.find(phylumName)
    if (phylum == FactDatabase.MonsterPhylum.NONE) {
        rt.print("What kind of random monster is a $phylumName?")
        return
    }
    val prefs = preferences
    val current = prefs?.getString("redSnapperPhylum", "") ?: ""
    if (current.equals(phylum.token, ignoreCase = true)) {
        rt.print("Your Red-Nosed Snapper is already hot on the tail of any ${phylum.token} it can see")
        return
    }
    visitKolPage("familiar.php?action=guideme")
    choiceRequest?.choose(1396, 1, mapOf("cat" to phylum.token))?.onSuccess {
        prefs?.setString("redSnapperPhylum", phylum.token)
        rt.print("Guiding your Red-Nosed Snapper toward ${phylum.token}")
    } ?: rt.print("Snapper guide unavailable")
}

private fun GameRuntimeLibrary.inventoryCount(itemId: Int): Int =
    inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

internal suspend fun GameRuntimeLibrary.runGardenCli(parameters: String, rt: AshRuntimeContext) {
    val state = character?.state?.value
    if (state == null || !CampgroundAvailability.haveCampground(state)) {
        rt.print("You can't get to your campground to visit your garden.")
        return
    }
    val crop = GardenCropAvailability.getCrop(preferences)
    val gardenType = state.gardenType.ifBlank { "unknown" }
    val command = parameters.trim().split(Regex("\\s+")).firstOrNull().orEmpty()
    if (command.equals("pick", ignoreCase = true)) {
        val client = httpClient ?: run {
            rt.print("Garden harvest unavailable")
            return
        }
        CampgroundRequest(client).harvestGarden().fold(
            onSuccess = { rt.print("Harvested your $gardenType garden.") },
            onFailure = { rt.print(it.message ?: "Harvest failed") },
        )
        return
    }
    if (crop == null || crop.count == 0) {
        rt.print("You don't have a garden.")
        return
    }
    val cropName = gameDatabase?.item(crop.itemId)?.name
        ?: ItemDatabase.getById(crop.itemId)?.name
        ?: "crop ${crop.itemId}"
    rt.print("Your $gardenType garden has ${crop.count} $cropName")
}

object LongTailCli {
    fun waitSeconds(parameters: String): Int {
        val parsed = parameters.trim().toIntOrNull() ?: 1
        return if (parsed <= 0) 1 else parsed
    }

    fun applyOlfact(
        cmd: String,
        parameters: String,
        prefs: Preferences,
        resolveItemId: (String) -> Int?,
        itemName: (Int) -> String?,
    ): String {
        val pref = if (cmd.equals("putty", ignoreCase = true)) "autoPutty" else "autoOlfact"
        var params = parameters.trim().lowercase()
        if (params == "none") {
            prefs.setString(pref, "")
        } else if (params.isNotEmpty()) {
            var isAbort = false
            if (params.endsWith(" abort")) {
                isAbort = true
                params = params.removeSuffix(" abort").trim()
            }
            val result = StringBuilder()
            when {
                params == "goals" -> result.append("goals")
                params.startsWith("monster ") -> {
                    result.append("monster ")
                    result.append(params.removePrefix("monster ").trim())
                }
                else -> {
                    val itemPart = params.removePrefix("item ").trim()
                    val id = resolveItemId(itemPart)
                    val name = id?.let { itemName(it) }
                    if (name != null) {
                        result.append("item ").append(name)
                    } else if (itemPart.isNotEmpty()) {
                        result.append("monster ").append(itemPart)
                    }
                }
            }
            if (result.isEmpty()) return "Unable to interpret your conditions!"
            if (isAbort) result.append(" abort")
            prefs.setString(pref, result.toString())
        }
        val option = prefs.getString(pref, "")
        return if (option.isEmpty()) {
            "$pref is disabled."
        } else {
            "$pref: " + option
                .replaceFirst("^goals".toRegex(), "first monster that can drop your remaining goals")
                .replaceFirst(" abort$".toRegex(), ", and then abort adventuring")
        }
    }

    fun resolveAutoAttack(raw: String, resolveSkillId: (String) -> Int?): Int? {
        val name = raw.trim().lowercase()
        if (name.isEmpty()) return null
        if (name == "none" || name.contains("disable")) return 0
        if (name == "attack" || name.startsWith("attack ")) return 1
        if (name == "steal" || name == "pickpocket" || name == "pick pocket") return 3
        return resolveSkillId(raw.trim())
    }

    fun formatAutoAttack(action: Int, skillName: (Int) -> String? = { null }): String = when (action) {
        0 -> "Autoattack is disabled."
        1 -> "Autoattack: attack with weapon."
        3 -> "Autoattack: pick pocket."
        else -> "Autoattack: ${skillName(action) ?: "skill $action"}"
    }

    fun saberUpgradeOption(parameter: String): Int? {
        val p = parameter.trim().lowercase()
        return when {
            p == "mp" -> 1
            p == "ml" -> 2
            p.isNotEmpty() && "resistance".startsWith(p) -> 3
            p.isNotEmpty() && "familiar".startsWith(p) -> 4
            else -> null
        }
    }

    fun formatBountyStatus(prefs: Preferences?): String {
        if (prefs == null) return "No bounty available"
        return listOf("Easy", "Hard", "Special").joinToString("\n") { type ->
            formatBountyLine(type, prefs)
        }
    }

    private fun formatBountyLine(type: String, prefs: Preferences): String {
        val currentKey = "current${type}BountyItem"
        val untakenKey = "_untaken${type}BountyItem"
        val current = prefs.getString(currentKey, "")
        val untaken = prefs.getString(untakenKey, "")
        val colon = current.indexOf(':')
        if (colon == -1 && untaken.isEmpty()) {
            return "$type: No bounty available"
        }
        if (colon != -1) {
            val name = current.substring(0, colon)
            val count = current.substring(colon + 1).toIntOrNull() ?: 0
            val bounty = BountyDatabase.getByName(name)
            val remaining = ((bounty?.count ?: 0) - count).coerceAtLeast(0)
            val label = bounty?.plural?.takeIf { it.isNotBlank() } ?: name
            val monster = bounty?.monster.orEmpty()
            val location = bounty?.bestLocation.orEmpty()
            return "$type: Need $remaining more $label | $monster | $location"
        }
        val bounty = BountyDatabase.getByName(untaken)
        val label = bounty?.plural?.takeIf { it.isNotBlank() } ?: untaken
        val number = bounty?.count ?: 0
        val monster = bounty?.monster.orEmpty()
        val location = bounty?.bestLocation.orEmpty()
        return "$type: Accept and get $number $label | $monster | $location"
    }

    data class Correspondent(
        val id: Int,
        val name: String,
        val aliases: List<String>,
    ) {
        companion object {
            val NONE = Correspondent(0, "None", listOf("none"))
            val ALL = listOf(
                Correspondent(1, "Pen Pal", listOf("penpal", "pen pal", "pen")),
                Correspondent(2, "GameInformPowerDailyPro Magazine", listOf("game", "gipdp")),
                Correspondent(3, "Xi Receiver Unit", listOf("xi")),
                Correspondent(4, "New-You Club", listOf("newyou", "new-you", "new you")),
                Correspondent(5, "Our Daily Candles", listOf("candle", "candles")),
                Correspondent(6, "Black & White Apron", listOf("apron")),
            )

            fun find(query: String): Correspondent? {
                val q = query.trim().lowercase().replace(Regex("[- ]"), "")
                if (q.isEmpty()) return null
                return ALL.firstOrNull { c ->
                    c.aliases.any { it.replace(Regex("[- ]"), "") == q } ||
                        c.name.lowercase().replace(Regex("[- ]"), "").contains(q) ||
                        c.name.contains(query.trim(), ignoreCase = true)
                }
            }

            fun findByName(name: String): Correspondent =
                ALL.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: NONE
        }
    }

    data class MayoChoice(val name: String, val alias: String, val option: Int, val setting: String)

    val MAYO_CHOICES = listOf(
        MayoChoice("mayodiol", "drunk", 2, "Mayodiol"),
        MayoChoice("mayoflex", "adv", 5, "Mayoflex"),
        MayoChoice("mayonex", "bmc", 1, "Mayonex"),
        MayoChoice("mayostat", "food", 3, "Mayostat"),
        MayoChoice("mayozapine", "stat", 4, "Mayozapine"),
    )

    fun findMayo(parameter: String): MayoChoice? {
        val p = parameter.trim().lowercase()
        if (p.isEmpty()) return null
        return MAYO_CHOICES.firstOrNull { it.name == p || it.alias == p }
    }

    fun mayoMinderUsage(): String = buildString {
        appendLine("Usage: mayominder mayodiol | drunk | mayoflex | adv | mayonex | bmc | mayostat | food | mayozapine | stat")
        appendLine("mayodiol or drunk: 1 full from next food converted to drunk")
        appendLine("mayoflex or adv: 1 adv from next food")
        appendLine("mayonex or bmc: adventures from next food converted to BMC")
        appendLine("mayostat or food: return some of next food")
        append("mayozapine or stat: double stat gain of next food")
    }

    data class Identifiable(val shortName: String, val itemId: Int)

    val BANG_POTIONS = listOf(
        Identifiable("bubbly", 821),
        Identifiable("cloudy", 823),
        Identifiable("dark", 826),
        Identifiable("effervescent", 824),
        Identifiable("fizzy", 825),
        Identifiable("milky", 819),
        Identifiable("murky", 827),
        Identifiable("smoky", 822),
        Identifiable("swirly", 820),
    )

    val SLIME_VIALS = listOf(
        Identifiable("red", 3885),
        Identifiable("yellow", 3886),
        Identifiable("blue", 3887),
        Identifiable("orange", 3888),
        Identifiable("green", 3889),
        Identifiable("violet", 3890),
        Identifiable("vermilion", 3891),
        Identifiable("amber", 3892),
        Identifiable("chartreuse", 3893),
        Identifiable("teal", 3894),
        Identifiable("indigo", 3895),
        Identifiable("purple", 3896),
        Identifiable("brown", 3897),
    )

    fun formatBangListing(
        vials: Boolean,
        prefs: Preferences?,
        inventoryCount: (Int) -> Int = { 0 },
    ): String {
        val table = if (vials) SLIME_VIALS else BANG_POTIONS
        val prefPrefix = if (vials) "lastSlimeVial" else "lastBangPotion"
        return table.joinToString("\n") { item ->
            val identified = prefs?.getString("$prefPrefix${item.itemId}", "").orEmpty()
            val label = identified.ifBlank { "unidentified" }
            val have = inventoryCount(item.itemId)
            buildString {
                append("${item.shortName}: $label")
                if (have > 0) append(" (have $have)")
            }
        }
    }

    fun resolveUpEffect(name: String): Pair<String?, List<String>> {
        val query = name.trim()
        if (query.isEmpty()) return null to emptyList()
        EffectDatabase.getByName(query)?.let { return it.name to emptyList() }
        val matches = EffectDatabase.all().filter { it.name.contains(query, ignoreCase = true) }
        return when {
            matches.size == 1 -> matches.first().name to emptyList()
            matches.size > 1 -> null to matches.map { it.name }
            else -> null to emptyList()
        }
    }

    fun defaultUpAction(effectName: String): Pair<String?, String?> {
        val known = MoodRemovalKnownSources.getKnownSources(effectName)
        if (known.isNotBlank()) {
            val first = known.split('|').firstOrNull { it.isNotBlank() }
            if (!first.isNullOrBlank()) return first to null
        }
        val effect = EffectDatabase.getByName(effectName) ?: return null to null
        val actions = effect.actions ?: return null to null
        if (actions.startsWith("#")) return null to actions.removePrefix("#").trim()
        val first = actions.split('|').map { it.trim() }.firstOrNull { it.isNotEmpty() }
        return first to null
    }

    fun formatCcsStatus(ccs: String, battleAction: String): String = buildString {
        append("CCS is ${ccs.ifBlank { "default" }}")
        if (battleAction.isNotEmpty() && !battleAction.startsWith("custom", ignoreCase = true)) {
            append("\n(but battle action is currently set to $battleAction)")
        }
    }

    val DUSTY_BOTTLE_IDS = 2271..2276

    fun dustyBottleType(itemId: Int): String = when (itemId) {
        2271 -> "average"
        2272 -> "vinegar"
        2273 -> "spooky"
        2274 -> "great"
        2275 -> "glassy"
        2276 -> "bad"
        else -> "dusty"
    }

    fun formatDustyListing(itemName: (Int) -> String?): String =
        DUSTY_BOTTLE_IDS.joinToString("\n") { id ->
            val name = itemName(id)?.takeIf { it.isNotBlank() } ?: "#$id"
            "$name: ${dustyBottleType(id)}"
        }

    fun formatHermitCloverCount(count: Int): String {
        val noun = if (count == 1) "clover" else "clovers"
        return "The Hermit has $count $noun available today."
    }

    fun formatCrimboTreeDays(days: Int): String = "Check back in $days days."

    fun formatCrimboTreeEmpty(days: Int): String =
        "There's nothing under the Crimbo Tree with your name on it right now. " +
            formatCrimboTreeDays(days)

    enum class NamedCafe { KITCHEN, RESTAURANT, BREWERY }

    data class MallShopPut(
        val itemName: String,
        val price: Int,
        val limit: Int,
    )

    data class MallShopReprice(
        val itemName: String,
        val price: Int,
        val limit: Int,
    )

    fun parseCountAndName(parameters: String): Pair<Int, String> {
        val trimmed = parameters.trim()
        if (trimmed.isEmpty()) return 1 to ""
        val space = trimmed.indexOf(' ')
        if (space == -1) return 1 to trimmed
        val first = trimmed.substring(0, space)
        val rest = trimmed.substring(space + 1).trim()
        val qty = first.toIntOrNull()
        return if (qty != null) qty to rest else 1 to trimmed
    }

    fun parseMallShopPuts(parameters: String): List<MallShopPut>? {
        val specs = mutableListOf<MallShopPut>()
        for (raw in parameters.split(',').map { it.trim() }.filter { it.isNotEmpty() }) {
            val at = raw.indexOf('@')
            val itemName: String
            var price = 0
            var limit = 0
            if (at == -1) {
                itemName = raw.trim()
            } else {
                itemName = raw.substring(0, at).trim()
                var description = raw.substring(at + 1).trim()
                val limitIdx = description.indexOf("limit", ignoreCase = true)
                if (limitIdx != -1) {
                    limit = description.substring(limitIdx + 5).trim().toIntOrNull() ?: 0
                    description = description.substring(0, limitIdx).trim()
                }
                price = description.replace(",", "").toIntOrNull() ?: 0
            }
            if (itemName.isNotEmpty() && itemName.all { it.isDigit() }) return null
            specs += MallShopPut(itemName, price, limit)
        }
        return specs
    }

    fun parseMallShopReprices(parameters: String): List<MallShopReprice>? {
        val parts = parameters.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.any { !it.contains('@') }) return null
        return parts.map { raw ->
            val at = raw.indexOf('@')
            val itemName = raw.substring(0, at).trim()
            var description = raw.substring(at + 1).trim()
            var limit = 0
            val limitIdx = description.indexOf("limit", ignoreCase = true)
            if (limitIdx != -1) {
                limit = description.substring(limitIdx + 5).trim().toIntOrNull() ?: 0
                description = description.substring(0, limitIdx).trim()
            }
            val price = description.replace(",", "").toIntOrNull() ?: 0
            MallShopReprice(itemName, price, limit)
        }
    }

    fun parseShopTake(parameters: String): Pair<Int, List<String>> {
        val trimmed = parameters.trim()
        val space = trimmed.indexOf(' ')
        if (space == -1) return 1 to listOf(trimmed)
        val first = trimmed.substring(0, space)
        val rest = trimmed.substring(space + 1).trim()
        val qty = first.toIntOrNull()
        val names = (if (qty != null) rest else trimmed)
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        return (qty ?: 1) to names
    }

    fun stickerEquipName(token: String): String {
        val item = token.trim()
        return if (item.contains("stick", ignoreCase = true)) item else "$item sticker"
    }
}

internal fun GameRuntimeLibrary.runEudoraCli(parameters: String, rt: AshRuntimeContext) {
    val arg = parameters.trim()
    if (arg.isEmpty()) {
        val current = preferences?.getString("eudora", "").orEmpty()
        val name = LongTailCli.Correspondent.findByName(current).name
        rt.print("Current correspondent is $name")
        return
    }
    val correspondent = LongTailCli.Correspondent.find(arg)
    if (correspondent == null) {
        rt.print("That is not a valid correspondent")
        return
    }
    val client = httpClient
    if (client == null) {
        rt.print("Cannot switch to ${correspondent.name}")
        return
    }
    kotlinx.coroutines.runBlocking {
        try {
            val response = client.get(
                "$KOL_BASE_URL/account.php?am=1&action=whichpenpal&ajax=1&value=${correspondent.id}",
            )
            val html = response.bodyAsText()
            val failed = html.contains("cannot", ignoreCase = true)
            if (failed) {
                rt.print("Cannot switch to ${correspondent.name}")
            } else {
                preferences?.setString("eudora", correspondent.name)
                rt.print("Switched to ${correspondent.name}")
            }
        } catch (_: Exception) {
            rt.print("Cannot switch to ${correspondent.name}")
        }
    }
}

internal suspend fun GameRuntimeLibrary.runMayoMinderCli(parameters: String, rt: AshRuntimeContext) {
    val arg = parameters.trim()
    if (arg.isEmpty()) {
        rt.print(LongTailCli.mayoMinderUsage())
        return
    }
    val mayo = LongTailCli.findMayo(arg)
    if (mayo == null) {
        rt.print("I don't understand what '$arg' mayo is.")
        return
    }
    if (!CampgroundItemSync.hasWorkshedItem(preferences, ConcoctionMayoQueue.MAYO_CLINIC)) {
        rt.print("Mayo clinic not installed")
        return
    }
    val minderId = 8285
    if (inventoryCount(minderId) <= 0) {
        val got = retrieveItemService?.retrieve(minderId, 1) ?: 0
        if (got < 1 && inventoryCount(minderId) <= 0) {
            rt.print("You cannot obtain a Mayo Minder")
            return
        }
    }
    useItemRequest?.use(minderId, 1)
    val chosen = choiceRequest?.choose(1076, mayo.option)
    if (chosen == null || chosen.isFailure) {
        rt.print("Mayo Minder unavailable")
        return
    }
    preferences?.setString("mayoMinderSetting", mayo.setting)
    rt.print("Mayo Minder™ now set to ${preferences?.getString("mayoMinderSetting", mayo.setting) ?: mayo.setting}")
}

internal fun GameRuntimeLibrary.runBangPotionsCli(vials: Boolean, rt: AshRuntimeContext) {
    rt.print(LongTailCli.formatBangListing(vials, preferences) { inventoryCount(it) })
}

internal fun GameRuntimeLibrary.runUpCli(parameters: String, rt: AshRuntimeContext) {
    val raw = parameters.trim()
    if (raw.contains(',')) {
        raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.forEach { runUpCli(it, rt) }
        return
    }
    val (resolved, matches) = LongTailCli.resolveUpEffect(raw)
    if (resolved == null) {
        if (matches.isNotEmpty()) {
            rt.print("Ambiguous effect name: $raw")
            matches.forEach { rt.print(it) }
        } else {
            rt.print("Unknown effect: $raw")
        }
        return
    }
    val (action, note) = LongTailCli.defaultUpAction(resolved)
    if (action.isNullOrBlank()) {
        if (!note.isNullOrBlank()) {
            rt.print("No direct source for: $resolved")
            rt.print("It may be obtainable via $note.")
        } else {
            rt.print("No booster known: $resolved")
        }
        return
    }
    dispatchCli(action, rt)
}

internal suspend fun GameRuntimeLibrary.runSpoonCli(parameters: String, rt: AshRuntimeContext) {
    val spoonId = 10254
    val replicaId = 11242
    val inLoL = character?.state?.value?.inLegacyOfLoathing == true
    val hasSpoon = hasAccessibleItem(spoonId) || (inLoL && hasAccessibleItem(replicaId))
    if (!hasSpoon) {
        rt.print("You need a hewn moon-rune spoon first.")
        return
    }
    if (preferences?.getBoolean("moonTuned", false) == true) {
        rt.print("You have already tuned the moon this ascension.")
        return
    }
    val signName = parameters.trim()
    if (signName.isEmpty()) {
        rt.print("Which sign do you want to change to?")
        return
    }
    val current = ZodiacSign.find(character?.state?.value?.zodiacSign.orEmpty())
    if (current?.isBadMoon == true) {
        rt.print("You can't escape the Bad Moon this way.")
        return
    }
    val sign = ZodiacSign.find(signName)
    if (sign == null) {
        rt.print("I don't understand what sign $signName is.")
        return
    }
    if (sign.isBadMoon) {
        rt.print("You can't choose to be born under a Bad Moon.")
        return
    }
    if (current != null && sign == current) {
        rt.print("No need to change, you're already a ${current.signName}.")
        return
    }
    val useId = if (inLoL && hasAccessibleItem(replicaId)) replicaId else spoonId
    val wornSlot = equipmentSlotOf(useId)
    if (wornSlot != null) {
        equipmentRequest?.unequipSlot(wornSlot)
    }
    val client = httpClient ?: run {
        rt.print("Spoon tune unavailable")
        return
    }
    try {
        client.get("$KOL_BASE_URL/inv_use.php?whichitem=$useId&doit=96&whichsign=${sign.id}")
        preferences?.setBoolean("moonTuned", true)
        character?.setZodiacSign(sign.signName)
        rt.print("Tuning moon to ${sign.signName}")
        if (wornSlot != null) {
            equipmentRequest?.equipItem(useId, wornSlot)
        }
    } catch (_: Exception) {
        rt.print("Spoon tune unavailable")
    }
}

internal fun GameRuntimeLibrary.runCcsStatusCli(rt: AshRuntimeContext) {
    val ccs = preferences?.getString("combatMacro", "").orEmpty()
        .ifBlank { preferences?.getString(Preferences.COMBAT_SCRIPT, "").orEmpty() }
    val battle = preferences?.getString("battleAction", "").orEmpty()
    rt.print(LongTailCli.formatCcsStatus(ccs, battle))
}

internal fun GameRuntimeLibrary.runDustyCli(rt: AshRuntimeContext) {
    rt.print(
        LongTailCli.formatDustyListing { id ->
            gameDatabase?.item(id)?.name ?: ItemDatabase.getById(id)?.name
        },
    )
}

internal fun GameRuntimeLibrary.runHermitStatusCli(rt: AshRuntimeContext) {
    val path = character?.state?.value?.ascensionPath ?: AscensionPath.NONE
    val count = kotlinx.coroutines.runBlocking {
        hermitRequest?.fetchCloverCount(path, preferences) ?: 0
    }
    rt.print(LongTailCli.formatHermitCloverCount(count))
}

internal fun GameRuntimeLibrary.runHermitTradeCli(itemName: String, quantity: Int, rt: AshRuntimeContext) {
    val trimmed = itemName.trim()
    val itemId = gameDatabase?.item(trimmed)?.id
        ?: ItemDatabase.getByName(trimmed)?.id
        ?: ItemDatabase.getByPluralOrName(trimmed)?.id
    if (itemId == null) {
        rt.print("You can't get a $trimmed from the hermit today.")
        return
    }
    kotlinx.coroutines.runBlocking { hermitRequest?.trade(itemId, quantity) }
}

internal fun GameRuntimeLibrary.runChipsCli(parameters: String, rt: AshRuntimeContext) {
    val raw = parameters.trim()
    if (raw.isEmpty()) {
        rt.print("What kind of chips do you want?")
        return
    }
    val flavors = raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    if (flavors.isEmpty() || flavors.size > 3) {
        rt.print("Specify from 1 to 3 kinds of chip")
        return
    }
    val bags = flavors.map { flavor ->
        val bag = ClanRumpusRequest.findChips(flavor)
        if (bag == 0) {
            rt.print("You can't buy '$flavor' chips")
            return
        }
        flavor to bag
    }
    val client = httpClient ?: run {
        rt.print("Chips unavailable")
        return
    }
    kotlinx.coroutines.runBlocking {
        for ((flavor, bag) in bags) {
            ClanRumpusRequest(client).buyChips(bag)
                .onSuccess { rt.print("Bought $flavor chips.") }
                .onFailure { rt.print(it.message ?: "Chips unavailable") }
        }
    }
}

internal fun GameRuntimeLibrary.runSofaCli(parameters: String, rt: AshRuntimeContext) {
    val raw = parameters.trim()
    val turns = if (raw.isEmpty()) 1 else raw.toIntOrNull()
    if (turns == null || turns <= 0) {
        rt.print("Specify a positive number of turns.")
        return
    }
    val adventures = character?.state?.value?.adventuresLeft ?: 0
    if (adventures < turns) {
        rt.print("Insufficient adventures.")
        return
    }
    val client = httpClient ?: run {
        rt.print("Sofa unavailable")
        return
    }
    kotlinx.coroutines.runBlocking {
        ClanRumpusRequest(client).nap(turns)
            .onSuccess {
                val noun = if (turns == 1) "turn" else "turns"
                rt.print("Rested for $turns $noun.")
            }
            .onFailure { rt.print(it.message ?: "Sofa unavailable") }
    }
}

internal fun GameRuntimeLibrary.runCrimboTreeCli(parameters: String, rt: AshRuntimeContext) {
    val days = preferences?.getInt("crimboTreeDays", 0) ?: 0
    if (!parameters.trim().equals("get", ignoreCase = true)) {
        rt.print(LongTailCli.formatCrimboTreeDays(days))
        return
    }
    val lounge = clanLoungeRequest ?: run {
        rt.print("Crimbo tree unavailable")
        return
    }
    kotlinx.coroutines.runBlocking {
        lounge.visitCrimboTree()
            .onFailure {
                rt.print(it.message ?: "Crimbo tree unavailable")
                return@runBlocking
            }
        if (days > 0) {
            rt.print(LongTailCli.formatCrimboTreeEmpty(days))
        } else {
            preferences?.setInt("crimboTreeDays", 0)
            preferences?.setBoolean("_crimboTree", true)
        }
    }
}

internal fun GameRuntimeLibrary.runVersionCli(rt: AshRuntimeContext) {
    rt.print("KoLmafia Mobile ${GameRuntimeLibrary.REVISION}")
}

internal fun GameRuntimeLibrary.runGreyYouCli(parameters: String, rt: AshRuntimeContext) {
    val tokens = parameters.trim().lowercase().split(Regex("\\s+")).filter { it.isNotBlank() }
    var showAll = true
    var filterType: GreyYouManager.AbsorptionType? = null
    for (token in tokens) {
        when (token) {
            "all" -> showAll = true
            "needed" -> showAll = false
            "skill", "skills" -> filterType = GreyYouManager.AbsorptionType.SKILL
            "adventures", "advs" -> filterType = GreyYouManager.AbsorptionType.ADVENTURES
            "muscle", "mus" -> filterType = GreyYouManager.AbsorptionType.MUSCLE
            "mysticality", "myst", "mys" -> filterType = GreyYouManager.AbsorptionType.MYSTICALITY
            "moxie", "mox" -> filterType = GreyYouManager.AbsorptionType.MOXIE
            "maxhp" -> filterType = GreyYouManager.AbsorptionType.MAX_HP
            "maxmp" -> filterType = GreyYouManager.AbsorptionType.MAX_MP
        }
    }
    GreyYouManager.loadRegistry()
    rt.print("Zone | Have | Monster | Reward")
    GreyYouManager.zoneAbsorptions.forEach { (zone, absorptions) ->
        val rows = absorptions.filter { absorption ->
            val have = absorption.haveAbsorbed()
            (showAll || !have) && (filterType == null || absorption.type == filterType)
        }
        if (rows.isEmpty()) return@forEach
        rows.forEachIndexed { index, absorption ->
            val prefix = if (index == 0) "$zone | " else " | "
            val have = if (absorption.haveAbsorbed()) "yes" else "no"
            rt.print("$prefix$have | ${absorption.monsterName} | ${absorption.rewardLabel()}")
        }
    }
}

internal fun GameRuntimeLibrary.runBurnCli(parameters: String, rt: AshRuntimeContext) {
    val charState = character?.state?.value
    if (charState?.inZombiecore == true) return
    val token = parameters.trim().substringBefore(' ').trim()
    val extra = token.equals("extra", ignoreCase = true)
    val all = token == "*"
    val amount = token.toIntOrNull()
    if (!extra && !all && amount == null) {
        rt.print("Specify how much mana you want to burn")
        return
    }
    val burner = manaBurnManager ?: return
    val mood = moodManager?.activeMood
    val moodLibrary = moodManager?.moodLibrary ?: emptyMap()
    val effectState = effectManager?.state?.value ?: EffectState()
    val skillState = skillManager?.state?.value ?: SkillState()
    kotlinx.coroutines.runBlocking {
        recoverHpBeforeBurn()
        val live = { character?.state?.value ?: CharacterState() }
        val snapshot = live()
        if (extra) {
            burner.burnExtraMana(
                mood, effectState, skillState, snapshot, moodLibrary, live,
            )
        } else {
            var burnAmount = if (all) 0 else amount ?: 0
            if (burnAmount > 0) burnAmount -= snapshot.currentMp
            burner.burnMana(
                (-burnAmount).toLong(),
                mood, effectState, skillState, snapshot, moodLibrary, live,
            )
        }
    }
}

private suspend fun GameRuntimeLibrary.recoverHpBeforeBurn() {
    val rm = recoveryManager ?: return
    val char = character ?: return
    rm.recoverIfNeeded(
        charState = char.state.value,
        invState = inventoryManager?.state?.value ?: InventoryState(),
        skillState = skillManager?.state?.value ?: SkillState(),
    )
}

internal fun GameRuntimeLibrary.runNamedCafeCli(
    kind: LongTailCli.NamedCafe,
    parameters: String,
    rt: AshRuntimeContext,
) {
    val state = character?.state?.value
    val available = when (kind) {
        LongTailCli.NamedCafe.KITCHEN -> CafeAccessibility.isHellKitchenAvailable(state)
        LongTailCli.NamedCafe.RESTAURANT -> CafeAccessibility.isChezSnooteeAvailable(state)
        LongTailCli.NamedCafe.BREWERY -> CafeAccessibility.isMicroBreweryAvailable(state, preferences)
    }
    if (!available) {
        rt.print(
            when (kind) {
                LongTailCli.NamedCafe.KITCHEN -> "Hell's Kitchen not available."
                LongTailCli.NamedCafe.RESTAURANT -> "Chez Snootée not available."
                LongTailCli.NamedCafe.BREWERY -> "Microbrewery not available."
            },
        )
        return
    }
    val raw = parameters.trim()
    if (raw.isEmpty()) {
        rt.print(
            when (kind) {
                LongTailCli.NamedCafe.KITCHEN -> "Hell's Kitchen not available."
                LongTailCli.NamedCafe.RESTAURANT,
                LongTailCli.NamedCafe.BREWERY,
                -> "Today's Special unavailable."
            },
        )
        return
    }
    val (qty, nameQuery) = LongTailCli.parseCountAndName(raw)
    if (nameQuery.isEmpty()) {
        rt.print(
            when (kind) {
                LongTailCli.NamedCafe.KITCHEN -> "Hell's Kitchen not available."
                else -> "Today's Special unavailable."
            },
        )
        return
    }
    val entry = when (kind) {
        LongTailCli.NamedCafe.KITCHEN -> HellKitchenDatabase.find(nameQuery)
        LongTailCli.NamedCafe.RESTAURANT -> ChezSnooteeDatabase.find(nameQuery)
        LongTailCli.NamedCafe.BREWERY -> MicroBreweryDatabase.find(nameQuery)
    }
    if (entry == null) {
        rt.print("Unknown cafe item.")
        return
    }
    val consumeType = when (entry.type) {
        ConsumableType.DRINK -> ConcoctionConsumptionType.DRINK
        else -> ConcoctionConsumptionType.EAT
    }
    val count = if (qty <= 0) 1 else qty
    cliCafePurchase(entry.name, count, consumeType, rt::print)
}

internal fun GameRuntimeLibrary.runMallSellCli(parameters: String, rt: AshRuntimeContext) {
    val specs = LongTailCli.parseMallShopPuts(parameters.trim())
    if (specs == null) {
        rt.print("That is not an item. Did you use a comma in the middle of a number?")
        return
    }
    if (specs.isEmpty()) return
    putMallShopItems(specs, rt)
}

internal fun GameRuntimeLibrary.runShopCli(parameters: String, rt: AshRuntimeContext) {
    val raw = parameters.trim()
    val space = raw.indexOf(' ')
    val verb = if (space == -1) raw else raw.substring(0, space)
    val rest = if (space == -1) "" else raw.substring(space + 1).trim()
    when {
        verb.equals("put", ignoreCase = true) -> runMallSellCli(rest, rt)
        verb.equals("take", ignoreCase = true) -> runShopTakeCli(rest, rt)
        verb.equals("reprice", ignoreCase = true) -> runShopRepriceCli(rest, rt)
        else -> rt.print("Invalid shop command.")
    }
}

private fun GameRuntimeLibrary.putMallShopItems(
    specs: List<LongTailCli.MallShopPut>,
    rt: AshRuntimeContext,
) {
    val store = manageStoreRequest ?: run {
        rt.print("Mall store unavailable")
        return
    }
    val resolved = specs.mapNotNull { spec ->
        val itemId = resolveCliItemId(spec.itemName)
        if (itemId == null) {
            rt.print("Unknown item: ${spec.itemName}")
            null
        } else {
            Triple(itemId, spec.price, spec.limit)
        }
    }
    if (resolved.isEmpty()) return
    kotlinx.coroutines.runBlocking {
        for ((itemId, price, limit) in resolved) {
            store.addItem(itemId, price, limit, quantity = 1)
        }
    }
}

private fun GameRuntimeLibrary.runShopTakeCli(parameters: String, rt: AshRuntimeContext) {
    val (qty, names) = LongTailCli.parseShopTake(parameters)
    if (names.isEmpty() || names.all { it.isEmpty() }) return
    val store = manageStoreRequest ?: run {
        rt.print("Mall store unavailable")
        return
    }
    val resolved = names.mapNotNull { name ->
        val itemId = resolveCliItemId(name)
        if (itemId == null) {
            rt.print("Unknown item: $name")
            null
        } else {
            itemId
        }
    }
    if (resolved.isEmpty()) return
    kotlinx.coroutines.runBlocking {
        for (itemId in resolved) {
            store.removeItem(itemId, qty.coerceAtLeast(1))
        }
    }
}

private fun GameRuntimeLibrary.runShopRepriceCli(parameters: String, rt: AshRuntimeContext) {
    val specs = LongTailCli.parseMallShopReprices(parameters.trim())
    if (specs == null) {
        rt.print("Specify a price with @ for reprice.")
        return
    }
    if (specs.isEmpty()) return
    val store = manageStoreRequest ?: run {
        rt.print("Mall store unavailable")
        return
    }
    val resolved = specs.mapNotNull { spec ->
        val itemId = resolveCliItemId(spec.itemName)
        if (itemId == null) {
            rt.print("Unknown item: ${spec.itemName}")
            null
        } else {
            Triple(itemId, spec.price, spec.limit)
        }
    }
    if (resolved.isEmpty()) return
    kotlinx.coroutines.runBlocking {
        for ((itemId, price, limit) in resolved) {
            store.repriceItem(itemId, price, limit)
        }
    }
}

private fun GameRuntimeLibrary.resolveCliItemId(name: String): Int? {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return null
    return gameDatabase?.item(trimmed)?.id
        ?: ItemDatabase.getByName(trimmed)?.id
        ?: ItemDatabase.getByPluralOrName(trimmed)?.id
}

internal fun GameRuntimeLibrary.runStickersCli(parameters: String, rt: AshRuntimeContext) {
    val equipment = character?.state?.value?.equipment ?: emptyMap()
    val tokens = parameters.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    if (tokens.isEmpty()) {
        for ((index, slot) in EquipmentSlot.STICKER_SLOTS.withIndex()) {
            val name = equipment[slot].orEmpty()
            rt.print("Sticker ${index + 1}: ${name.ifBlank { "(empty)" }}")
        }
        return
    }
    var index = 0
    for (slot in EquipmentSlot.STICKER_SLOTS) {
        val occupied = equipment[slot]?.isNotBlank() == true
        if (occupied) continue
        if (index >= tokens.size) break
        val name = LongTailCli.stickerEquipName(tokens[index++])
        cliEquip("${slot.apiKey} $name")
    }
}

/**
 * Card sleeve list/equip CLI — mirrors stickers/folders sub-slot helpers.
 */
internal fun GameRuntimeLibrary.runCardsleeveCli(parameters: String, rt: AshRuntimeContext) {
    val equipment = character?.state?.value?.equipment ?: emptyMap()
    val current = equipment[EquipmentSlot.CARDSLEEVE].orEmpty()
    val arg = parameters.trim()
    if (arg.isEmpty()) {
        rt.print("Card Sleeve: ${current.ifBlank { "(empty)" }}")
        return
    }
    cliEquip("${EquipmentSlot.CARDSLEEVE.apiKey} $arg", rt)
}

/**
 * Cowboy-boot sub-slot CLI — bare status or equip into bootskin/bootspur.
 */
internal fun GameRuntimeLibrary.runBootSubSlotCli(
    slot: EquipmentSlot,
    parameters: String,
    rt: AshRuntimeContext,
) {
    val equipment = character?.state?.value?.equipment ?: emptyMap()
    val current = equipment[slot].orEmpty()
    val arg = parameters.trim()
    if (arg.isEmpty()) {
        rt.print("${slot.displayName}: ${current.ifBlank { "(empty)" }}")
        return
    }
    cliEquip("${slot.apiKey} $arg", rt)
}

/**
 * Mobile ocean destination/action prefs CLI (desktop ChoiceOptionsPanel Pirate ocean selects).
 * Prefs drive [OceanManager] automation on Poop Deck redirects.
 */
internal fun GameRuntimeLibrary.runOceanCli(parameters: String, rt: AshRuntimeContext) {
    val prefs = preferences ?: return
    val raw = parameters.trim()
    val lower = raw.lowercase()
    when {
        raw.isEmpty() || lower == "status" -> {
            val dest = prefs.getString("oceanDestination", "manual")
            val action = prefs.getString("oceanAction", "savecontinue")
            rt.print("Ocean destination: $dest")
            rt.print("Ocean action: $action")
        }
        lower == "list" -> {
            rt.print("Destinations: muscle, mysticality, moxie, sand, altar, sphere, plinth, random, manual, ignore, lon,lat")
            rt.print("Actions: continue, show, stop, savecontinue, saveshow, savestop")
        }
        lower.startsWith("dest ") || lower.startsWith("destination ") -> {
            val value = raw.substringAfter(' ').trim()
            val normalized = normalizeOceanDestination(value)
            if (normalized == null) {
                rt.print("($value) are not valid ocean coordinates")
                return
            }
            prefs.setString("oceanDestination", normalized)
            rt.print("oceanDestination = $normalized")
        }
        lower.startsWith("action ") -> {
            val value = raw.substringAfter(' ').trim()
            val normalized = normalizeOceanAction(value)
            if (normalized == null) {
                rt.print("Unknown ocean action: $value")
                return
            }
            prefs.setString("oceanAction", normalized)
            rt.print("oceanAction = $normalized")
        }
        else -> {
            rt.print("Usage: ocean [status|list|dest <value>|action <value>]")
        }
    }
}

internal fun normalizeOceanDestination(raw: String): String? {
    val trimmed = raw.trim()
    if (trimmed.isEmpty()) return null
    val lower = trimmed.lowercase()
    when (lower) {
        "muscle", "mysticality", "moxie", "sand", "altar", "sphere", "plinth",
        "random", "manual", "ignore",
        -> return lower
    }
    val point = OceanDatabase.OceanPoint.parse(trimmed) ?: return null
    return point.toString()
}

internal fun normalizeOceanAction(raw: String): String? {
    val compact = raw.trim().lowercase().replace(Regex("\\s+"), "")
    return when (compact) {
        "continue" -> "continue"
        "show" -> "show"
        "stop" -> "stop"
        "savecontinue", "saveandcontinue" -> "savecontinue"
        "saveshow", "saveandshow" -> "saveshow"
        "savestop", "saveandstop" -> "savestop"
        else -> null
    }
}

internal fun GameRuntimeLibrary.runFoldersCli(parameters: String, rt: AshRuntimeContext) {
    val hasHolder = FolderHolderAccessibility.hasFolderHolder { id ->
        inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
    }
    if (!hasHolder) {
        rt.print("You need a folder holder.")
        return
    }
    val slots = EquipmentSlot.folderSlotsFor(character?.state?.value?.inKoLHS == true)
    val equipment = character?.state?.value?.equipment ?: emptyMap()
    val tokens = parameters.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    if (tokens.isEmpty()) {
        for (slot in slots) {
            val name = equipment[slot].orEmpty()
            rt.print("${slot.displayName}: $name")
        }
        return
    }
    var index = 0
    for (slot in slots) {
        val occupied = equipment[slot]?.isNotBlank() == true
        if (occupied) continue
        if (index >= tokens.size) break
        val name = tokens[index++]
        cliEquip("${slot.apiKey} $name")
    }
}

internal fun GameRuntimeLibrary.runConditionCli(parameters: String, rt: AshRuntimeContext) {
    val raw = parameters.trim()
    if (raw.isEmpty()) {
        goalManager?.allGoalsAsStrings()?.forEach { rt.print(it) }
        return
    }
    val lower = raw.lowercase()
    when {
        lower == "clear" -> {
            goalManager?.clearGoals()
            rt.print("Conditions list cleared.")
        }
        lower == "list" -> goalManager?.allGoalsAsStrings()?.forEach { rt.print(it) }
        lower.startsWith("add ") -> applyConditions(raw.substring(4).trim(), GoalManager.ConditionMode.ADD, rt)
        lower.startsWith("remove ") -> applyConditions(raw.substring(7).trim(), GoalManager.ConditionMode.REMOVE, rt)
        lower.startsWith("set ") -> applyConditions(raw.substring(4).trim(), GoalManager.ConditionMode.SET, rt)
        else -> applyConditions(raw, GoalManager.ConditionMode.ADD, rt)
    }
}

private fun GameRuntimeLibrary.applyConditions(
    conditionList: String,
    mode: GoalManager.ConditionMode,
    rt: AshRuntimeContext,
) {
    val manager = goalManager ?: return
    val state = character?.state?.value
    val context = GoalManager.ConditionContext(
        characterState = state,
        preferences = preferences,
        lastAdventure = preferences?.getString("lastAdventure", "") ?: "",
        isEquipped = { piece ->
            state?.equipment?.values?.any { it.equals(piece, ignoreCase = true) } == true
        },
    )
    for (part in GoalConditionParser.splitConditions(conditionList)) {
        val parsed = GoalConditionParser.parse(part) ?: continue
        manager.applyCondition(parsed, mode, context)
        rt.print("Condition ${mode.name.lowercase()}: $part")
    }
}

internal fun GameRuntimeLibrary.runRefreshCli(target: String, rt: AshRuntimeContext) {
    val trimmed = target.trim()
    val key = trimmed.lowercase()
    if (key.startsWith("camp")) {
        visitKolPage("campground.php")
        return
    }
    kotlinx.coroutines.runBlocking {
        when {
            key.isEmpty() || key == "all" -> refreshSessionState()
            key == "status" || key == "effects" -> {
                characterRequest?.fetchCharacterState()?.onSuccess { resp ->
                    character?.updateFromApiResponse(resp)
                }
                effectManager?.fetchEffects()
            }
            key == "gear" || key.startsWith("equip") || key == "outfit" || key.startsWith("stick") -> {
                equipmentRequest?.syncCharacterEquipment()
            }
            key.startsWith("inv") -> {
                inventoryManager?.fetchInventory()
                inventoryManager?.syncCharacterEquipment()
            }
            key == "storage" -> {
                val req = storageRequest ?: return@runBlocking
                val prefs = preferences ?: return@runBlocking
                CollectionCacheSync.refreshStorage(req, character?.state?.value, prefs)
            }
            key == "stash" -> {
                val req = clanStashRequest ?: return@runBlocking
                val prefs = preferences ?: return@runBlocking
                CollectionCacheSync.refreshStash(req, prefs)
            }
            key == "closet" -> {
                val req = closetRequest ?: return@runBlocking
                val prefs = preferences ?: return@runBlocking
                CollectionCacheSync.refreshCloset(req, prefs)
            }
            key.startsWith("familiar") || key == "terrarium" -> familiarManager?.fetchFamiliars()
            key == "quests" -> questLogRequest?.syncAll()
            key == "shop" -> manageStoreRequest?.refreshPrices()
            key == "concoctions" -> ConcoctionDatabase.refreshConcoctionsNowFromLastContext()
            else -> rt.print("$trimmed cannot be refreshed.")
        }
    }
}

private suspend fun GameRuntimeLibrary.refreshSessionState() {
    characterRequest?.fetchCharacterState()?.onSuccess { resp ->
        character?.updateFromApiResponse(resp)
    }
    inventoryManager?.fetchInventory()
    skillManager?.fetchSkills()
    effectManager?.fetchEffects()
    familiarManager?.fetchFamiliars()
    questLogRequest?.syncAll()
    checkDynamicModifiers()
}

internal fun GameRuntimeLibrary.runMpItemsCli(rt: AshRuntimeContext) {
    val setting = preferences?.getString("mpAutoRecoveryItems", "").orEmpty().lowercase()
    var count = 0
    if (setting.isNotEmpty()) {
        for (restore in RestoreDatabase.mpRestores()) {
            if (restore.type != RestoreType.ITEM) continue
            if (setting.indexOf(restore.name.lowercase()) < 0) continue
            val itemId = gameDatabase?.item(restore.name)?.id
                ?: ItemDatabase.getByName(restore.name)?.id
                ?: continue
            count += inventoryCount(itemId)
        }
    }
    rt.print("$count mana restores remaining.")
}

internal fun GameRuntimeLibrary.runEchoCli(text: String, rt: AshRuntimeContext) {
    if (text.equals("timestamp", ignoreCase = true)) {
        rt.print(KolGameHolidayCalendar.getCalendarDayAsString())
    } else {
        rt.print(text)
    }
}

internal fun GameRuntimeLibrary.sanitizeLogEcho(text: String): String {
    var parameters = text.replace("\n", "").replace("\r", "")
    if (parameters.equals("timestamp", ignoreCase = true)) {
        parameters = KolGameHolidayCalendar.getCalendarDayAsString()
    }
    return parameters.replace("<", "&lt;")
}

internal fun GameRuntimeLibrary.runLogEchoCli(text: String) {
    sessionLogger?.appendRawLine(" > ${sanitizeLogEcho(text)}")
}

internal fun GameRuntimeLibrary.runFullEchoCli(text: String, rt: AshRuntimeContext) {
    val sanitized = sanitizeLogEcho(text)
    rt.print(sanitized)
    sessionLogger?.appendRawLine(" > $sanitized")
}

internal fun GameRuntimeLibrary.runPullAllCli(rt: AshRuntimeContext) {
    val state = character?.state?.value
    if (state?.isHardcore == true) {
        rt.print("You cannot empty storage when you are in Hardcore.")
        return
    }
    if (!net.sourceforge.kolmafia.request.StoragePullRules.canInteract(state)) {
        rt.print("You cannot pull everything while your pulls are limited.")
        return
    }
    runBlocking { refreshStorageCacheAfter(storageRequest?.emptyStorage()) }
}

internal fun GameRuntimeLibrary.runPullOutfitCli(outfitName: String, rt: AshRuntimeContext) {
    val state = character?.state?.value
    if (state?.isHardcore == true) {
        rt.print("You cannot pull things from storage when you are in Hardcore.")
        return
    }
    val outfit = outfitManager?.getMatchingOutfit(outfitName.trim())
    if (outfit == null) {
        rt.print("No such outfit.")
        return
    }
    runBlocking {
        for (pieceName in outfit.pieces) {
            val itemId = gameDatabase?.item(pieceName)?.id ?: continue
            val classified = storageRequest?.fetchClassifiedContents(state, preferences)
            val storageCount = (classified?.storage?.get(itemId) ?: 0) +
                (classified?.freepulls?.get(itemId) ?: 0)
            var available = physicalAccessibleCount(itemId, pieceName)
            if (net.sourceforge.kolmafia.request.StorageRequest.canUseStorage(state)) {
                available -= storageCount
            }
            if (available > 0) {
                rt.print("$pieceName is available without pulling.")
                continue
            }
            if (storageCount <= 0) {
                rt.print("$pieceName is not in storage.")
                continue
            }
            refreshStorageCacheAfter(storageRequest?.withdraw(itemId, minOf(1, storageCount)))
        }
    }
}

/** Desktop BudgetCommand — show/set pulls budgeted for automatic use. */
internal fun GameRuntimeLibrary.runBudgetCli(parameters: String, rt: AshRuntimeContext) {
    val trimmed = parameters.trim()
    if (trimmed.isNotEmpty()) {
        trimmed.toIntOrNull()?.let { ConcoctionDatabase.setPullsBudgeted(it) }
    }
    rt.print(
        "${ConcoctionDatabase.getPullsBudgeted()} pulls budgeted for automatic use, " +
            "${ConcoctionDatabase.getPullsRemaining()} pulls remaining.",
    )
}

/** Desktop StorageCommand item-list pull — qty-optional comma lists (default qty 1). */
internal fun GameRuntimeLibrary.runPullCli(parameters: String, rt: AshRuntimeContext) {
    val request = storageRequest ?: return
    val state = character?.state?.value
    val inHardcore = state?.isHardcore == true
    val freepulls = if (inHardcore) {
        kotlinx.coroutines.runBlocking {
            request.fetchClassifiedContents(state, preferences).freepulls
        }
    } else {
        emptyMap()
    }
    var pulledItems = false
    for (raw in parameters.split(',')) {
        val piece = raw.trim()
        if (piece.isEmpty()) continue
        val tokens = piece.split(Regex("\\s+"), limit = 2)
        val leading = tokens.firstOrNull()?.toIntOrNull()
        val itemQuery: String
        val explicitQty: Int?
        if (leading != null && tokens.size == 2) {
            explicitQty = leading
            itemQuery = tokens[1]
        } else {
            explicitQty = null
            itemQuery = piece
        }
        if (itemQuery.equals("meat", ignoreCase = true)) {
            if (inHardcore) continue
            val qty = (explicitQty ?: 1).coerceAtLeast(1)
            kotlinx.coroutines.runBlocking {
                refreshStorageCacheAfter(request.pullMeat(qty))
            }
            continue
        }
        val itemId = resolveMallBuyItemId(itemQuery) ?: continue
        val qty = if (inHardcore) {
            val freeCount = freepulls[itemId] ?: continue
            if (freeCount <= 0) continue
            (explicitQty ?: freeCount).coerceAtLeast(1).coerceAtMost(freeCount)
        } else {
            (explicitQty ?: 1).coerceAtLeast(1)
        }
        val result = kotlinx.coroutines.runBlocking {
            val withdrawResult = request.withdraw(itemId, qty)
            refreshStorageCacheAfter(withdrawResult)
            withdrawResult
        }
        if (result.isSuccess) pulledItems = true
    }
    if (pulledItems && !inHardcore && !StoragePullRules.canInteract(state)) {
        val pulls = ConcoctionDatabase.getPullsRemaining()
        if (pulls >= 0) {
            val pullWord = if (pulls == 1) "pull" else "pulls"
            rt.print(
                "$pulls $pullWord remaining, " +
                    "${ConcoctionDatabase.getPullsBudgeted()} budgeted for automatic use.",
            )
        }
    }
}

internal fun looksLikeVisitUrl(cmd: String): Boolean {
    val trimmed = cmd.trim()
    return trimmed.contains(".php", ignoreCase = true) ||
        trimmed.startsWith("http://", ignoreCase = true) ||
        trimmed.startsWith("https://", ignoreCase = true)
}

internal fun normalizeKolVisitPath(raw: String): String {
    var url = raw.trim()
    val origins = listOf(
        "$KOL_BASE_URL/",
        "https://www.kingdomofloathing.com/",
        "http://www.kingdomofloathing.com/",
        "https://kingdomofloathing.com/",
        "http://kingdomofloathing.com/",
    )
    for (origin in origins) {
        if (url.startsWith(origin, ignoreCase = true)) {
            url = url.substring(origin.length)
            break
        }
    }
    return url.trimStart('/')
}

private fun stripHtmlForCli(html: String): String =
    html.replace(Regex("<[^>]+>"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()

internal fun GameRuntimeLibrary.runVisitUrlCli(rawUrl: String, printHtml: Boolean, rt: AshRuntimeContext) {
    val path = normalizeKolVisitPath(rawUrl)
    if (path.isBlank()) return
    val html = visitKolPage(path)
    if (printHtml && html != null) {
        val stripped = stripHtmlForCli(html)
        if (stripped.isNotBlank()) rt.print(stripped)
    }
}

internal fun GameRuntimeLibrary.runAcquireCli(
    parameters: String,
    rt: AshRuntimeContext,
    checkOnly: Boolean = false,
) {
    val parts = parameters.split(',').map { it.trim() }.filter { it.isNotBlank() }
    val simCtx = if (checkOnly) acquireSimulatorContext() else null
    for (part in parts) {
        val (qty, name) = parseAcquireQtyItem(part)
        val itemId = resolveCliItemName(name) ?: continue
        if (checkOnly && simCtx != null) {
            val method = RetrieveItemSimulator.simRetrieve(itemId, qty, simCtx)
            rt.print("$name: $method")
        } else {
            kotlinx.coroutines.runBlocking { retrieveItemService?.retrieve(itemId, qty) }
        }
    }
}

private fun GameRuntimeLibrary.acquireSimulatorContext(): RetrieveItemSimulator.Context {
    val prefs = preferences
    return RetrieveItemSimulator.Context(
        inventoryCount = { inventoryCount(it) },
        closetContents = prefs?.let { CollectionCache.load(it, Preferences.CACHED_CLOSET) } ?: emptyMap(),
        storageContents = prefs?.let { CollectionCache.load(it, Preferences.CACHED_STORAGE) } ?: emptyMap(),
        displayContents = prefs?.let { CollectionCache.load(it, Preferences.CACHED_DISPLAY) } ?: emptyMap(),
        stashContents = prefs?.let { CollectionCache.load(it, Preferences.CACHED_STASH) } ?: emptyMap(),
    )
}

internal fun parseAcquireQtyItem(raw: String): Pair<Int, String> {
    val trimmed = raw.trim()
    val space = trimmed.indexOf(' ')
    if (space > 0) {
        val qty = trimmed.substring(0, space).toIntOrNull()
        if (qty != null) return qty to trimmed.substring(space + 1).trim()
    }
    return 1 to trimmed
}

internal fun GameRuntimeLibrary.runCountersCli(parameters: String, rt: AshRuntimeContext) {
    val prefs = preferences ?: return
    val currentRun = character?.state?.value?.currentRun ?: 0
    val params = parameters.trim()
    when {
        params.equals("clear", ignoreCase = true) -> {
            TurnCounter.save(prefs, emptyList())
        }
        params.startsWith("add ", ignoreCase = true) -> {
            var rest = params.substring(4).trim()
            var image = "watch.gif"
            var title = "Manual"
            if (rest.endsWith(".gif", ignoreCase = true)) {
                val lastSpace = rest.lastIndexOf(' ')
                if (lastSpace >= 0) {
                    image = rest.substring(lastSpace + 1)
                    rest = rest.substring(0, lastSpace).trim()
                }
            }
            val spacePos = rest.indexOf(' ')
            val turns: Int
            if (spacePos != -1) {
                title = rest.substring(spacePos + 1)
                turns = rest.substring(0, spacePos).trim().toIntOrNull() ?: 0
            } else {
                turns = rest.toIntOrNull() ?: 0
            }
            TurnCounter.startCounting(prefs, currentRun, turns, title, image)
        }
        params.startsWith("stop ", ignoreCase = true) -> {
            TurnCounter.stopCounting(prefs, params.substring(5).trim())
        }
        params.startsWith("warn ", ignoreCase = true) -> {
            TurnCounter.addWarning(prefs, params.substring(5).trim())
        }
        params.startsWith("nowarn ", ignoreCase = true) -> {
            TurnCounter.removeWarning(prefs, params.substring(7).trim())
        }
    }
    val formatted = TurnCounter.formatRelayCounters(prefs, currentRun)
    if (formatted.isNotBlank()) rt.print(formatted)
}

internal fun GameRuntimeLibrary.runCampgroundActionCli(parameters: String, rt: AshRuntimeContext) {
    val tokens = parameters.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (tokens.isEmpty()) return
    val action = tokens[0]
    if (action.equals("rest", ignoreCase = true)) {
        runCampgroundRestCli(tokens.drop(1), rt)
        return
    }
    val count = tokens.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val state = character?.state?.value
    val client = httpClient ?: return
    kotlinx.coroutines.runBlocking {
        if (state?.inNuclearAutumn == true) {
            val falloutAction = if (action.equals("terminal", ignoreCase = true)) {
                FalloutShelterRequest.VAULT_TERMINAL
            } else {
                action
            }
            repeat(count) {
                FalloutShelterRequest(client).visitAction(falloutAction)
            }
            return@runBlocking
        }
        if (state == null || !CampgroundAvailability.haveCampground(state)) {
            rt.print("You don't have a campground right now.")
            return@runBlocking
        }
        val req = CampgroundRequest(client)
        repeat(count) {
            req.visitAction(action)
        }
    }
}

internal fun GameRuntimeLibrary.runRestCli(parameters: String, rt: AshRuntimeContext) {
    val tokens = parameters.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    runCampgroundRestCli(tokens, rt)
}

internal fun GameRuntimeLibrary.runCampgroundRestCli(restTokens: List<String>, rt: AshRuntimeContext) {
    val blocked = setOf("chateau", "campaway", "free")
    if (restTokens.any { it.lowercase() in blocked }) {
        rt.print("campground rest is not available")
        return
    }
    val count = restTokens.lastOrNull()?.toIntOrNull()?.coerceAtLeast(1) ?: 1
    val explicitVault = restTokens.any { it.equals("vault", ignoreCase = true) }
    val state = character?.state?.value
    val client = httpClient ?: return
    kotlinx.coroutines.runBlocking {
        if (explicitVault || state?.inNuclearAutumn == true) {
            repeat(count) {
                FalloutShelterRequest(client).visitAction(FalloutShelterRequest.VAULT1)
            }
            return@runBlocking
        }
        if (state == null || !CampgroundAvailability.haveCampground(state)) {
            rt.print("You don't have a campground right now.")
            return@runBlocking
        }
        val req = CampgroundRequest(client)
        repeat(count) {
            req.visitAction("rest")
        }
    }
}

internal fun GameRuntimeLibrary.runRestoresCli(parameters: String, rt: AshRuntimeContext) {
    val level = parameters.trim().ifBlank { "available" }.lowercase()
    if (level !in setOf("all", "available", "obtainable")) {
        rt.print("Valid parameters are all, available or obtainable")
        return
    }
    val cached = restoreCachedCounts()
    for (restore in RestoreDatabase.all()) {
        if (!restoreMatchesLevel(restore, level, cached)) continue
        val uses = restore.usesLeftExpr.ifBlank { "Unlimited" }
        rt.print(
            "${restore.name} | ${restore.type} | ${restore.hpMinExpr}-${restore.hpMaxExpr} | " +
                "${restore.mpMinExpr}-${restore.mpMaxExpr} | ${restore.advCost} | $uses | ${restore.notes}",
        )
    }
}

private fun GameRuntimeLibrary.restoreCachedCounts(): Map<String, Map<Int, Int>> {
    val prefs = preferences ?: return emptyMap()
    return mapOf(
        "closet" to CollectionCache.load(prefs, Preferences.CACHED_CLOSET),
        "storage" to CollectionCache.load(prefs, Preferences.CACHED_STORAGE),
        "display" to CollectionCache.load(prefs, Preferences.CACHED_DISPLAY),
        "stash" to CollectionCache.load(prefs, Preferences.CACHED_STASH),
    )
}

private fun GameRuntimeLibrary.restoreMatchesLevel(
    restore: RestoreData,
    level: String,
    cached: Map<String, Map<Int, Int>>,
): Boolean {
    if (level == "all") return true
    return when (restore.type) {
        RestoreType.ITEM -> {
            val itemId = resolveCliItemName(restore.name) ?: return false
            if (inventoryCount(itemId) > 0) return true
            if (level != "obtainable") return false
            cached.values.any { (it[itemId] ?: 0) > 0 }
        }
        RestoreType.SKILL -> {
            skillManager?.state?.value?.skills?.any {
                it.name.equals(restore.name, ignoreCase = true)
            } == true
        }
        RestoreType.LOC, RestoreType.UNKNOWN -> false
    }
}

internal fun GameRuntimeLibrary.runAshRefCli(filter: String, rt: AshRuntimeContext) {
    val scope = AshScope()
    registerAll(scope)
    val needle = filter.trim().lowercase()
    for (fn in scope.listFunctions()) {
        val matches = needle.isEmpty() ||
            fn.name.lowercase().contains(needle) ||
            fn.params.any { it.second.toString().lowercase().contains(needle) }
        if (!matches) continue
        val args = fn.params.joinToString(", ") { (pname, ptype) -> "$ptype $pname" }
        rt.print("${fn.returnType} ${fn.name}( $args )")
    }
}

internal fun GameRuntimeLibrary.runColorEchoCli(parameters: String, rt: AshRuntimeContext) {
    val spaceIndex = parameters.indexOf(' ')
    if (spaceIndex == -1) return
    rt.print(parameters.substring(spaceIndex + 1))
}

internal fun GameRuntimeLibrary.runEventsCli(parameters: String, rt: AshRuntimeContext) {
    if (parameters.trim().equals("clear", ignoreCase = true)) {
        EventHistory.clear()
        return
    }
    for (line in EventHistory.texts()) {
        rt.print(line)
    }
}

internal fun GameRuntimeLibrary.runPrefRefCli(parameters: String, rt: AshRuntimeContext) {
    val prefs = preferences ?: return
    val tokens = parameters.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    val searchText = tokens.firstOrNull().orEmpty()
    val isRegex = tokens.any { it.equals("regex", ignoreCase = true) }
    val pattern = if (isRegex) {
        try {
            Regex(searchText)
        } catch (_: IllegalArgumentException) {
            return
        }
    } else {
        null
    }
    val needle = searchText.lowercase()
    for (name in prefs.storedKeys().sorted()) {
        val matches = if (pattern != null) {
            pattern.containsMatchIn(name)
        } else {
            needle.isEmpty() || name.lowercase().contains(needle)
        }
        if (!matches) continue
        val value = prefs.getString(name)
        val default = if (DefaultsDatabase.has(name)) DefaultsDatabase.getString(name) else "N/A"
        rt.print("$name = $value ($default)")
    }
}

internal fun GameRuntimeLibrary.runPoolSkillCli(rt: AshRuntimeContext) {
    val drunk = character?.state?.value?.inebriety ?: 0
    val drunkBonus = drunk - if (drunk > 10) (drunk - 10) * 3 else 0
    val equip = buildCurrentModifiers().values.get(DoubleModifier.POOL_SKILL).toInt()
    val poolsSharked = preferences?.getInt("poolSharkCount", 0) ?: 0
    val poolSharkBonus = when {
        poolsSharked > 25 -> 10
        poolsSharked > 0 -> kotlin.math.floor(2 * kotlin.math.sqrt(poolsSharked.toDouble())).toInt()
        else -> 0
    }
    val training = preferences?.getInt("poolSkill", 0) ?: 0
    val poolSkill = equip + training + poolSharkBonus + drunkBonus
    rt.print("Pool Skill is estimated at : $poolSkill.")
    rt.print(
        "$equip from equipment, $drunkBonus from having $drunk inebriety, " +
            "$training hustling training and $poolSharkBonus learning from $poolsSharked sharks.",
    )
}

internal val IMPLEMENTED_CLI_COMMANDS = listOf(
    "aa", "abort", "absorb", "absorptions", "accordions", "acquire", "actionbar", "adv", "adventure", "alias", "alliedradio",
    "ash", "ashq", "ashref", "ashwiki", "ascensionhistory", "attack", "autoattack", "automall", "autosell", "autumnaton", "backupcamera",
    "badmoon", "bake", "bang", "banishes", "baron", "basement", "beach", "bjornify", "boombox", "bootskin",
    "bootspur", "bounty", "breakfast", "budget", "buff", "buffbot", "bugbears", "burn", "buy", "cache",
    "call", "campground", "cardsleeve", "cargo", "cast", "ccs", "cheapest", "cheat", "checkpoint", "chew",
    "chewqueue", "chibi", "chips", "choice", "choice-goal", "cleanup", "closet", "cmc", "coinmaster", "condition", "condref",
    "clan", "complete",
    "council", "counters", "create", "createqueue", "crimbotrain", "csend", "dad", "demons", "devilcandyegg", "display", "donate",
    "drink", "drinkqueue", "drinksilent", "dusty", "dvorak", "eat", "eatqueue", "eatsilent", "echo", "editmood",
    "edpiece", "effects", "else", "elseif", "encounters", "enthrone", "equip", "events", "exit", "expensive",
    "fallguy", "fax", "faxbot", "fecho", "field", "find", "flea", "fleamarket", "flicker", "florist", "fold", "folders", "foresee",
    "fprint", "garden", "get", "ghostqueue", "gift", "gong", "gooskills", "gourd", "grandpa", "greyyou",
    "hagnk", "heist", "help", "hermit", "hoboqueue", "holiday", "horsery", "hottub", "if", "ingredients",
    "inv", "inventory", "jillcandle", "journey", "junk", "kgb", "kmail", "latte", "leaves", "ledcandle", "leprecondo",
    "location", "locations", "logecho", "logout", "logprint", "lookup", "macro", "mail", "make", "mallbuy",
    "mallsell", "maximize", "mayam", "mcd", "min", "mind-control", "mix", "modifiers", "modifies", "modref",
    "monsters", "mood", "moon", "moons", "mummery", "nemesis", "note", "numberology", "ocean", "olfact",
    "olfaction", "outfit", "overdrink", "panda", "parka", "ping", "pingpong", "play", "ply", "prefref", "print", "profile",
    "pull", "pulverize", "putty", "pvp", "quark", "quit", "raffle", "recipe", "recover", "refresh",
    "relog", "relogin", "remedy", "reminisce", "remove", "repeat", "reprice", "rest", "restore", "retrieve",
    "retrocape", "roboequeue", "saber", "safe", "searchmall", "sell", "send", "servant", "servants", "session",
    "set", "shrug", "skeeball", "skill", "skills", "slime-stack", "slime-stacks", "slimestack", "slimelingqueue", "smash", "smith", "snapper", "snowsuit",
    "soak", "spade", "speculate", "spookyraven", "squeeze", "stash", "stash-log", "status", "sticker", "stickers", "storage", "summary", "summon",
    "sven", "taleofdread", "tavern", "teatree", "terminal", "thralls", "throw", "timein", "timeout", "timespinner",
    "tcrs", "tinker", "train", "trigger", "try", "umbrella", "unalias", "undercut", "uneffect", "unequip", "untinker",
    "use", "usequeue", "validate", "verify", "version", "vise", "volcano", "wait", "waitq", "which",
    "while", "wiki", "witchess", "zap",
)

internal fun GameRuntimeLibrary.runHelpCli(parameters: String, rt: AshRuntimeContext) {
    val leftover = parameters.trim()
    if (leftover.isEmpty() || leftover.equals("help", ignoreCase = true)) {
        rt.print("help [filter]")
        rt.print("Parameters in [brackets] are optional.")
    }
    for (name in IMPLEMENTED_CLI_COMMANDS) {
        if (leftover.isNotEmpty() && !name.contains(leftover, ignoreCase = true)) continue
        rt.print(name)
    }
    if (leftover.isEmpty() || leftover.equals("help", ignoreCase = true) || isNonGoalHelpTopic(leftover)) {
        rt.print("GUI/Relay, JavaScript, full TCRS dumps, and desktop scripting are not available in KoLmafia Mobile.")
    }
}

private fun isNonGoalHelpTopic(leftover: String): Boolean {
    val needle = leftover.lowercase()
    return needle.contains("relay") ||
        needle.contains("javascript") ||
        needle == "js" ||
        needle.contains("tcrs") ||
        needle.contains("script") ||
        needle.contains("gui")
}

internal fun GameRuntimeLibrary.runModRefCli(parameters: String, rt: AshRuntimeContext) {
    val trimmed = parameters.trim()
    var filter = ""
    var itemValues: net.sourceforge.kolmafia.modifiers.ModifierValues? = null
    if (trimmed.isNotEmpty()) {
        val asItem = ModifierDatabase.getItem(trimmed)
        if (asItem != null) {
            itemValues = ModifierParser.parse(asItem.modifiers)
        } else {
            val tokens = trimmed.split(Regex("\\s+"), limit = 2)
            filter = tokens[0]
            if (tokens.size == 2) {
                ModifierDatabase.getItem(tokens[1])?.let {
                    itemValues = ModifierParser.parse(it.modifiers)
                }
            }
        }
    }
    val player = buildCurrentModifiers().values
    fun emit(name: String, playerText: String, itemText: String?) {
        if (filter.isNotEmpty() && !name.contains(filter, ignoreCase = true)) return
        if (itemText != null) {
            rt.print("$name: $playerText | $itemText")
        } else {
            rt.print("$name: $playerText")
        }
    }
    fun formatDouble(value: Double): String {
        val asInt = value.toInt()
        return if (value == asInt.toDouble()) asInt.toString() else value.toString()
    }
    for (mod in DoubleModifier.entries) {
        emit(
            mod.tag,
            formatDouble(player.get(mod)),
            itemValues?.let { formatDouble(it.get(mod)) },
        )
    }
    for (mod in BooleanModifier.entries) {
        emit(
            mod.tag,
            player.get(mod).toString(),
            itemValues?.let { it.get(mod).toString() },
        )
    }
}

internal fun GameRuntimeLibrary.runReminisceCli(parameters: String, rt: AshRuntimeContext) {
    val spec = parameters.trim()
    if (spec.isEmpty()) {
        rt.print("No monster specified.")
        return
    }
    val monster = spec.toIntOrNull()?.let { id ->
        gameDatabase?.monster(id) ?: MonsterDatabase.getById(id)
    } ?: gameDatabase?.monster(spec) ?: MonsterDatabase.getByName(spec)
    if (monster == null) {
        rt.print("$spec does not match a monster.")
        return
    }
    val locketCount = runBlocking {
        physicalAccessibleCount(LocketRequest.LOCKET_ITEM_ID, LocketRequest.LOCKET_NAME)
    }
    if (locketCount <= 0) {
        rt.print("You do not own a combat lover's locket.")
        return
    }
    val fought = preferences?.getString(LocketRequest.PREF_FOUGHT, "").orEmpty()
        .split(',')
        .mapNotNull { it.trim().toIntOrNull() }
    if (fought.size >= 3) {
        rt.print("You can only reminisce thrice daily.")
        return
    }
    if (monster.id in fought) {
        rt.print("You've already reminisced ${monster.name} today.")
        return
    }
    val client = httpClient ?: return
    val result = runBlocking { LocketRequest(client).reminisce(monster.id) }
    if (result.isSuccess) {
        val updated = (fought + monster.id).joinToString(",")
        preferences?.setString(LocketRequest.PREF_FOUGHT, updated)
    }
}

internal fun GameRuntimeLibrary.runInsultsCli(rt: AshRuntimeContext) {
    val prefs = preferences ?: return
    rt.print("Known insults:")
    val known = PirateInsults.knownRetorts(prefs)
    if (known.isEmpty()) {
        rt.print("None.")
    } else {
        rt.print("")
        known.forEach { rt.print(it) }
    }
    val count = known.size
    val noun = if (count == 1) "insult" else "insults"
    val odds = PirateInsults.formatOddsPercent(count)
    rt.print("")
    rt.print(
        "Since you know $count $noun, you have a $odds% chance of winning at Insult Beer Pong.",
    )
}

internal fun GameRuntimeLibrary.runRecoverCli(target: String, rt: AshRuntimeContext) {
    val rm = recoveryManager ?: return
    val char = character ?: return
    val key = target.trim().lowercase()
    val doHp = key == "hp" || key == "health" || key == "both"
    val doMp = key == "mp" || key == "mana" || key == "both"
    kotlinx.coroutines.runBlocking {
        if (doHp) {
            val cs = char.state.value
            rm.checkpointedRecoverHp(
                cs.currentHp + 1,
                cs,
                inventoryManager?.state?.value ?: InventoryState(),
                skillManager?.state?.value ?: SkillState(),
            ) { refreshCharacterStates() }
        }
        if (doMp) {
            val cs = char.state.value
            rm.checkpointedRecoverMp(
                cs.currentMp + 1,
                cs,
                inventoryManager?.state?.value ?: InventoryState(),
                skillManager?.state?.value ?: SkillState(),
            ) { refreshCharacterStates() }
        }
        characterRequest?.fetchCharacterState()?.onSuccess { char.updateFromApiResponse(it) }
    }
}

internal fun GameRuntimeLibrary.runChoiceCli(parameters: String, rt: AshRuntimeContext) {
    val raw = parameters.trim()
    if (raw.isEmpty()) return
    var tokens = raw.split(Regex("\\s+")).filter { it.isNotBlank() }
    var always = false
    if (tokens.lastOrNull()?.equals("always", ignoreCase = true) == true) {
        always = true
        tokens = tokens.dropLast(1)
    }
    if (tokens.isEmpty()) return
    val extras = linkedMapOf<String, String>()
    val numeric = mutableListOf<Int>()
    for (tok in tokens) {
        val eq = tok.indexOf('=')
        if (eq > 0) {
            extras[tok.substring(0, eq)] = tok.substring(eq + 1)
        } else {
            val n = tok.toIntOrNull()
            if (n != null) {
                numeric.add(n)
            } else {
                rt.print("Field '$tok' must have a value; ignoring.")
            }
        }
    }
    val choiceId: Int
    val option: Int
    when (numeric.size) {
        2 -> {
            choiceId = numeric[0]
            option = numeric[1]
        }
        1 -> {
            choiceId = preferences?.getInt(AdventureManager.LAST_CHOICE_ID, 0) ?: 0
            if (choiceId <= 0) return
            option = numeric[0]
        }
        else -> return
    }
    if (always) {
        val prefs = preferences
        if (prefs != null) {
            val pref = "choiceAdventure$choiceId"
            val value = if (extras.isEmpty()) {
                option.toString()
            } else {
                option.toString() + "&" + extras.entries.joinToString("&") { "${it.key}=${it.value}" }
            }
            prefs.setString(pref, value)
            rt.print("$pref => $value")
        }
    }
    cliChoice(choiceId, option, extras)
}

private fun GameRuntimeLibrary.hasAccessibleItem(itemId: Int): Boolean {
    if (inventoryCount(itemId) > 0) return true
    return equipmentSlotOf(itemId) != null
}

private fun GameRuntimeLibrary.equipmentSlotOf(itemId: Int): EquipmentSlot? {
    val name = gameDatabase?.item(itemId)?.name ?: ItemDatabase.getById(itemId)?.name ?: return null
    return character?.state?.value?.equipment?.entries
        ?.firstOrNull { it.value.equals(name, ignoreCase = true) }
        ?.key
}

/** Desktop UnequipCommand — bare/all, slot, or item-name substring match. */
internal fun GameRuntimeLibrary.runUnequipCli(parameters: String, rt: AshRuntimeContext) {
    val trimmed = parameters.trim()
    if (trimmed.isEmpty() || trimmed.equals("all", ignoreCase = true)) {
        kotlinx.coroutines.runBlocking { equipmentRequest?.unequipAll() }
        return
    }
    val firstToken = trimmed.split(Regex("\\s+")).first()
    val knownSlot = EquipmentSlot.entries.find { s ->
        s.apiKey.equals(firstToken, ignoreCase = true)
    } ?: when {
        firstToken.equals("familiar", ignoreCase = true) -> EquipmentSlot.FAMILIAR
        firstToken.equals("off-hand", ignoreCase = true) -> EquipmentSlot.OFFHAND
        else -> null
    }
    if (knownSlot != null) {
        kotlinx.coroutines.runBlocking {
            val req = equipmentRequest
            if (req != null) {
                req.unequipSlot(knownSlot)
            } else {
                inventoryManager?.unequipSlot(knownSlot.apiKey)
            }
        }
        return
    }
    val query = trimmed.lowercase()
    val equipment = character?.state?.value?.equipment.orEmpty()
    var matched = false
    for ((slot, name) in equipment) {
        if (!name.lowercase().contains(query)) continue
        matched = true
        kotlinx.coroutines.runBlocking {
            val req = equipmentRequest
            if (req != null) {
                req.unequipSlot(slot)
            } else {
                inventoryManager?.unequipSlot(slot.apiKey)
            }
        }
    }
    if (!matched) {
        rt.print("Unknown unequip target: $trimmed")
    }
}

/** Desktop CoinmasterCommand — qty-optional comma item lists after nickname. */
internal fun GameRuntimeLibrary.runCoinmasterTradeCli(
    isBuy: Boolean,
    nickname: String,
    itemParams: String,
) {
    val master = coinmasterManager?.resolveMaster(nickname) ?: return
    for (raw in itemParams.split(',')) {
        val piece = raw.trim()
        if (piece.isEmpty()) continue
        val tokens = piece.split(Regex("\\s+"), limit = 2)
        val leading = tokens.firstOrNull()?.toIntOrNull()
        val itemQuery: String
        val qty: Int
        if (leading != null && tokens.size == 2) {
            qty = leading
            itemQuery = tokens[1]
        } else {
            qty = 1
            itemQuery = piece
        }
        val itemId = resolveMallBuyItemId(itemQuery) ?: continue
        if (qty <= 0) continue
        kotlinx.coroutines.runBlocking {
            if (isBuy) coinmasterManager?.buy(master, itemId, qty)
            else coinmasterManager?.sell(master, itemId, qty)
        }
    }
}

/** Desktop CreateItemCommand — bare creatable list, or qty-optional comma item lists. */
internal fun GameRuntimeLibrary.runCreateCli(parameters: String, rt: AshRuntimeContext) {
    val trimmed = parameters.trim()
    if (trimmed.isEmpty()) {
        val creatables = ConcoctionDatabase.getCreatables()
        if (creatables.isEmpty()) {
            rt.print("nothing creatable")
            return
        }
        for (entry in creatables) {
            rt.print("${entry.resultName} (${entry.creatable})")
        }
        return
    }
    for (raw in trimmed.split(',')) {
        val piece = raw.trim()
        if (piece.isEmpty()) continue
        val tokens = piece.split(Regex("\\s+"), limit = 2)
        val leading = tokens.firstOrNull()?.toIntOrNull()
        val itemQuery: String
        val qty: Int
        if (leading != null && tokens.size == 2) {
            qty = leading
            itemQuery = tokens[1]
        } else {
            qty = 1
            itemQuery = piece
        }
        val itemId = resolveMallBuyItemId(itemQuery) ?: continue
        if (qty <= 0) continue
        kotlinx.coroutines.runBlocking { createItem(itemId, qty) }
    }
}
