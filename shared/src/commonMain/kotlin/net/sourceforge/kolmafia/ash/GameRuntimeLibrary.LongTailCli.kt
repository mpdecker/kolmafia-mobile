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
import net.sourceforge.kolmafia.data.BountyDatabase
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
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.maximizer.MaximizerContinuation
import net.sourceforge.kolmafia.mood.MoodRemovalKnownSources
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CampgroundRequest
import net.sourceforge.kolmafia.request.ClanRumpusRequest
import net.sourceforge.kolmafia.request.FalloutShelterRequest
import net.sourceforge.kolmafia.request.FoldItemRequest
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.session.EventHistory
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

internal fun GameRuntimeLibrary.runAbortCli(message: String, rt: AshRuntimeContext) {
    val text = message.trim().ifBlank { "Script abort." }
    rt.print(text)
    MaximizerContinuation.abort()
    adventureManager?.stop()
    throw ScriptException(text)
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
    val tokens = parameters.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    if (tokens.isEmpty()) {
        rt.print("Which stickers?")
        return
    }
    val equipment = character?.state?.value?.equipment ?: emptyMap()
    var index = 0
    for (slot in EquipmentSlot.STICKER_SLOTS) {
        val occupied = equipment[slot]?.isNotBlank() == true
        if (occupied) continue
        if (index >= tokens.size) break
        val name = LongTailCli.stickerEquipName(tokens[index++])
        cliEquip("${slot.apiKey} $name")
    }
}

internal fun GameRuntimeLibrary.runConditionCli(parameters: String, rt: AshRuntimeContext) {
    val raw = parameters.trim()
    if (raw.isEmpty()) return
    val lower = raw.lowercase()
    when {
        lower == "clear" -> goalManager?.clearGoals()
        lower == "substats" -> goalManager?.setSubstatsGoal(true)
        lower.startsWith("meat ") -> {
            val n = raw.substringAfter(' ').trim().toIntOrNull() ?: return
            goalManager?.setMeatGoal(n)
        }
        lower.startsWith("level ") -> {
            val n = raw.substringAfter(' ').trim().toIntOrNull() ?: return
            goalManager?.setLevelGoal(n)
        }
        lower.startsWith("choice ") -> {
            val n = raw.substringAfter(' ').trim().toIntOrNull() ?: return
            goalManager?.setChoiceGoal(n)
        }
        else -> {
            val rest = raw.removePrefix("item ").removePrefix("Item ").trim()
            if (rest.isNotEmpty()) goalManager?.addItemGoalByName(rest)
        }
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
