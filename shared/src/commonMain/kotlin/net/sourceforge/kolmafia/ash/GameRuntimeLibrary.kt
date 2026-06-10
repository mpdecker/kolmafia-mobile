package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.equipment.OutfitCheckpoint
import net.sourceforge.kolmafia.equipment.OutfitManager
import net.sourceforge.kolmafia.request.CraftRequest
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.effect.EffectState
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.AutosellRequest
import net.sourceforge.kolmafia.request.ChewRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DrinkBoozeRequest
import net.sourceforge.kolmafia.request.EatFoodRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.shop.CoinmasterManager
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.request.ManageStoreRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.skill.SkillState
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random

class GameRuntimeLibrary(
    internal val character: KoLCharacter? = null,
    internal val inventoryManager: InventoryManager? = null,
    internal val skillManager: SkillManager? = null,
    internal val effectManager: EffectManager? = null,
    internal val adventureManager: AdventureManager? = null,
    // new params — all nullable so forTesting() and existing tests still compile
    internal val familiarManager: FamiliarManager? = null,
    internal val goalManager: GoalManager? = null,
    internal val moodManager: MoodManager? = null,
    internal val preferences: Preferences? = null,
    internal val gameDatabase: GameDatabase? = null,
    internal val useItemRequest: UseItemRequest? = null,
    internal val eatFoodRequest: EatFoodRequest? = null,
    internal val drinkBoozeRequest: DrinkBoozeRequest? = null,
    internal val chewRequest: ChewRequest? = null,
    internal val autosellRequest: AutosellRequest? = null,
    internal val closetRequest: ClosetRequest? = null,
    internal val storageRequest: StorageRequest? = null,
    internal val banishManager: BanishManager? = null,
    internal val httpClient: HttpClient? = null,
    internal val hermitRequest: HermitRequest? = null,
    internal val displayCaseRequest: DisplayCaseRequest? = null,
    internal val clanStashRequest: ClanStashRequest? = null,
    internal val mallManager: MallManager? = null,
    internal val retrieveItemService: RetrieveItemService? = null,
    internal val outfitManager: OutfitManager? = null,
    internal val equipmentRequest: EquipmentRequest? = null,
    internal val coinmasterManager: CoinmasterManager? = null,
    internal val craftRequest: CraftRequest? = null,
    internal val manageStoreRequest: ManageStoreRequest? = null,
    internal val mallPriceManager: MallPriceManager? = null,
) : RuntimeLibrary() {

    companion object {
        /** Used in tests where no game managers are needed. */
        fun forTesting() = GameRuntimeLibrary()
    }

    private val cliDispatch: List<Pair<Regex, (MatchResult, AshRuntimeContext) -> Unit>> = listOf(

        // "mood execute" — run missing triggers for active mood
        Regex("^mood\\s+execute$", RegexOption.IGNORE_CASE) to { _, _ ->
            moodManager?.let { mood ->
                kotlinx.coroutines.runBlocking {
                    mood.executeActiveMood(
                        effectState = effectManager?.state?.value ?: EffectState(),
                        skillState  = skillManager?.state?.value  ?: SkillState(),
                        charState   = character?.state?.value     ?: CharacterState(),
                    )
                }
            }
        },

        // "mood <name>" — set active mood by name, then execute
        Regex("^mood\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val name = m.groupValues[1].trim()
            moodManager?.setActiveMoodByName(name)
            moodManager?.let { mood ->
                kotlinx.coroutines.runBlocking {
                    mood.executeActiveMood(
                        effectState = effectManager?.state?.value ?: EffectState(),
                        skillState  = skillManager?.state?.value  ?: SkillState(),
                        charState   = character?.state?.value     ?: CharacterState(),
                    )
                }
            }
        },

        // "set key=value" — write a preference string
        Regex("^set\\s+(.+?)\\s*=\\s*(.*)$") to { m, _ ->
            preferences?.setString(m.groupValues[1].trim(), m.groupValues[2])
        },

        // "get key" — read and print a preference string
        Regex("^get\\s+(.+)$") to { m, rt ->
            val value = preferences?.getString(m.groupValues[1].trim(), "") ?: ""
            rt.print(value)
        },

        // "cast N skill-name" — cast a skill N times (count form: silent no-op if unknown)
        Regex("^cast\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val count = m.groupValues[1].toIntOrNull() ?: 1
            val skillName = m.groupValues[2].trim()
            val skill = skillManager?.state?.value?.skills
                ?.find { it.name.equals(skillName, ignoreCase = true) }
            if (skill != null) {
                kotlinx.coroutines.runBlocking { skillManager!!.cast(skill, count) }
            }
            // skill not found → silent no-op (no echo for count form)
        },

        // "cast skill-name" — cast a skill once (no count prefix; echo if unknown)
        Regex("^cast\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val skillName = m.groupValues[1].trim()
            val skill = skillManager?.state?.value?.skills
                ?.find { it.name.equals(skillName, ignoreCase = true) }
            if (skill != null) {
                kotlinx.coroutines.runBlocking { skillManager!!.cast(skill, 1) }
            } else {
                rt.print("[cli] cast $skillName")  // unknown skill → echo
            }
        },

        // "familiar name" — switch to a familiar by species name
        Regex("^familiar\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val name = m.groupValues[1].trim()
            kotlinx.coroutines.runBlocking { familiarManager?.setFamiliar(name) }
        },

        // "enthrone name" / "enthrone none" — Crown of Thrones
        Regex("^enthrone\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val name = m.groupValues[1].trim()
            kotlinx.coroutines.runBlocking { familiarManager?.setEnthroned(name) }
        },

        // "bjornify name" / "bjornify none" — Buddy Bjorn
        Regex("^bjornify\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val name = m.groupValues[1].trim()
            kotlinx.coroutines.runBlocking { familiarManager?.setBjornified(name) }
        },

        // "retrieve N item" — compound retrieve chain
        Regex("^retrieve\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val count = m.groupValues[1].toIntOrNull() ?: return@to
            val itemName = m.groupValues[2].trim()
            val itemId = gameDatabase?.item(itemName)?.id ?: return@to
            kotlinx.coroutines.runBlocking { retrieveItemService?.retrieve(itemId, count) }
        },

        // "use N item" — use item from inventory
        Regex("^use\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemName = m.groupValues[2].trim()
            val itemId = gameDatabase?.item(itemName)?.id ?: return@to
            kotlinx.coroutines.runBlocking { useItemRequest?.use(itemId, qty) }
        },

        // "use item" — use one copy
        Regex("^use\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val itemName = m.groupValues[1].trim()
            val itemId = gameDatabase?.item(itemName)?.id ?: return@to
            kotlinx.coroutines.runBlocking { useItemRequest?.use(itemId, 1) }
        },

        // "eat N item" / "eat item"
        Regex("^eat\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { eatFoodRequest?.eat(itemId, qty) }
        },
        Regex("^eat\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val itemId = gameDatabase?.item(m.groupValues[1].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { eatFoodRequest?.eat(itemId, 1) }
        },

        // "drink N item" / "drink item"
        Regex("^drink\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { drinkBoozeRequest?.drink(itemId, qty) }
        },
        Regex("^drink\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val itemId = gameDatabase?.item(m.groupValues[1].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { drinkBoozeRequest?.drink(itemId, 1) }
        },

        // "chew N item" / "chew item"
        Regex("^chew\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { chewRequest?.chew(itemId, qty) }
        },
        Regex("^chew\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val itemId = gameDatabase?.item(m.groupValues[1].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { chewRequest?.chew(itemId, 1) }
        },

        // "eatsilent N item" / "drinksilent N item"
        Regex("^eatsilent\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { eatFoodRequest?.eat(itemId, qty) }
        },
        Regex("^drinksilent\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { drinkBoozeRequest?.drink(itemId, qty) }
        },

        // "visit <coinmaster>" — open coinmaster shop page
        Regex("^visit\\s+(\\S+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val master = coinmasterManager?.resolveMaster(m.groupValues[1].trim()) ?: return@to
            kotlinx.coroutines.runBlocking { coinmasterManager?.visit(master) }
        },

        // closet / display / stash put & take
        Regex("^closet\\s+put\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { closetRequest?.putIn(itemId, qty) }
        },
        Regex("^closet\\s+take\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { closetRequest?.takeOut(itemId, qty) }
        },
        Regex("^storage\\s+take\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { storageRequest?.withdraw(itemId, qty) }
        },
        Regex("^display\\s+put\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { displayCaseRequest?.putIn(itemId, qty) }
        },
        Regex("^display\\s+take\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { displayCaseRequest?.takeOut(itemId, qty) }
        },
        Regex("^stash\\s+put\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { clanStashRequest?.putIn(itemId, qty) }
        },
        Regex("^stash\\s+take\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { clanStashRequest?.takeOut(itemId, qty) }
        },

        // goal add/remove/clear
        Regex("^goal\\s+add\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.addItemGoalByName(m.groupValues[1].trim())
        },
        Regex("^goal\\s+remove\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.removeGoal(m.groupValues[1].trim())
        },
        Regex("^goal\\s+clear$", RegexOption.IGNORE_CASE) to { _, _ ->
            goalManager?.clearGoals()
        },

        // "putshop price[@limit] N item" — list item in mall store
        Regex("^putshop\\s+(\\d+)(?:@(\\d+))?\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val price = m.groupValues[1].toIntOrNull() ?: return@to
            val limit = m.groupValues[2].toIntOrNull() ?: 0
            val qty = m.groupValues[3].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[4].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking {
                manageStoreRequest?.addItem(itemId, price, limit, qty)
            }
        },

        // "equip [<slot>] <item-name>" — equip item, optionally into a named slot.
        // If the first word after "equip" is a known slot apiKey, treat it as a slot and the
        // remainder as the item name; if that item lookup fails, fall back to trying the full
        // remainder as an item name with "default" slot. Unknown item → silent no-op.
        Regex("^equip\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val rest = m.groupValues[1].trim()
            val spaceIdx = rest.indexOf(' ')

            if (spaceIdx > 0) {
                val firstToken = rest.substring(0, spaceIdx)
                val afterFirst = rest.substring(spaceIdx + 1).trim()
                val knownSlot = EquipmentSlot.entries.find { s ->
                    s.apiKey.equals(firstToken, ignoreCase = true)
                }
                if (knownSlot != null) {
                    // "equip <slot> <item>" — try afterFirst as item name in named slot
                    val item = inventoryManager?.state?.value?.items?.values
                        ?.find { it.name.equals(afterFirst, ignoreCase = true) }
                    if (item != null) {
                        kotlinx.coroutines.runBlocking { inventoryManager?.equipItem(item, knownSlot.apiKey) }
                        return@to
                    }
                    // slot was recognised but item not found — could be item name starting with a slot word
                    // fall through to try full rest as item name with "default" slot
                }
            }

            // "equip <item>" — no slot, or slot+item fallback
            val item = inventoryManager?.state?.value?.items?.values
                ?.find { it.name.equals(rest, ignoreCase = true) }
            if (item != null) {
                kotlinx.coroutines.runBlocking { inventoryManager?.equipItem(item, "default") }
            }
            // not in backpack → silent no-op
        },

        // "unequip <slot>" — remove equipped item from a slot
        Regex("^unequip\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val slotName = m.groupValues[1].trim()
            kotlinx.coroutines.runBlocking { inventoryManager?.unequipSlot(slotName) }
        },

        // "sell N <item>" or "autosell N <item>" — autosell N copies
        Regex("^(?:sell|autosell)\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemName = m.groupValues[2].trim()
            val itemId = gameDatabase?.item(itemName)?.id ?: return@to
            kotlinx.coroutines.runBlocking { autosellRequest?.autosell(itemId, qty) }
        },

        // "outfit save <name>" — save current equipment as custom outfit
        Regex("^outfit\\s+save\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val name = m.groupValues[1].trim()
            kotlinx.coroutines.runBlocking { outfitManager?.saveOutfit(name) }
        },

        // "outfit list [filter]" — print outfit names matching optional filter
        Regex("^outfit\\s+list(?:\\s+(.*))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            val filter = m.groupValues[1].trim().lowercase()
            kotlinx.coroutines.runBlocking {
                val names = outfitManager?.getOutfitsWithPieces()?.map { it.name } ?: emptyList()
                names.filter { filter.isEmpty() || it.lowercase().contains(filter) }
                    .forEach { rt.print(it) }
            }
        },

        // "outfit checkpoint" — restore equipment from last checkpoint
        Regex("^outfit\\s+checkpoint$", RegexOption.IGNORE_CASE) to { _, _ ->
            val char = character ?: return@to
            val equip = equipmentRequest ?: return@to
            val db = gameDatabase ?: return@to
            kotlinx.coroutines.runBlocking {
                OutfitCheckpoint.restoreSaved(equip, db)
            }
        },

        // "outfit <name>" — wear named outfit (runs embedded c=/e=/f= actions after wear)
        Regex("^outfit\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val name = m.groupValues[1].trim()
            if (name.equals("list", ignoreCase = true) ||
                name.equals("checkpoint", ignoreCase = true) ||
                name.startsWith("save ", ignoreCase = true)
            ) return@to
            kotlinx.coroutines.runBlocking {
                outfitManager?.wearOutfit(name) { cmd -> dispatchCli(cmd, rt) }
            }
        },

        // "buy using storage N ¶itemId[@limit]" — HC/Ronin mall buy tracked via storage
        Regex("^buy\\s+using\\s+storage\\s+(\\d+)\\s+[\\u00B6¶](\\d+)(?:@(\\d+))?$", RegexOption.IGNORE_CASE) to { m, _ ->
            val count = m.groupValues[1].toIntOrNull() ?: return@to
            val itemId = m.groupValues[2].toIntOrNull() ?: return@to
            val limit = m.groupValues[3].toIntOrNull() ?: Int.MAX_VALUE
            val mall = mallManager ?: return@to
            val char = character ?: return@to
            val equip = equipmentRequest ?: return@to
            val db = gameDatabase ?: return@to
            val cs = char.state.value
            if (!cs.isHardcore && !cs.isInRonin) return@to
            kotlinx.coroutines.runBlocking {
                val initial = storageRequest?.fetchContents()?.get(itemId) ?: 0
                val checkpoint = OutfitCheckpoint.snapshot(char, equip, db)
                checkpoint.use { mall.buy(itemId, count, limit) }
                storageRequest?.fetchContents()?.get(itemId) ?: initial
            }
        },

        // "coinmaster buy <nick> <item>" / "coinmaster sell <nick> <item>"
        Regex("^coinmaster\\s+(buy|sell)\\s+(\\S+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val isBuy = m.groupValues[1].equals("buy", ignoreCase = true)
            val nickname = m.groupValues[2].trim()
            val itemName = m.groupValues[3].trim()
            val master = coinmasterManager?.resolveMaster(nickname) ?: return@to
            val itemId = gameDatabase?.item(itemName)?.id ?: return@to
            kotlinx.coroutines.runBlocking {
                if (isBuy) coinmasterManager?.buy(master, itemId, 1)
                else coinmasterManager?.sell(master, itemId, 1)
            }
        },

        // "create N item" — compound retrieve (includes craft/coinmaster/mall)
        Regex("^create\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val count = m.groupValues[1].toIntOrNull() ?: return@to
            val itemName = m.groupValues[2].trim()
            val itemId = gameDatabase?.item(itemName)?.id ?: return@to
            kotlinx.coroutines.runBlocking { retrieveItemService?.retrieve(itemId, count) }
        },

        // "buy N item[@limit]" — checkpoint-wrapped mall purchase
        Regex("^buy\\s+(\\d+)\\s+(.+?)(?:@(\\d+))?$", RegexOption.IGNORE_CASE) to { m, _ ->
            val count = m.groupValues[1].toIntOrNull() ?: return@to
            val itemName = m.groupValues[2].trim()
            val limit = m.groupValues[3].toIntOrNull() ?: Int.MAX_VALUE
            val itemId = gameDatabase?.item(itemName)?.id ?: return@to
            val mall = mallManager ?: return@to
            val char = character ?: return@to
            val equip = equipmentRequest ?: return@to
            val db = gameDatabase
            kotlinx.coroutines.runBlocking {
                val checkpoint = OutfitCheckpoint.snapshot(char, equip, db)
                checkpoint.use {
                    mall.buy(itemId, count, limit)
                }
            }
        },
    )

    internal fun dispatchCli(cmd: String, rt: AshRuntimeContext) {
        val matched = cliDispatch.firstOrNull { (regex, _) -> regex.matches(cmd) }
        if (matched != null) {
            matched.second(matched.first.find(cmd)!!, rt)
        } else {
            rt.print("[cli] $cmd")
        }
    }

    /** Bridges the protected [register] so extension functions in this module can call it. */
    internal fun regFn(
        scope: AshScope,
        name: String,
        returnType: AshType,
        params: List<Pair<String, AshType>>,
        impl: (AshRuntimeContext, List<AshValue>) -> AshValue
    ) = register(scope, name, returnType, params, impl)

    override fun registerAll(scope: AshScope) {
        super.registerAll(scope) // registers print() and to_string() overloads from stub
        registerTypeConversions(scope)
        registerStringUtils(scope)
        registerMathUtils(scope)
        registerAggregateUtils(scope)
        registerPrintUtils(scope)
        registerCharacterQueries(scope)
        registerItemQueries(scope)
        registerSkillQueries(scope)
        registerEffectQueries(scope)
        registerGameActions(scope)
        // new extension calls (added as tasks T4–T13 are implemented):
        registerCharacterExtensions(scope)
        registerFamiliarQueries(scope)
        registerEquipmentQueries(scope)
        registerModifierQueries(scope)
        registerCollectionQueries(scope)
        registerDateTimeQueries(scope)
        registerGoalQueries(scope)
        registerMoodQueries(scope)
        registerPreferenceAccess(scope)
        registerCombatStubs(scope)
        registerItemActions(scope)
        registerPricingQueries(scope)
        registerMallFunctions(scope)
        registerShopFunctions(scope)
        registerOutfitFunctions(scope)
        registerCoinmasterFunctions(scope)
        registerCraftFunctions(scope)
        registerBanishQueries(scope)
        registerWebRequests(scope)
        registerHermit(scope)
        registerTimingAndLogging(scope)
    }

    // ──────────────────────────────────────────────────────────────
    // Type conversion
    // ──────────────────────────────────────────────────────────────

    private fun registerTypeConversions(scope: AshScope) {
        // to_int
        register(scope, "to_int", AshType.INT, listOf("value" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().toLongOrNull() ?: 0L)
        }
        register(scope, "to_int", AshType.INT, listOf("value" to AshType.FLOAT)) { _, args ->
            AshValue.of(args[0].toLong())
        }
        register(scope, "to_int", AshType.INT, listOf("value" to AshType.BOOLEAN)) { _, args ->
            AshValue.of(if (args[0].toBoolean()) 1L else 0L)
        }
        register(scope, "to_int", AshType.INT, listOf("value" to AshType.INT)) { _, args ->
            args[0]
        }

        // to_float
        register(scope, "to_float", AshType.FLOAT, listOf("value" to AshType.INT)) { _, args ->
            AshValue.of(args[0].toDouble())
        }
        register(scope, "to_float", AshType.FLOAT, listOf("value" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().toDoubleOrNull() ?: 0.0)
        }
        register(scope, "to_float", AshType.FLOAT, listOf("value" to AshType.FLOAT)) { _, args ->
            args[0]
        }

        // to_boolean
        register(scope, "to_boolean", AshType.BOOLEAN, listOf("value" to AshType.INT)) { _, args ->
            AshValue.of(args[0].toLong() != 0L)
        }
        register(scope, "to_boolean", AshType.BOOLEAN, listOf("value" to AshType.STRING)) { _, args ->
            val s = args[0].toString()
            AshValue.of(s.isNotEmpty() && s != "false")
        }
        register(scope, "to_boolean", AshType.BOOLEAN, listOf("value" to AshType.BOOLEAN)) { _, args ->
            args[0]
        }

        // to_int for game entity types — returns the entity's numeric database ID
        // Returns 0 when gameDatabase is null (test/no-db context) or entity unknown.
        register(scope, "to_int", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
            AshValue.of(gameDatabase?.item(args[0].toString())?.id?.toLong() ?: 0L)
        }
        register(scope, "to_int", AshType.INT, listOf("ef" to AshType.EFFECT)) { _, args ->
            AshValue.of(gameDatabase?.effect(args[0].toString())?.id?.toLong() ?: 0L)
        }
        register(scope, "to_int", AshType.INT, listOf("sk" to AshType.SKILL)) { _, args ->
            AshValue.of(gameDatabase?.skill(args[0].toString())?.id?.toLong() ?: 0L)
        }
        register(scope, "to_int", AshType.INT, listOf("fa" to AshType.FAMILIAR)) { _, args ->
            AshValue.of(gameDatabase?.familiar(args[0].toString())?.id?.toLong() ?: 0L)
        }
        register(scope, "to_int", AshType.INT, listOf("loc" to AshType.LOCATION)) { _, args ->
            AshValue.of(
                gameDatabase?.zone(args[0].toString())
                    ?.snarfblat?.toIntOrNull()?.toLong() ?: 0L
            )
        }
        register(scope, "to_int", AshType.INT, listOf("mo" to AshType.MONSTER)) { _, args ->
            AshValue.of(gameDatabase?.monster(args[0].toString())?.id?.toLong() ?: 0L)
        }

        // to_string for game entity types
        for (entityType in listOf(
            AshType.ITEM, AshType.SKILL, AshType.EFFECT,
            AshType.FAMILIAR, AshType.LOCATION, AshType.MONSTER,
            AshType.CLASS, AshType.STAT, AshType.SLOT,
            AshType.ELEMENT, AshType.COINMASTER, AshType.PHYLUM, AshType.PATH
        )) {
            val capturedType = entityType
            register(scope, "to_string", AshType.STRING, listOf("value" to capturedType)) { _, args ->
                AshValue.of(args[0].toString())
            }
        }

        // to_location(string) → location — type conversion for locations
        register(scope, "to_location", AshType.LOCATION, listOf("name" to AshType.STRING)) { _, args ->
            AshValue.location(args[0].toString())
        }

        register(scope, "to_coinmaster", AshType.COINMASTER, listOf("name" to AshType.STRING)) { _, args ->
            AshValue(AshType.COINMASTER, args[0].toString())
        }
    }

    // ──────────────────────────────────────────────────────────────
    // String utilities
    // ──────────────────────────────────────────────────────────────

    private fun registerStringUtils(scope: AshScope) {
        register(scope, "length", AshType.INT, listOf("s" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().length)
        }
        register(scope, "substring", AshType.STRING,
            listOf("s" to AshType.STRING, "start" to AshType.INT, "end" to AshType.INT)) { _, args ->
            val s = args[0].toString()
            val start = args[1].toLong().toInt().coerceIn(0, s.length)
            // ASH end is inclusive: substring("hello",1,3) == "ell"
            val endInclusive = args[2].toLong().toInt().coerceIn(start - 1, s.length - 1)
            AshValue.of(s.substring(start, endInclusive + 1))
        }
        register(scope, "substring", AshType.STRING,
            listOf("s" to AshType.STRING, "start" to AshType.INT)) { _, args ->
            val s = args[0].toString()
            val start = args[1].toLong().toInt().coerceIn(0, s.length)
            AshValue.of(s.substring(start))
        }
        register(scope, "index_of", AshType.INT,
            listOf("source" to AshType.STRING, "search" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().indexOf(args[1].toString()).toLong())
        }
        register(scope, "index_of", AshType.INT,
            listOf("source" to AshType.STRING, "search" to AshType.STRING, "start" to AshType.INT)) { _, args ->
            val start = args[2].toLong().toInt()
            AshValue.of(args[0].toString().indexOf(args[1].toString(), start).toLong())
        }
        register(scope, "to_upper_case", AshType.STRING, listOf("s" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().uppercase())
        }
        register(scope, "to_lower_case", AshType.STRING, listOf("s" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().lowercase())
        }
        register(scope, "starts_with", AshType.BOOLEAN,
            listOf("s" to AshType.STRING, "prefix" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().startsWith(args[1].toString()))
        }
        register(scope, "ends_with", AshType.BOOLEAN,
            listOf("s" to AshType.STRING, "suffix" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().endsWith(args[1].toString()))
        }
        register(scope, "contains", AshType.BOOLEAN,
            listOf("s" to AshType.STRING, "sub" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().contains(args[1].toString()))
        }
        register(scope, "replace_string", AshType.STRING,
            listOf("s" to AshType.STRING, "old" to AshType.STRING, "new" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().replace(args[1].toString(), args[2].toString()))
        }
        register(scope, "trim", AshType.STRING, listOf("s" to AshType.STRING)) { _, args ->
            AshValue.of(args[0].toString().trim())
        }
        // split_string returns string[int] → AggregateType(indexType=INT, dataType=STRING)
        register(scope, "split_string", AggregateType(AshType.INT, AshType.STRING),
            listOf("s" to AshType.STRING, "sep" to AshType.STRING)) { _, args ->
            val parts = args[0].toString().split(args[1].toString())
            val result = AggregateValue(AggregateType(AshType.INT, AshType.STRING))
            parts.forEachIndexed { i, part -> result[AshValue.of(i)] = AshValue.of(part) }
            result
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Math utilities
    // ──────────────────────────────────────────────────────────────

    private fun registerMathUtils(scope: AshScope) {
        register(scope, "floor", AshType.INT, listOf("f" to AshType.FLOAT)) { _, args ->
            AshValue.of(floor(args[0].toDouble()).toLong())
        }
        register(scope, "ceil", AshType.INT, listOf("f" to AshType.FLOAT)) { _, args ->
            AshValue.of(ceil(args[0].toDouble()).toLong())
        }
        register(scope, "round", AshType.INT, listOf("f" to AshType.FLOAT)) { _, args ->
            AshValue.of(args[0].toDouble().roundToLong())
        }
        register(scope, "sqrt", AshType.FLOAT, listOf("f" to AshType.FLOAT)) { _, args ->
            AshValue.of(sqrt(args[0].toDouble()))
        }
        register(scope, "abs", AshType.INT, listOf("n" to AshType.INT)) { _, args ->
            AshValue.of(abs(args[0].toLong()))
        }
        register(scope, "abs", AshType.FLOAT, listOf("f" to AshType.FLOAT)) { _, args ->
            AshValue.of(abs(args[0].toDouble()))
        }
        register(scope, "max", AshType.INT, listOf("a" to AshType.INT, "b" to AshType.INT)) { _, args ->
            AshValue.of(maxOf(args[0].toLong(), args[1].toLong()))
        }
        register(scope, "max", AshType.FLOAT, listOf("a" to AshType.FLOAT, "b" to AshType.FLOAT)) { _, args ->
            AshValue.of(maxOf(args[0].toDouble(), args[1].toDouble()))
        }
        register(scope, "min", AshType.INT, listOf("a" to AshType.INT, "b" to AshType.INT)) { _, args ->
            AshValue.of(minOf(args[0].toLong(), args[1].toLong()))
        }
        register(scope, "min", AshType.FLOAT, listOf("a" to AshType.FLOAT, "b" to AshType.FLOAT)) { _, args ->
            AshValue.of(minOf(args[0].toDouble(), args[1].toDouble()))
        }
        register(scope, "random", AshType.FLOAT, listOf("limit" to AshType.FLOAT)) { _, args ->
            AshValue.of(Random.nextDouble() * args[0].toDouble())
        }
        register(scope, "pow", AshType.FLOAT, listOf("base" to AshType.FLOAT, "exp" to AshType.FLOAT)) { _, args ->
            AshValue.of(args[0].toDouble().pow(args[1].toDouble()))
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Aggregate utilities
    //
    // Fix: canCoerce already returns true for aggregate→aggregate (added in AshType),
    // so registering one concrete aggregate type as the parameter type is enough —
    // the resolver will accept any aggregate argument.
    // ──────────────────────────────────────────────────────────────

    private fun registerAggregateUtils(scope: AshScope) {
        // AshType.AGGREGATE is a sentinel: canCoerce(anyConcreteAggregate, AGGREGATE) == true,
        // so count/clear accept any aggregate type without needing per-type overloads.
        register(scope, "count", AshType.INT, listOf("agg" to AshType.AGGREGATE)) { _, args ->
            AshValue.of((args[0] as? AggregateValue)?.map?.size?.toLong() ?: 0L)
        }
        register(scope, "clear", AshType.VOID, listOf("agg" to AshType.AGGREGATE)) { _, args ->
            (args[0] as? AggregateValue)?.map?.clear()
            AshValue.VOID
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Print utilities
    // ──────────────────────────────────────────────────────────────

    private fun registerPrintUtils(scope: AshScope) {
        // print(string) already registered in super
        register(scope, "print_html", AshType.VOID, listOf("html" to AshType.STRING)) { runtime, args ->
            val stripped = args[0].toString()
                .replace(Regex("<[^>]+>"), "")
                .replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
            runtime.print(stripped)
            AshValue.VOID
        }
        register(scope, "print_to_string", AshType.STRING, listOf("value" to AshType.STRING)) { _, args ->
            args[0]
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Character state queries
    // ──────────────────────────────────────────────────────────────

    private fun registerCharacterQueries(scope: AshScope) {
        register(scope, "my_name", AshType.STRING, emptyList()) { _, _ ->
            AshValue.of(character?.state?.value?.name ?: "")
        }
        register(scope, "my_level", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.level ?: 1).toLong())
        }
        register(scope, "my_hp", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.currentHp ?: 0).toLong())
        }
        register(scope, "my_maxhp", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.maxHp ?: 0).toLong())
        }
        register(scope, "my_mp", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.currentMp ?: 0).toLong())
        }
        register(scope, "my_maxmp", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.maxMp ?: 0).toLong())
        }
        register(scope, "my_meat", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.meat ?: 0).toLong())
        }
        register(scope, "my_adventures", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.adventuresLeft ?: 0).toLong())
        }
        register(scope, "my_fullness", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.fullness ?: 0).toLong())
        }
        register(scope, "my_inebriety", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.inebriety ?: 0).toLong())
        }
        register(scope, "my_spleen_use", AshType.INT, emptyList()) { _, _ ->
            AshValue.of((character?.state?.value?.spleenUsed ?: 0).toLong())
        }
        register(scope, "is_headless", AshType.BOOLEAN, emptyList()) { _, _ ->
            AshValue.of(true)
        }
        register(scope, "my_basestat", AshType.INT, listOf("stat" to AshType.STAT)) { _, args ->
            val statName = args[0].toString().lowercase()
            val cs = character?.state?.value
            AshValue.of(when (statName) {
                "muscle"      -> (cs?.baseMusc ?: 0).toLong()
                "mysticality" -> (cs?.baseMyst ?: 0).toLong()
                "moxie"       -> (cs?.baseMoxie ?: 0).toLong()
                else          -> 0L
            })
        }
        register(scope, "in_hardcore", AshType.BOOLEAN, emptyList()) { _, _ ->
            AshValue.of(character?.state?.value?.isHardcore ?: false)
        }
        register(scope, "my_familiar", AshType.FAMILIAR, emptyList()) { _, _ ->
            AshValue.familiar(
                familiarManager?.state?.value?.activeFamiliar?.race
                    ?.takeIf { it.isNotBlank() } ?: "none"
            )
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Item queries
    // ──────────────────────────────────────────────────────────────

    private fun registerItemQueries(scope: AshScope) {
        register(scope, "item_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
            val name = args[0].toString()
            val qty = inventoryManager?.state?.value?.items?.values
                ?.find { it.name.equals(name, ignoreCase = true) }?.quantity ?: 0
            AshValue.of(qty.toLong())
        }
        register(scope, "available_amount", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
            // same as item_amount for mobile (no closet/storage distinction yet)
            val name = args[0].toString()
            val qty = inventoryManager?.state?.value?.items?.values
                ?.find { it.name.equals(name, ignoreCase = true) }?.quantity ?: 0
            AshValue.of(qty.toLong())
        }
        register(scope, "to_item", AshType.ITEM, listOf("name" to AshType.STRING)) { _, args ->
            AshValue.item(args[0].toString())
        }
        register(scope, "to_item", AshType.ITEM, listOf("id" to AshType.INT)) { _, args ->
            val id = args[0].toLong().toInt()
            val name = inventoryManager?.state?.value?.items?.values
                ?.find { it.itemId == id }?.name ?: id.toString()
            AshValue.item(name)
        }
        register(scope, "have_item", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
            val name = args[0].toString()
            val qty = inventoryManager?.state?.value?.items?.values
                ?.find { it.name.equals(name, ignoreCase = true) }?.quantity ?: 0
            AshValue.of(qty > 0)
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Skill queries
    // ──────────────────────────────────────────────────────────────

    private fun registerSkillQueries(scope: AshScope) {
        register(scope, "have_skill", AshType.BOOLEAN, listOf("sk" to AshType.SKILL)) { _, args ->
            val name = args[0].toString()
            val has = skillManager?.state?.value?.skills
                ?.any { it.name.equals(name, ignoreCase = true) } ?: false
            AshValue.of(has)
        }
        register(scope, "mp_cost", AshType.INT, listOf("sk" to AshType.SKILL)) { _, args ->
            val name = args[0].toString()
            val cost = skillManager?.state?.value?.skills
                ?.find { it.name.equals(name, ignoreCase = true) }?.mpCost ?: 0
            AshValue.of(cost.toLong())
        }
        register(scope, "to_skill", AshType.SKILL, listOf("name" to AshType.STRING)) { _, args ->
            AshValue.skill(args[0].toString())
        }
        register(scope, "daily_limit", AshType.INT, listOf("sk" to AshType.SKILL)) { _, args ->
            val name = args[0].toString()
            val limit = skillManager?.state?.value?.skills
                ?.find { it.name.equals(name, ignoreCase = true) }?.dailyLimit ?: 0
            AshValue.of(limit.toLong())
        }
        register(scope, "times_cast", AshType.INT, listOf("sk" to AshType.SKILL)) { _, args ->
            val name = args[0].toString()
            val cast = skillManager?.state?.value?.skills
                ?.find { it.name.equals(name, ignoreCase = true) }?.timesCast ?: 0
            AshValue.of(cast.toLong())
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Effect queries
    // ──────────────────────────────────────────────────────────────

    private fun registerEffectQueries(scope: AshScope) {
        register(scope, "have_effect", AshType.INT, listOf("ef" to AshType.EFFECT)) { _, args ->
            val name = args[0].toString()
            val duration = effectManager?.state?.value?.effects
                ?.find { it.name.equals(name, ignoreCase = true) }?.duration ?: 0
            AshValue.of(duration.toLong())
        }
        register(scope, "to_effect", AshType.EFFECT, listOf("name" to AshType.STRING)) { _, args ->
            AshValue.effect(args[0].toString())
        }
    }

    // ──────────────────────────────────────────────────────────────
    // Game actions (suspend-aware wrappers)
    // ──────────────────────────────────────────────────────────────

    private fun registerGameActions(scope: AshScope) {
        register(scope, "adventure", AshType.BOOLEAN,
            listOf("turns" to AshType.INT, "loc" to AshType.LOCATION)) { _, args ->
            val turns = args[0].toLong().toInt()
            val locName = args[1].toString()
            val manager = adventureManager
                ?: throw ScriptException("Adventure manager not available")
            val location = AdventureLocation(locName, locName, "")
            // Fix: call .join() so the script coroutine blocks until all turns complete.
            kotlinx.coroutines.runBlocking {
                manager.runAdventures(location, turns, this).join()
            }
            AshValue.of(true)
        }

        // adv1(loc: location, adventuresUsed: int) → boolean
        // Runs a single adventure at loc. Returns false if no AdventureManager.
        register(scope, "adv1", AshType.BOOLEAN,
            listOf("loc" to AshType.LOCATION, "adventuresUsed" to AshType.INT)) { _, args ->
            val locName = args[0].toString()
            val manager = adventureManager ?: return@register AshValue.of(false)
            val location = AdventureLocation(locName, locName, "")
            kotlinx.coroutines.runBlocking {
                manager.runAdventures(location, 1, this).join()
            }
            AshValue.of(true)
        }

        register(scope, "use_skill", AshType.BOOLEAN,
            listOf("turns" to AshType.INT, "sk" to AshType.SKILL)) { _, args ->
            val count = args[0].toLong().toInt()
            val skillName = args[1].toString()
            val manager = skillManager
                ?: throw ScriptException("Skill manager not available")
            val skill = manager.state.value.skills
                .find { it.name.equals(skillName, ignoreCase = true) }
                ?: throw ScriptException("Unknown skill: $skillName")
            kotlinx.coroutines.runBlocking {
                repeat(count) { manager.cast(skill, 1) }
            }
            AshValue.of(true)
        }

        register(scope, "use_skill", AshType.BOOLEAN,
            listOf("sk" to AshType.SKILL)) { _, args ->
            val skillName = args[0].toString()
            val manager = skillManager
                ?: throw ScriptException("Skill manager not available")
            val skill = manager.state.value.skills
                .find { it.name.equals(skillName, ignoreCase = true) }
                ?: throw ScriptException("Unknown skill: $skillName")
            kotlinx.coroutines.runBlocking { manager.cast(skill, 1) }
            AshValue.of(true)
        }

        register(scope, "cli_execute", AshType.BOOLEAN, listOf("cmd" to AshType.STRING)) { runtime, args ->
            dispatchCli(args[0].toString(), runtime)
            AshValue.of(true)
        }

    }

    // ──────────────────────────────────────────────────────────────
    // Banish queries
    // ──────────────────────────────────────────────────────────────

    private fun registerBanishQueries(scope: AshScope) {
        // to_monster(string) → monster
        register(scope, "to_monster", AshType.MONSTER, listOf("name" to AshType.STRING)) { _, args ->
            AshValue(AshType.MONSTER, args[0].toString())
        }

        // is_banished(monster) → boolean — accepts both monster type and string
        register(scope, "is_banished", AshType.BOOLEAN, listOf("monster" to AshType.MONSTER)) { _, args ->
            val name = args[0].toString()
            val currentTurn = character?.state?.value?.currentRun ?: 0
            AshValue.of(banishManager?.isBanished(name, currentTurn) ?: false)
        }
        register(scope, "is_banished", AshType.BOOLEAN, listOf("monster" to AshType.STRING)) { _, args ->
            val name = args[0].toString()
            val currentTurn = character?.state?.value?.currentRun ?: 0
            AshValue.of(banishManager?.isBanished(name, currentTurn) ?: false)
        }

        // banishers_used() → string[monster]
        val returnType = AggregateType(AshType.MONSTER, AshType.STRING)
        register(scope, "banishers_used", returnType, emptyList()) { _, _ ->
            val result = AggregateValue(returnType)
            val currentTurn = character?.state?.value?.currentRun ?: 0
            banishManager?.getActiveBanishes(currentTurn)
                ?.forEach { (monsterName, banisher) ->
                    result[AshValue(AshType.MONSTER, monsterName)] = AshValue.of(banisher.canonicalName)
                }
            result
        }
    }
}
