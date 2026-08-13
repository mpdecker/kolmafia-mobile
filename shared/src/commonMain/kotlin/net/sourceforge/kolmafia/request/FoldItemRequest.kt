package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.FoldGroupDatabase
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ItemPrimaryUse
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.recovery.RecoveryManager
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState

/** Desktop FoldItemCommand — inv_use fold loop, garbage tote choice 1275, equipped specials. */
open class FoldItemRequest(
    private val client: HttpClient,
    private val useItemRequest: UseItemRequest? = null,
    private val choiceRequest: ChoiceRequest? = null,
    private val equipmentRequest: EquipmentRequest? = null,
    private val inventoryManager: InventoryManager? = null,
    private val retrieveItemService: RetrieveItemService? = null,
    private val recoveryManager: RecoveryManager? = null,
    private val character: KoLCharacter? = null,
    private val skillManager: SkillManager? = null,
    private val preferences: Preferences? = null,
    private val gameDatabase: GameDatabase? = null,
) {
    open suspend fun fold(targetId: Int): Result<String> {
        val plan = FoldItemPlanner.plan(targetId, plannerContext())
        if (plan.error != null) return Result.failure(IllegalStateException(plan.error))
        if (plan.alreadyHave) return Result.success("have")
        plan.retrieveItemId?.let { id ->
            val got = retrieveItemService?.retrieve(id, 1) ?: 0
            if (got < 1 && inventoryCount(id) < 1) {
                return Result.failure(IllegalStateException("Unable to retrieve item $id"))
            }
        }
        val shouldUnequip = plan.unequipSlot != null &&
            plan.special != FoldItemPlanner.Special.BORIS_HELM &&
            plan.special != FoldItemPlanner.Special.JARLSBERG_PAN &&
            plan.special != FoldItemPlanner.Special.PETE_JACKET &&
            plan.special != FoldItemPlanner.Special.TOGGLE &&
            plan.legionSlot == null
        if (shouldUnequip) {
            plan.unequipSlot?.let { equipmentRequest?.unequipSlot(it) }
        }
        return when (plan.special) {
            FoldItemPlanner.Special.BORIS_HELM -> twistHorns(plan.unequipSlot ?: EquipmentSlot.HAT)
            FoldItemPlanner.Special.JARLSBERG_PAN -> getAction("inventory.php?action=shakepan")
            FoldItemPlanner.Special.PETE_JACKET -> getAction("inventory.php?action=popcollar")
            FoldItemPlanner.Special.TOGGLE ->
                getAction("inventory.php?action=togglebutt&slot=familiarequip&ajax=1")
            FoldItemPlanner.Special.LOATHING_LEGION -> foldLegion(plan)
            FoldItemPlanner.Special.GARBAGE_TOTE -> foldGarbageTote(plan)
            null -> foldUseLoop(plan)
        }
    }

    internal fun plannerContext(): FoldItemPlanner.Context {
        val state = character?.state?.value ?: net.sourceforge.kolmafia.character.CharacterState()
        return FoldItemPlanner.Context(
            inventoryCount = { id -> inventoryCount(id) },
            equippedSlot = { name ->
                state.equipment.entries.firstOrNull { it.value.equals(name, ignoreCase = true) }?.key
            },
            accessibleCount = { id ->
                val have = inventoryCount(id)
                if (have > 0) have else retrieveItemService?.let { 0 } ?: 0
            },
            skills = skillManager?.state?.value?.skills.orEmpty(),
            charState = state,
            preferences = preferences,
            itemId = { name ->
                gameDatabase?.item(name)?.id ?: ItemDatabase.getByName(name)?.id
            },
            itemName = { id ->
                gameDatabase?.item(id)?.name ?: ItemDatabase.getById(id)?.name
            },
            isShirt = { id ->
                (gameDatabase?.item(id) ?: ItemDatabase.getById(id))?.primaryUse == ItemPrimaryUse.SHIRT
            },
            isChefStaff = { name ->
                EquipmentDatabase.getByName(name)?.itemType?.contains("chefstaff", ignoreCase = true) == true
            },
            foldGroup = { FoldGroupDatabase.groupFor(it) },
        )
    }

    private suspend fun foldUseLoop(plan: FoldItemPlanner.Plan): Result<String> {
        val damage = plan.hpDamage
        val char = character
        if (damage > 0 && char != null && recoveryManager != null) {
            val hp = char.state.value.currentHp
            if (hp > 0 && hp < damage) {
                val skills = skillManager?.state?.value ?: SkillState()
                val inv = inventoryManager?.state?.value ?: net.sourceforge.kolmafia.inventory.InventoryState()
                recoveryManager.checkpointedRecoverHp(
                    damage.toInt(),
                    char.state.value,
                    inv,
                    skills,
                ) {
                    Triple(char.state.value, inventoryManager?.state?.value ?: inv, skillManager?.state?.value ?: skills)
                }
            }
        }
        var last = ""
        for (itemId in plan.useItemIds) {
            val result = useItemRequest?.use(itemId, 1) ?: getUse(itemId)
            last = result.getOrElse { return result }
        }
        return Result.success(last)
    }

    private suspend fun foldGarbageTote(plan: FoldItemPlanner.Plan): Result<String> {
        val tote = plan.toteItemId ?: return Result.failure(IllegalStateException("missing tote"))
        val option = plan.toteOption ?: return Result.failure(IllegalStateException("missing tote option"))
        useItemRequest?.use(tote, 1) ?: getUse(tote).getOrElse { return Result.failure(it) }
        val choice = choiceRequest ?: return Result.failure(IllegalStateException("choice unavailable"))
        return choice.choose(FoldItemPlanner.GARBAGE_CHOICE, option).map { it.first }
    }

    private suspend fun foldLegion(plan: FoldItemPlanner.Plan): Result<String> {
        val source = plan.sourceItemId ?: return Result.failure(IllegalStateException("missing legion source"))
        val targetName = plan.checkOnlyLine?.substringAfter(" => ")?.trim().orEmpty()
        val encoded = encode(targetName)
        val eq = plan.legionSlot?.apiKey
        val url = buildString {
            append("inv_use.php?switch=1&whichitem=$source&fold=$encoded")
            if (eq != null) append("&eq=$eq")
        }
        val result = getAction(url)
        if (result.isSuccess && plan.legionSlot != null && targetName.isNotBlank()) {
            character?.updateEquipment(plan.legionSlot, targetName)
        }
        return result
    }

    private suspend fun twistHorns(slot: EquipmentSlot): Result<String> {
        val slotName = if (slot == EquipmentSlot.HAT) "hat" else "familiarequip"
        return getAction("inventory.php?action=twisthorns&slot=$slotName")
    }

    private suspend fun getUse(itemId: Int): Result<String> = try {
        val response = client.get("$KOL_BASE_URL/inv_use.php") {
            parameter("which", 3)
            parameter("whichitem", itemId)
            parameter("ajax", 1)
        }
        if (response.status.isSuccess()) Result.success(response.bodyAsText())
        else Result.failure(Exception("HTTP ${response.status.value}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun getAction(path: String): Result<String> = try {
        val url = if (path.startsWith("http")) path else "$KOL_BASE_URL/$path"
        val response = client.get(url)
        if (response.status.isSuccess()) Result.success(response.bodyAsText())
        else Result.failure(Exception("HTTP ${response.status.value}"))
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun inventoryCount(itemId: Int): Int =
        inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0

    private fun encode(value: String): String = buildString {
        for (ch in value) {
            when (ch) {
                ' ' -> append('+')
                in 'A'..'Z', in 'a'..'z', in '0'..'9', '-', '_', '.', '~' -> append(ch)
                else -> {
                    val bytes = ch.toString().encodeToByteArray()
                    for (b in bytes) append('%').append(((b.toInt() and 0xFF).toString(16).uppercase()).padStart(2, '0'))
                }
            }
        }
    }
}
