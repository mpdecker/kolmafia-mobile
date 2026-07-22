package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.data.CultShortsDatabase
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.PocketDatabase
import net.sourceforge.kolmafia.data.PocketDatabase.MonsterPocket
import net.sourceforge.kolmafia.data.PocketDatabase.OneResultPocket
import net.sourceforge.kolmafia.data.PocketDatabase.Pocket
import net.sourceforge.kolmafia.data.PocketDatabase.PocketType
import net.sourceforge.kolmafia.data.PocketDatabase.StatsPocket
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CargoCultistShortsRequest

/** Orchestrates desktop-style `cargo` CLI / cargo cultist shorts HTTP. */
open class CargoCultManager(
    private val preferences: Preferences,
    private val request: CargoCultistShortsRequest,
    private val pocketSync: CargoPocketSync,
    private val yegDemonNameSync: YegDemonNameSync,
    private val inventoryManager: InventoryManager?,
) {

    fun printPickedPockets(print: (String) -> Unit) {
        val pockets = pocketSync.pickedPocketIds()
        if (pockets.isEmpty()) {
            print("You haven't emptied any pockets yet this ascension.")
        } else {
            print(pockets.joinToString(", "))
        }
    }

    suspend fun run(
        parameters: String,
        inventoryState: InventoryState,
        characterState: CharacterState?,
        print: (String) -> Unit,
    ): Result<Unit> {
        val trimmed = parameters.trim()
        if (trimmed.isEmpty()) {
            printPickedPockets(print)
            return Result.success(Unit)
        }

        val split = trimmed.split(" ", limit = 2)
        val command = split[0].lowercase()
        val args = split.getOrNull(1).orEmpty().trim()

        return when (command) {
            "inspect" -> runInspect(inventoryState, characterState, print)
            "pick" -> runPick(args, inventoryState, characterState, print).map { }
            "demon" -> {
                printDemonScraps(print)
                Result.success(Unit)
            }
            "pocket" -> runDescribePocket(args, print)
            "count", "list" -> runCountOrList(command, args, print)
            "monster" -> runTypedPick(parseMonster(trimmed), inventoryState, characterState, print)
            "effect" -> runTypedPickEffect(parseEffect(trimmed), inventoryState, characterState, print)
            "item" -> runTypedPickItem(parseItem(trimmed), inventoryState, characterState, print)
            "stat" -> runTypedPickStat(parseStat(trimmed), inventoryState, characterState, print)
            else -> {
                val pocket = command.toIntOrNull()
                if (pocket != null) {
                    runPick(pocket.toString(), inventoryState, characterState, print).map { }
                } else {
                    print("Unknown cargo command: $parameters")
                    Result.failure(IllegalArgumentException("unknown cargo command"))
                }
            }
        }
    }

    suspend fun pickPocketNumber(
        pocket: Int,
        inventoryState: InventoryState,
        characterState: CharacterState?,
        print: (String) -> Unit = {},
    ): Boolean = runPick(pocket.toString(), inventoryState, characterState, print).isSuccess

    private suspend fun runInspect(
        inventoryState: InventoryState,
        characterState: CharacterState?,
        print: (String) -> Unit,
    ): Result<Unit> {
        if (!hasShorts(inventoryState, characterState)) {
            print("You don't have a pair of Cargo Cultist Shorts available.")
            return Result.failure(IllegalStateException("no shorts"))
        }
        return request.inspect().fold(
            onSuccess = { html ->
                pocketSync.parseAvailablePockets(html)
                printPickedPockets(print)
                Result.success(Unit)
            },
            onFailure = { Result.failure(it) },
        )
    }

    private fun runDescribePocket(args: String, print: (String) -> Unit): Result<Unit> {
        val pocket = parsePocketNumber(args, print) ?: return Result.failure(IllegalArgumentException("invalid pocket"))
        print("Pocket #${pocket.pocket} contains $pocket")
        return Result.success(Unit)
    }

    private fun runCountOrList(command: String, args: String, print: (String) -> Unit): Result<Unit> {
        val usage = "cargo $command ( type TYPE | unpicked TYPE | monster MONSTER | effect EFFECT | item ITEM | stat STAT )"
        val parts = args.split(" ", limit = 2)
        if (parts.size < 2) {
            print(usage)
            return Result.failure(IllegalArgumentException("usage"))
        }
        val subset = parts[0].lowercase()
        val remainder = parts[1].trim()
        return when (subset) {
            "type", "unpicked" -> runCountByType(command, subset, remainder, print)
            "monster" -> runCountMonster(command, parseMonster("$subset $remainder"), print)
            "effect" -> runCountEffect(command, parseEffect("$subset $remainder"), print)
            "item" -> runCountItem(command, parseItem("$subset $remainder"), print)
            "stat" -> runCountStat(command, parseStat("$subset $remainder"), print)
            else -> {
                print(usage)
                Result.failure(IllegalArgumentException("usage"))
            }
        }
    }

    private fun runCountByType(
        command: String,
        subset: String,
        tag: String,
        print: (String) -> Unit,
    ): Result<Unit> {
        val type = parsePocketType(tag, print) ?: return Result.failure(IllegalArgumentException("bad type"))
        var pockets = PocketDatabase.getPockets(type) ?: return Result.failure(IllegalStateException("no pockets"))
        val modifier = if (subset == "unpicked") {
            pockets = PocketDatabase.removePickedPockets(pockets, pocketSync.pickedPocketIds())
            " unpicked "
        } else {
            " "
        }
        print("There are ${pockets.size}$modifier$tag pockets.")
        if (command == "list") {
            printPockets(PocketDatabase.sortPockets(type, pockets), print)
        }
        return Result.success(Unit)
    }

    private fun runCountMonster(command: String, monsterName: String?, print: (String) -> Unit): Result<Unit> {
        val pocket = getMonsterPocket(monsterName, print) ?: return Result.failure(IllegalArgumentException("bad monster"))
        if (command == "count") {
            print("There is one pocket that contains a '$monsterName'.")
        } else {
            printPocket(pocket, print)
        }
        return Result.success(Unit)
    }

    private fun runCountEffect(command: String, effectName: String?, print: (String) -> Unit): Result<Unit> {
        val pockets = getEffectPockets(effectName, print) ?: return Result.failure(IllegalArgumentException("bad effect"))
        val plural = pockets.size != 1
        print(
            "There ${if (plural) "are " else "is "}${pockets.size} pocket${if (plural) "s" else ""} " +
                "that grant${if (plural) "" else "s"} the '$effectName' effect.",
        )
        if (command == "list") {
            printPockets(PocketDatabase.sortResults(effectName!!, pockets), print)
        }
        return Result.success(Unit)
    }

    private fun runCountItem(command: String, itemName: String?, print: (String) -> Unit): Result<Unit> {
        val pockets = getItemPockets(itemName, print) ?: return Result.failure(IllegalArgumentException("bad item"))
        val plural = pockets.size != 1
        print(
            "There ${if (plural) "are " else "is "}${pockets.size} pocket${if (plural) "s" else ""} " +
                "that contain${if (plural) "" else "s"} a '$itemName'.",
        )
        if (command == "list") {
            printPockets(PocketDatabase.sortResults(itemName!!, pockets), print)
        }
        return Result.success(Unit)
    }

    private fun runCountStat(command: String, stat: String?, print: (String) -> Unit): Result<Unit> {
        val pockets = getStatsPockets(stat, print) ?: return Result.failure(IllegalArgumentException("bad stat"))
        val plural = pockets.size != 1
        print(
            "There ${if (plural) "are " else "is "}${pockets.size} pocket${if (plural) "s" else ""} " +
                "that contain${if (plural) "" else "s"} '$stat' stats.",
        )
        if (command == "list") {
            printPockets(PocketDatabase.sortStats(stat!!, pockets), print)
        }
        return Result.success(Unit)
    }

    private suspend fun runTypedPick(
        monsterName: String?,
        inventoryState: InventoryState,
        characterState: CharacterState?,
        print: (String) -> Unit,
    ): Result<Unit> {
        val pocket = getMonsterPocket(monsterName, print) ?: return Result.failure(IllegalArgumentException("bad monster"))
        return runPick(pocket.pocket.toString(), inventoryState, characterState, print).map { }
    }

    private suspend fun runTypedPickEffect(
        effectName: String?,
        inventoryState: InventoryState,
        characterState: CharacterState?,
        print: (String) -> Unit,
    ): Result<Unit> {
        val pockets = getEffectPockets(effectName, print) ?: return Result.failure(IllegalArgumentException("bad effect"))
        val sorted = PocketDatabase.sortResults(effectName!!, pockets)
        val pocket = firstUnpickedPocket(effectName, sorted, print) ?: return Result.failure(IllegalStateException("none"))
        return runPick(pocket.pocket.toString(), inventoryState, characterState, print).map { }
    }

    private suspend fun runTypedPickItem(
        itemName: String?,
        inventoryState: InventoryState,
        characterState: CharacterState?,
        print: (String) -> Unit,
    ): Result<Unit> {
        val pockets = getItemPockets(itemName, print) ?: return Result.failure(IllegalArgumentException("bad item"))
        val sorted = PocketDatabase.sortResults(itemName!!, pockets)
        val pocket = firstUnpickedPocket(itemName, sorted, print) ?: return Result.failure(IllegalStateException("none"))
        return runPick(pocket.pocket.toString(), inventoryState, characterState, print).map { }
    }

    private suspend fun runTypedPickStat(
        stat: String?,
        inventoryState: InventoryState,
        characterState: CharacterState?,
        print: (String) -> Unit,
    ): Result<Unit> {
        val pockets = getStatsPockets(stat, print) ?: return Result.failure(IllegalArgumentException("bad stat"))
        val sorted = PocketDatabase.sortStats(stat!!, pockets)
        val pocket = firstUnpickedPocket(stat, sorted, print) ?: return Result.failure(IllegalStateException("none"))
        return runPick(pocket.pocket.toString(), inventoryState, characterState, print).map { }
    }

    private suspend fun runPick(
        args: String,
        inventoryState: InventoryState,
        characterState: CharacterState?,
        print: (String) -> Unit,
    ): Result<Unit> {
        val pocketNum = args.toIntOrNull()
        if (pocketNum == null || pocketNum !in 1..666) {
            print("cargo pick POCKET")
            return Result.failure(IllegalArgumentException("invalid pocket"))
        }
        if (!hasShorts(inventoryState, characterState)) {
            print("You don't have a pair of Cargo Cultist Shorts available.")
            return Result.failure(IllegalStateException("no shorts"))
        }
        if (preferences.getBoolean(Preferences.CARGO_POCKET_EMPTIED, false)) {
            print("You've already looted a pocket from your Cargo Cultist Shorts today.")
            return Result.failure(IllegalStateException("daily exhausted"))
        }
        if (pocketSync.pickedPocketIds().contains(pocketNum)) {
            print("You've already emptied that pocket this ascension.")
            return Result.failure(IllegalStateException("already picked"))
        }

        return request.pickPocket(pocketNum).fold(
            onSuccess = { html ->
                if (html.contains("the power of the pockets has been exhausted for the day")) {
                    print("You already picked a pocket today.")
                    pocketSync.parsePocketPick(pocketNum, html)
                    return@fold Result.failure(IllegalStateException("daily exhausted"))
                }
                pocketSync.parsePocketPick(pocketNum, html)
                print("Emptied pocket #$pocketNum.")
                Result.success(Unit)
            },
            onFailure = { Result.failure(it) },
        )
    }

    private fun printDemonScraps(print: (String) -> Unit) {
        val known = yegDemonNameSync.knownScrapPockets()
        val ordered = CultShortsDatabase.scrapPocketsInOrder
        val pockets = if (ordered.isNotEmpty()) ordered else known.keys.sorted()
        for (pocket in pockets) {
            val syllable = known[pocket]
            if (syllable == null) {
                print("Pocket #$pocket: unknown")
            } else {
                print("Pocket #$pocket: $syllable")
            }
        }
    }

    private fun firstUnpickedPocket(name: String, pockets: List<Pocket>, print: (String) -> Unit): Pocket? {
        val result = PocketDatabase.firstUnpickedPocket(pockets, pocketSync.pickedPocketIds())
        if (result == null) {
            print("No unpicked pockets contain '$name'.")
        }
        return result
    }

    private fun printPockets(pockets: Collection<Pocket>, print: (String) -> Unit) {
        for (pocket in pockets) {
            printPocket(pocket, print)
        }
    }

    private fun printPocket(pocket: Pocket, print: (String) -> Unit) {
        print("Pocket #${pocket.pocket}: $pocket")
    }

    private fun parsePocketNumber(input: String, print: (String) -> Unit): Pocket? {
        val pocketNum = input.toIntOrNull()
        if (pocketNum == null) {
            print("Specify a pocket # from 1-666")
            return null
        }
        if (pocketNum !in 1..666) {
            print("Pocket must be from 1-666")
            return null
        }
        return PocketDatabase.pocketByNumber(pocketNum)
    }

    private fun parsePocketType(tag: String, print: (String) -> Unit): PocketType? {
        val type = PocketDatabase.getPocketType(tag)
        if (type == null) {
            print("What is type '$tag'?")
        }
        return type
    }

    private fun parseMonster(parameters: String): String? {
        val name = parseName("monster", parameters)
        if (name.isEmpty()) return null
        val monster = if (name.all { it.isDigit() }) {
            MonsterDatabase.getById(name.toIntOrNull() ?: return null)
        } else {
            MonsterDatabase.getByName(name)
        }
        if (monster == null) {
            return null
        }
        return monster.name
    }

    private fun parseEffect(parameters: String): String? {
        val name = parseName("effect", parameters)
        if (name.isEmpty()) return null
        val resolved = if (name.all { it.isDigit() }) {
            EffectDatabase.getById(name.toIntOrNull() ?: return null)?.name
        } else {
            EffectDatabase.getByName(name)?.name
        }
        return resolved
    }

    private fun parseItem(parameters: String): String? {
        val name = parseName("item", parameters)
        if (name.isEmpty()) return null
        val resolved = if (name.all { it.isDigit() }) {
            ItemDatabase.getById(name.toIntOrNull() ?: return null)?.name
        } else {
            ItemDatabase.getByName(name)?.name
        }
        return resolved
    }

    private fun parseStat(parameters: String): String? = parseName("stat", parameters).ifEmpty { null }

    private fun parseName(type: String, parameters: String): String {
        val index = parameters.indexOf("$type ", ignoreCase = true)
        if (index == -1) return ""
        return parameters.substring(parameters.indexOf(' ', index)).trim()
    }

    private fun getMonsterPocket(monsterName: String?, print: (String) -> Unit): MonsterPocket? {
        if (monsterName == null) return null
        val pocket = PocketDatabase.monsterPockets[monsterName.lowercase()]
        if (pocket == null) {
            print("Your shorts do not contain a monster named '$monsterName'.")
        }
        return pocket
    }

    private fun getEffectPockets(effectName: String?, print: (String) -> Unit): Set<OneResultPocket>? {
        if (effectName == null) return null
        val pockets = PocketDatabase.effectPockets[effectName]
        if (pockets == null) {
            print("Your shorts do not contain an effect named '$effectName'.")
        }
        return pockets
    }

    private fun getItemPockets(itemName: String?, print: (String) -> Unit): Set<OneResultPocket>? {
        if (itemName == null) return null
        val pockets = PocketDatabase.itemPockets[itemName]
        if (pockets == null) {
            print("Your shorts do not contain an item named '$itemName'.")
        }
        return pockets
    }

    private fun getStatsPockets(stat: String?, print: (String) -> Unit): Set<StatsPocket>? {
        if (stat == null) return null
        val pockets = PocketDatabase.statsPockets[stat.lowercase()]
        if (pockets == null) {
            print("Your shorts do not produce stat '$stat'.")
        }
        return pockets
    }

    fun hasShorts(inventoryState: InventoryState, characterState: CharacterState?): Boolean {
        if (inventoryState.items.containsKey(BreakfastItemIds.CARGO_CULTIST_SHORTS_ID)) return true
        if (inventoryState.items.containsKey(BreakfastItemIds.REPLICA_CARGO_CULTIST_SHORTS_ID)) {
            return true
        }
        val pants = characterState?.equippedItem(EquipmentSlot.PANTS).orEmpty()
        return pants.contains("Cargo Cultist Shorts", ignoreCase = true)
    }
}
