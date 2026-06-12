package net.sourceforge.kolmafia.ash

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.adventure.AdventureRequest
import net.sourceforge.kolmafia.adventure.ChoiceRequest
import net.sourceforge.kolmafia.data.AdventureDatabase
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
import net.sourceforge.kolmafia.familiar.FamiliarRequest
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.location.LocationDatabase
import net.sourceforge.kolmafia.mood.MoodManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.QuestChoiceRules
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.recovery.RecoveryManager
import net.sourceforge.kolmafia.request.AutosellRequest
import net.sourceforge.kolmafia.request.CharacterRequest
import net.sourceforge.kolmafia.request.ChewRequest
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.request.ClosetRequest
import net.sourceforge.kolmafia.request.DrinkBoozeRequest
import net.sourceforge.kolmafia.request.EatFoodRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.shop.CoinmasterManager
import net.sourceforge.kolmafia.request.ClanStashRequest
import net.sourceforge.kolmafia.item.RetrieveItemService
import net.sourceforge.kolmafia.mall.MallManager
import net.sourceforge.kolmafia.mall.MallPriceManager
import net.sourceforge.kolmafia.maximizer.MaximizerManager
import net.sourceforge.kolmafia.request.DisplayCaseRequest
import net.sourceforge.kolmafia.request.HermitRequest
import net.sourceforge.kolmafia.request.ManageStoreRequest
import net.sourceforge.kolmafia.quest.QuestLogSync
import net.sourceforge.kolmafia.request.QuestLogRequest
import net.sourceforge.kolmafia.request.SendGiftRequest
import net.sourceforge.kolmafia.request.SendMailRequest
import net.sourceforge.kolmafia.request.StorageRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.session.BreakfastManager
import net.sourceforge.kolmafia.session.GoalManager
import net.sourceforge.kolmafia.session.PastaThrall
import net.sourceforge.kolmafia.chat.ChatSender
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
    internal val characterRequest: CharacterRequest? = null,
    internal val recoveryManager: RecoveryManager? = null,
    internal val adventureRequest: AdventureRequest? = null,
    internal val uneffectRequest: net.sourceforge.kolmafia.request.UneffectRequest? = null,
    internal val questDatabase: QuestDatabase? = null,
    internal val questLogRequest: QuestLogRequest? = null,
    internal val clanLoungeRequest: ClanLoungeRequest? = null,
    internal val familiarRequest: FamiliarRequest? = null,
    internal val chatSender: ChatSender? = null,
    internal val maximizerManager: MaximizerManager? = null,
    internal val sessionLogger: net.sourceforge.kolmafia.session.SessionLogger? = null,
    internal val breakfastManager: BreakfastManager? = null,
    internal val sendMailRequest: SendMailRequest? = null,
    internal val sendGiftRequest: SendGiftRequest? = null,
    internal val choiceRequest: ChoiceRequest? = null,
) : RuntimeLibrary() {

    companion object {
        /** Used in tests where no game managers are needed. */
        fun forTesting() = GameRuntimeLibrary()

        const val VERSION = "1.0.0-mobile"
        const val REVISION = "phase35"
    }

    /** Captured stdout from the most recent [cli_execute] call. */
    internal val lastCliOutput = StringBuilder()

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

        // "cast|skill N skill-name" — cast a skill N times (count form: silent no-op if unknown)
        Regex("^(?:cast|skill)\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val count = m.groupValues[1].toIntOrNull() ?: 1
            val skillName = m.groupValues[2].trim()
            val skill = skillManager?.state?.value?.skills
                ?.find { it.name.equals(skillName, ignoreCase = true) }
            if (skill != null) {
                kotlinx.coroutines.runBlocking { skillManager!!.cast(skill, count) }
            }
            // skill not found → silent no-op (no echo for count form)
        },

        // "cast|skill skill-name" — cast a skill once (no count prefix; echo if unknown)
        Regex("^(?:cast|skill)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
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

        // acquire / find — aliases for retrieve
        Regex("^(?:acquire|find)\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val count = m.groupValues[1].toIntOrNull() ?: return@to
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
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

        // goal add id:N — before generic goal add (order matters in cliDispatch)
        Regex("^goal\\s+add\\s+id:(\\d+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.addItemGoal(m.groupValues[1].toIntOrNull() ?: return@to)
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

        Regex("^goal\\s+meat\\s+(\\d+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.setMeatGoal(m.groupValues[1].toIntOrNull() ?: return@to)
        },

        Regex("^goal\\s+level\\s+(\\d+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.setLevelGoal(m.groupValues[1].toIntOrNull() ?: return@to)
        },

        Regex("^goal\\s+choice\\s+(\\d+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.setChoiceGoal(m.groupValues[1].toIntOrNull() ?: return@to)
        },

        Regex("^goal\\s+substats$", RegexOption.IGNORE_CASE) to { _, _ ->
            goalManager?.setSubstatsGoal(true)
        },

        // set location zone — must precede generic set pref handler
        Regex("^set\\s+location\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val zoneName = m.groupValues[1].trim()
            val location = resolveLocation(zoneName) ?: return@to
            preferences?.setString(Preferences.LAST_LOCATION, location.name)
            kotlinx.coroutines.runBlocking {
                adventureRequest?.travel(location.id)
            }
        },

        // set pref value — bare preference alias
        Regex("^set\\s+(\\S+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            preferences?.setString(m.groupValues[1].trim(), m.groupValues[2])
        },

        // get pref value
        Regex("^get\\s+(\\S+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            rt.print(preferences?.getString(m.groupValues[1].trim(), "") ?: "")
        },

        // counter — print/set named pref, or list relay counters
        Regex("^counter\\s+relay$", RegexOption.IGNORE_CASE) to { _, rt ->
            val currentRun = character?.state?.value?.currentRun ?: 0
            val prefs = preferences ?: return@to
            val formatted = net.sourceforge.kolmafia.session.TurnCounter.formatRelayCounters(prefs, currentRun)
            if (formatted.isNotBlank()) rt.print(formatted)
        },
        Regex("^counters$", RegexOption.IGNORE_CASE) to { _, rt ->
            val prefs = preferences ?: return@to
            for (name in prefs.counterNames()) {
                rt.print("$name: ${prefs.getInt("counter_$name", 0)}")
            }
        },
        Regex("^counter\\s+(\\S+)(?:\\s+(\\d+))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            val name = m.groupValues[1].trim()
            val value = m.groupValues.getOrNull(2)?.trim()
            if (value.isNullOrBlank()) {
                rt.print(preferences?.getInt("counter_$name", 0).toString())
            } else {
                preferences?.setInt("counter_$name", value.toIntOrNull() ?: 0)
                preferences?.registerCounterName(name)
            }
        },

        Regex("^choice\\s+(\\d+)\\s+(\\d+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            cliChoice(m.groupValues[1].toIntOrNull() ?: return@to, m.groupValues[2].toIntOrNull() ?: return@to)
        },
        Regex("^choice\\s+(\\d+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val choiceId = preferences?.getInt(AdventureManager.LAST_CHOICE_ID, 0) ?: return@to
            if (choiceId <= 0) return@to
            cliChoice(choiceId, m.groupValues[1].toIntOrNull() ?: return@to)
        },

        Regex("^thralls$", RegexOption.IGNORE_CASE) to { _, rt ->
            val prefs = preferences ?: return@to
            val table = PastaThrall.formatTable(prefs)
            if (table.isNotBlank()) rt.print(table)
        },

        Regex("^cemet(?:ery|ary)$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("place.php?whichplace=cemetery", applyQuestHooks = true)
        },

        Regex("^(?:clear|cls)$", RegexOption.IGNORE_CASE) to { _, _ ->
            lastCliOutput.clear()
        },

        // ccs / ccprep — store combat macro script text
        Regex("^ccs\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            preferences?.setString("combatMacro", m.groupValues[1])
        },
        Regex("^ccprep$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print(preferences?.getString("combatMacro", "") ?: "")
        },

        // location shortcuts
        Regex("^spooky$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("adventure.php?snarfblat=61")
        },
        Regex("^cellar2?$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("cellar.php")
        },
        Regex("^tower$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("tower.php", applyQuestHooks = true)
        },
        Regex("^fern$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("tower.php", applyQuestHooks = true)
        },
        Regex("^guild$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("guild.php", applyQuestHooks = true)
        },
        Regex("^guild\\s+(paco|ocg|scg|challenge)$", RegexOption.IGNORE_CASE) to { m, _ ->
            visitKolPage("guild.php?place=${m.groupValues[1].lowercase()}", applyQuestHooks = true)
        },

        // macro — print stored combat macro
        Regex("^macro$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print(preferences?.getString("combatMacro", "") ?: "")
        },

        // jukebox — visit jukebox (campground)
        Regex("^jukebox$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("campground.php?action=jukebox")
        },

        Regex("^(?:adventure|adv)\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val turns = m.groupValues[1].toIntOrNull() ?: return@to
            val zoneName = m.groupValues[2].trim()
            val location = resolveLocation(zoneName) ?: return@to
            val manager = adventureManager ?: return@to
            kotlinx.coroutines.runBlocking {
                manager.runAdventures(location, turns, this).join()
            }
        },

        // location — print last known location
        Regex("^location$", RegexOption.IGNORE_CASE) to { _, rt ->
            val loc = preferences?.getString(Preferences.LAST_LOCATION, "") ?: ""
            rt.print(loc)
        },

        // zone — print adventures.txt zone name for current location
        Regex("^zone$", RegexOption.IGNORE_CASE) to { _, rt ->
            val loc = preferences?.getString(Preferences.LAST_LOCATION, "") ?: ""
            val zone = AdventureDatabase.getByName(loc)?.zoneName ?: loc
            rt.print(zone)
        },

        // count [N] item — print inventory quantity (N ignored; desktop compatibility)
        Regex("^count\\s+(?:(\\d+)\\s+)?(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val itemName = m.groupValues[2].trim()
            val qty = inventoryManager?.state?.value?.items?.values
                ?.find { it.name.equals(itemName, ignoreCase = true) }?.quantity ?: 0
            rt.print(qty.toString())
        },

        // put_storage N item — alias for storage put
        Regex("^put_storage\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { storageRequest?.deposit(itemId, qty) }
        },

        // refresh — sync character, inventory, skills, effects, familiars, quest log
        Regex("^refresh$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking {
                characterRequest?.fetchCharacterState()?.onSuccess { resp ->
                    character?.updateFromApiResponse(resp)
                }
                inventoryManager?.fetchInventory()
                skillManager?.fetchSkills()
                effectManager?.fetchEffects()
                familiarManager?.fetchFamiliars()
                questLogRequest?.syncAll()
            }
        },

        // questlog / quests — sync quest log pages
        Regex("^(?:questlog|quests|quest)$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking { questLogRequest?.syncAll() }
        },

        // quest NAME — print current step for one quest
        Regex("^quest\\s+(\\S+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val quest = resolveQuest(m.groupValues[1]) ?: run {
                rt.print(QuestDatabase.UNSTARTED)
                return@to
            }
            rt.print(questDatabase?.getProgress(quest) ?: QuestDatabase.UNSTARTED)
        },

        // whatis item — alias for description
        Regex("^whatis\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val item = gameDatabase?.item(m.groupValues[1].trim())
            if (item == null) {
                rt.print("Unknown item: ${m.groupValues[1].trim()}")
            } else {
                rt.print("${item.name} (${item.primaryUse.name.lowercase()}, autosell ${item.autosellPrice} meat)")
            }
        },

        // skills / effects / inv — refresh cached state
        Regex("^skills$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking { skillManager?.fetchSkills() }
        },
        Regex("^effects$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking { effectManager?.fetchEffects() }
        },
        Regex("^inv$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking {
                inventoryManager?.fetchInventory()
                inventoryManager?.syncCharacterEquipment()
            }
        },
        Regex("^inventory$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking {
                inventoryManager?.fetchInventory()
                inventoryManager?.syncCharacterEquipment()
            }
        },

        // contacts / mail — visit common KoL pages
        Regex("^contacts$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("contacts.php")
        },
        Regex("^(?:mail|readmail)$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("mail.php")
        },

        // description / desc / show item — print item summary from database
        Regex("^(?:description|desc|show)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val item = gameDatabase?.item(m.groupValues[1].trim())
            if (item != null) {
                rt.print("${item.name} [${item.primaryUse.name.lowercase()}] autosell=${item.autosellPrice}")
            }
        },

        // pool — play one VIP lounge pool game
        Regex("^pool$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking { clanLoungeRequest?.playPoolGame() }
        },

        // refreshshop / refresh shop — refresh mall store prices
        Regex("^refresh\\s*shop$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking { manageStoreRequest?.refreshPrices() }
        },

        // itemnotify on/off — pref toggle (headless stub)
        Regex("^itemnotify\\s+(on|off)$", RegexOption.IGNORE_CASE) to { m, _ ->
            preferences?.setBoolean("itemNotify", m.groupValues[1].equals("on", ignoreCase = true))
        },

        // vendor / managestore / mall — visit store pages
        Regex("^(?:vendor|managestore)$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("managestore.php")
        },
        Regex("^mall$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("mallstore.php")
        },

        // familiars — refresh familiar list
        Regex("^familiars$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking { familiarManager?.fetchFamiliars() }
        },

        // steal N item — familiar steal
        Regex("^steal\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: return@to
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            val req = familiarRequest ?: return@to
            kotlinx.coroutines.runBlocking {
                repeat(qty) {
                    if (req.stealItem(itemId).isFailure) return@runBlocking
                    inventoryManager?.fetchInventory()
                }
            }
        },

        // sendmsg channel message — public chat
        Regex("^sendmsg\\s+(\\S+)\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val channel = m.groupValues[1].trim()
            val message = m.groupValues[2]
            kotlinx.coroutines.runBlocking { chatSender?.send(channel, message) }
        },

        // msg player message — private chat
        Regex("^msg\\s+(\\S+)\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val recipient = m.groupValues[1].trim()
            val message = m.groupValues[2]
            kotlinx.coroutines.runBlocking { chatSender?.sendPrivate(recipient, message) }
        },

        // kmail recipient message — text-only kmail via sendmessage.php
        Regex("^kmail\\s+(\\S+)\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val recipient = m.groupValues[1].trim()
            val message = m.groupValues[2]
            kotlinx.coroutines.runBlocking { sendMailRequest?.send(recipient, message) }
        },

        // send item(s) to recipient [|| message]
        Regex("^send\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliSend(m.groupValues[1], isMeat = false, rt)
        },

        // csend meat to recipient [|| message]
        Regex("^csend\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliSend(m.groupValues[1], isMeat = true, rt)
        },

        // gift item(s) to recipient [|| message] — town_sendgift.php
        Regex("^gift\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            cliGift(m.groupValues[1], rt)
        },

        // note — print user note; note text — save user note
        Regex("^note$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print(preferences?.getString(Preferences.USER_NOTE, "") ?: "")
        },
        Regex("^note\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, _ ->
            preferences?.setString(Preferences.USER_NOTE, m.groupValues[1])
        },

        // absorb — refresh character state (Grey path sync)
        Regex("^absorb$", RegexOption.IGNORE_CASE) to { _, rt ->
            visitKolPage("charpane.php")
            kotlinx.coroutines.runBlocking {
                characterRequest?.fetchCharacterState()?.onSuccess { resp ->
                    character?.updateFromApiResponse(resp)
                }
            }
            rt.print((character?.state?.value?.absorbs ?: 0).toString())
        },

        // version / cli — print mobile version string
        Regex("^(?:version|cli)$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print(GameRuntimeLibrary.VERSION)
        },

        // charpane — visit character pane
        Regex("^charpane$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("charpane.php")
        },

        // run scriptname — execute saved ASH script from preferences
        Regex("^run(?:script)?\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val scriptName = m.groupValues[1].trim()
            if (!runSavedScript(scriptName, rt)) {
                rt.print("Script '$scriptName' not found")
            }
        },

        Regex("^maximizer$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print("Usage: maximize <goal>  (e.g. maximize mysticality)")
        },
        Regex("^maximize(?:\\s+(.+))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            val goal = m.groupValues.getOrNull(1)?.trim().orEmpty().ifBlank { "all" }
            val mgr = maximizerManager ?: return@to
            val result = kotlinx.coroutines.runBlocking { mgr.maximize(goal) }
            rt.print(if (result.success) "Maximized for $goal" else "No improvement for $goal")
        },

        // autoscript on/off — persist preference stub
        Regex("^autoscript\\s+(on|off)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val on = m.groupValues[1].equals("on", ignoreCase = true)
            preferences?.setBoolean(Preferences.AUTO_SCRIPTING, on)
            rt.print(if (on) "autoscript enabled" else "autoscript disabled")
        },

        // sync — alias for full refresh (character + quest log)
        Regex("^sync$", RegexOption.IGNORE_CASE) to { _, rt ->
            dispatchCli("refresh", rt)
        },

        // recover / rest / restore / check — force recovery loop once
        Regex("^(?:recover|rest|restore|check)$", RegexOption.IGNORE_CASE) to { _, _ ->
            val rm = recoveryManager ?: return@to
            val char = character ?: return@to
            kotlinx.coroutines.runBlocking {
                rm.recoverIfNeeded(
                    charState  = char.state.value,
                    invState   = inventoryManager?.state?.value ?: InventoryState(),
                    skillState = skillManager?.state?.value ?: SkillState(),
                    force      = true,
                )
                characterRequest?.fetchCharacterState()?.onSuccess { char.updateFromApiResponse(it) }
            }
        },

        // storage put N item
        Regex("^storage\\s+put\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { storageRequest?.deposit(itemId, qty) }
        },

        // takeshop N item
        Regex("^takeshop\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { manageStoreRequest?.removeItem(itemId, qty) }
        },

        // empty closet
        Regex("^empty\\s+closet$", RegexOption.IGNORE_CASE) to { _, _ ->
            kotlinx.coroutines.runBlocking { closetRequest?.emptyCloset() }
        },

        // overdrink N item
        Regex("^overdrink\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { drinkBoozeRequest?.drink(itemId, qty) }
        },

        // echo / print — output text to CLI stream
        Regex("^(?:echo|print)\\s+(.*)$", RegexOption.IGNORE_CASE) to { m, rt ->
            rt.print(m.groupValues[1])
        },

        // status — one-line character summary
        Regex("^status$", RegexOption.IGNORE_CASE) to { _, rt ->
            val cs = character?.state?.value
            if (cs != null) {
                rt.print(
                    "${cs.name} (Level ${cs.level}); ${cs.adventuresLeft} adventures; " +
                        "${cs.meat} meat; ${cs.currentHp}/${cs.maxHp} HP; ${cs.currentMp}/${cs.maxMp} MP"
                )
            }
        },

        // stop / abort / pause — cancel running adventure loop
        Regex("^(?:stop|abort|pause)$", RegexOption.IGNORE_CASE) to { _, _ ->
            adventureManager?.stop()
        },

        // main / council / campground / homepage — visit common KoL pages
        Regex("^main$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("main.php")
        },
        Regex("^homepage$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("main.php")
        },
        Regex("^council$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("council.php", applyQuestHooks = true)
        },
        Regex("^campground$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("campground.php")
        },
        Regex("^camp$", RegexOption.IGNORE_CASE) to { _, _ ->
            visitKolPage("campground.php")
        },

        // breakfast — run daily breakfast sequence
        Regex("^breakfast$", RegexOption.IGNORE_CASE) to { _, _ ->
            val mgr = breakfastManager ?: return@to
            val char = character ?: return@to
            val inv = inventoryManager ?: return@to
            kotlinx.coroutines.runBlocking {
                mgr.runBreakfast(char.state.value, inv.state.value)
            }
        },

        // wiki / javadoc item — print Kol Wiki URL (headless has no browser)
        Regex("^wiki\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            rt.print(wikiUrlFor(m.groupValues[1].trim()))
        },
        Regex("^javadoc\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            rt.print(wikiUrlFor(m.groupValues[1].trim()))
        },

        // turns / turnsleft — print adventures remaining
        Regex("^(?:turns|turnsleft)$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print((character?.state?.value?.adventuresLeft ?: 0).toString())
        },

        // relay on/off — headless stub; scripts check pref only
        Regex("^relay(?:\\s+(on|off|open|close))?$", RegexOption.IGNORE_CASE) to { m, rt ->
            when (m.groupValues.getOrNull(1)?.lowercase()) {
                "on", "open" -> preferences?.setBoolean("relayActive", true)
                "off", "close" -> preferences?.setBoolean("relayActive", false)
                else -> rt.print("Relay is not available in KoLmafia Mobile.")
            }
        },

        // hermit N item — trade with the hermit
        Regex("^hermit\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: return@to
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { hermitRequest?.trade(itemId, qty) }
        },

        // config get/set — aliases for get/set prefs
        Regex("^config\\s+get\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val value = preferences?.getString(m.groupValues[1].trim(), "") ?: ""
            rt.print(value)
        },
        Regex("^config\\s+set\\s+(\\S+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            preferences?.setString(m.groupValues[1].trim(), m.groupValues[2])
        },

        // put_closet N item — alias for closet put
        Regex("^put_closet\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { closetRequest?.putIn(itemId, qty) }
        },

        // take_closet N item — alias for closet take
        Regex("^take_closet\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { closetRequest?.takeOut(itemId, qty) }
        },

        // take_storage N item — alias for storage take
        Regex("^take_storage\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { storageRequest?.withdraw(itemId, qty) }
        },

        // pull / hagnk — aliases for storage take
        Regex("^(?:pull|hagnk)\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { storageRequest?.withdraw(itemId, qty) }
        },

        // searchmall [with limit N] item — print cheapest mall price
        Regex("^searchmall(?:\\s+with\\s+limit\\s+(\\d+))?\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, rt ->
            val itemName = m.groupValues[2].trim()
            val price = kotlinx.coroutines.runBlocking { mallManager?.cheapestPrice(itemName) ?: -1L }
            rt.print(price.toString())
        },

        // reprice N item[@limit] — mall store reprice
        Regex("^reprice\\s+(\\d+)\\s+(.+?)(?:@(\\d+))?$", RegexOption.IGNORE_CASE) to { m, _ ->
            val price = m.groupValues[1].toIntOrNull() ?: return@to
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            val limit = m.groupValues[3].toIntOrNull() ?: 0
            kotlinx.coroutines.runBlocking { manageStoreRequest?.repriceItem(itemId, price, limit) }
        },

        // checkpoint / checkpoint clear — save or clear outfit checkpoint
        Regex("^checkpoint(?:\\s+clear)?$", RegexOption.IGNORE_CASE) to { m, _ ->
            if (m.value.endsWith("clear", ignoreCase = true)) {
                OutfitCheckpoint.clearSaved()
                return@to
            }
            val char = character ?: return@to
            val equip = equipmentRequest ?: return@to
            val db = gameDatabase ?: return@to
            kotlinx.coroutines.runBlocking {
                OutfitCheckpoint.snapshot(char, equip, db)
            }
        },

        // uneffect name / uneffect all
        Regex("^uneffect\\s+all$", RegexOption.IGNORE_CASE) to { _, _ ->
            uneffectAll()
        },
        Regex("^uneffect\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            uneffectByName(m.groupValues[1].trim())
        },

        // shrug / remedy — aliases for uneffect
        Regex("^(?:shrug|remedy)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            uneffectByName(m.groupValues[1].trim())
        },

        // dump / dump off — compact state summary
        Regex("^dump(?:\\s+off)?$", RegexOption.IGNORE_CASE) to { _, rt ->
            rt.print(buildDumpSummary())
        },

        // batch open / batch close — pref counter only
        Regex("^batch\\s+open$", RegexOption.IGNORE_CASE) to { _, _ ->
            val prefs = preferences ?: return@to
            prefs.setInt("batching", (prefs.getInt("batching", 0) + 1).coerceAtLeast(1))
        },
        Regex("^batch\\s+close$", RegexOption.IGNORE_CASE) to { _, _ ->
            val prefs = preferences ?: return@to
            val current = prefs.getInt("batching", 0)
            if (current > 0) prefs.setInt("batching", current - 1)
        },

        // reagent N item
        Regex("^reagent\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { useItemRequest?.use(itemId, qty) }
        },

        // goal factoid text — stop when response contains text
        Regex("^goal\\s+factoid\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.setFactoidGoal(m.groupValues[1].trim())
        },

        // goal autostop text — desktop alias for factoid goal
        Regex("^goal\\s+autostop\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            goalManager?.setFactoidGoal(m.groupValues[1].trim())
        },

        // putshop price[@limit] N item — list item in mall store
        Regex("^putshop\\s+(\\d+)(?:@(\\d+))?\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val price = m.groupValues[1].toIntOrNull() ?: return@to
            val limit = m.groupValues[2].toIntOrNull() ?: 0
            val qty = m.groupValues[3].toIntOrNull() ?: 1
            val itemId = gameDatabase?.item(m.groupValues[4].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking {
                manageStoreRequest?.addItem(itemId, price, limit, qty)
            }
        },

        // wear / wield — aliases for equip
        Regex("^(?:wear|wield)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            cliEquip(m.groupValues[1].trim())
        },

        // "equip [<slot>] <item-name>" — equip item, optionally into a named slot.
        Regex("^equip\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            cliEquip(m.groupValues[1].trim())
        },

        // remove slot — unequip (after goal remove)
        Regex("^remove\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val slotName = m.groupValues[1].trim()
            kotlinx.coroutines.runBlocking { inventoryManager?.unequipSlot(slotName) }
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

        // "coinmaster buy N <nick> <item>" — quantity buy
        Regex("^coinmaster\\s+buy\\s+(\\d+)\\s+(\\S+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val qty = m.groupValues[1].toIntOrNull() ?: return@to
            val nickname = m.groupValues[2].trim()
            val itemName = m.groupValues[3].trim()
            val master = coinmasterManager?.resolveMaster(nickname) ?: return@to
            val itemId = gameDatabase?.item(itemName)?.id ?: return@to
            kotlinx.coroutines.runBlocking { coinmasterManager?.buy(master, itemId, qty) }
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

        // make / bake / mix / smith / tinker / ply — aliases for create
        Regex("^(?:make|bake|mix|smith|tinker|ply)\\s+(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE) to { m, _ ->
            val count = m.groupValues[1].toIntOrNull() ?: return@to
            val itemId = gameDatabase?.item(m.groupValues[2].trim())?.id ?: return@to
            kotlinx.coroutines.runBlocking { retrieveItemService?.retrieve(itemId, count) }
        },

        // "buy|mallbuy N item[@limit]" — checkpoint-wrapped mall purchase
        Regex("^(?:buy|mallbuy)\\s+(\\d+)\\s+(.+?)(?:@(\\d+))?$", RegexOption.IGNORE_CASE) to { m, _ ->
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

    internal fun resolveLocation(name: String): AdventureLocation? {
        LocationDatabase.ALL_LOCATIONS.find { it.name.equals(name, ignoreCase = true) }?.let {
            return AdventureLocation(it.snarfblat, it.name, it.zone)
        }
        LocationDatabase.findBySnarfblat(name)?.let {
            return AdventureLocation(it.snarfblat, it.name, it.zone)
        }
        if (name.all { it.isDigit() }) {
            return AdventureLocation(name, name, "")
        }
        return null
    }

    internal fun wikiUrlFor(name: String): String {
        val slug = name.trim().replace(' ', '_')
        return "https://wiki.a.kolmafia.us/wiki/$slug"
    }

    internal fun processVisitQuestHooks(html: String, url: String? = null) {
        val db = questDatabase ?: return
        kotlinx.coroutines.runBlocking {
            QuestLogSync.processResponse(html, db, questLogRequest, buildQuestSyncContext(url))
        }
    }

    internal fun buildQuestSyncContext(urlOrPath: String? = null): QuestLogSync.QuestSyncContext =
        QuestLogSync.QuestSyncContext(
            hasItemId = { id -> inventoryManager?.state?.value?.items?.containsKey(id) == true },
            place = urlOrPath?.let { extractQuestPlace(it) },
            preferences = preferences,
            currentRun = character?.state?.value?.currentRun ?: 0,
        )

    internal fun extractQuestPlace(urlOrPath: String): String? =
        extractGuildPlace(urlOrPath)
            ?: when {
                urlOrPath.contains("tower.php", ignoreCase = true) -> "fern"
                urlOrPath.contains("fernruin", ignoreCase = true) -> "fernruin"
                urlOrPath.contains("whichplace=cemetery", ignoreCase = true) -> "cemetery"
                else -> null
            }

    internal fun extractGuildPlace(urlOrPath: String): String? =
        Regex("(?:^|[?&])place=([a-z]+)", RegexOption.IGNORE_CASE)
            .find(urlOrPath)
            ?.groupValues
            ?.get(1)
            ?.lowercase()

    internal fun cliSend(parameters: String, isMeat: Boolean, rt: AshRuntimeContext) {
        val normalized = parameters.replace(Regex("(?i)(?:^| )to "), " => ")
        val parts = normalized.split(" => ", limit = 2)
        if (parts.size != 2) {
            rt.print("Invalid send request.")
            return
        }
        var recipientPart = parts[1].trim()
        var message = net.sourceforge.kolmafia.request.SendMailRequest.DEFAULT_MESSAGE
        val sep = recipientPart.indexOf("||")
        if (sep >= 0) {
            message = recipientPart.substring(sep + 2).trim()
            recipientPart = recipientPart.substring(0, sep).trim()
        }
        val recipient = recipientPart
        val itemPart = parts[0].trim().trimEnd(',')
        if (itemPart.isBlank()) {
            kotlinx.coroutines.runBlocking { sendMailRequest?.send(recipient, message) }
            return
        }
        var meat = 0L
        val attachments = mutableListOf<net.sourceforge.kolmafia.request.MailAttachment>()
        val itemSpecs = if (itemPart.contains(',')) {
            itemPart.split(',').map { it.trim() }.filter { it.isNotBlank() }
        } else {
            listOf(itemPart)
        }
        for (spec in itemSpecs) {
            val match = Regex("^(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE).find(spec) ?: continue
            val qty = match.groupValues[1].toLongOrNull() ?: continue
            val name = match.groupValues[2].trim()
            if (name.equals("meat", ignoreCase = true)) {
                if (!isMeat) {
                    rt.print("Please use 'csend' if you need to transfer meat.")
                    return
                }
                meat += qty
                continue
            }
            if (isMeat) continue
            val itemId = gameDatabase?.item(name)?.id
            if (itemId == null) {
                rt.print("Unknown item: $name")
                return
            }
            val available = inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
            if (available < qty) {
                rt.print("[$qty $name] requested, but only $available available.")
                return
            }
            attachments.add(net.sourceforge.kolmafia.request.MailAttachment(itemId, qty.toInt()))
        }
        if (attachments.size > net.sourceforge.kolmafia.request.SendMailRequest.MAX_ATTACHMENTS) {
            rt.print("Too many attachments.")
            return
        }
        kotlinx.coroutines.runBlocking {
            val mailResult = sendMailRequest?.send(recipient, message, attachments, meat)
            if (mailResult?.isFailure == true && attachments.isNotEmpty() && meat == 0L) {
                sendGiftRequest?.send(recipient, message, attachments)
                    ?: mailResult
            } else {
                mailResult
            }
        }
    }

    internal fun cliGift(parameters: String, rt: AshRuntimeContext) {
        val normalized = parameters.replace(Regex("(?i)(?:^| )to "), " => ")
        val parts = normalized.split(" => ", limit = 2)
        if (parts.size != 2) {
            rt.print("Invalid gift request.")
            return
        }
        var recipientPart = parts[1].trim()
        var message = SendMailRequest.DEFAULT_MESSAGE
        val sep = recipientPart.indexOf("||")
        if (sep >= 0) {
            message = recipientPart.substring(sep + 2).trim()
            recipientPart = recipientPart.substring(0, sep).trim()
        }
        val recipient = recipientPart
        val itemPart = parts[0].trim().trimEnd(',')
        if (itemPart.isBlank()) {
            rt.print("Invalid gift request.")
            return
        }
        val attachments = parseSendAttachments(itemPart, rt) ?: return
        if (attachments.isEmpty()) {
            rt.print("Invalid gift request.")
            return
        }
        kotlinx.coroutines.runBlocking {
            sendGiftRequest?.send(recipient, message, attachments)
        }
    }

    private fun parseSendAttachments(
        itemPart: String,
        rt: AshRuntimeContext,
    ): List<net.sourceforge.kolmafia.request.MailAttachment>? {
        val attachments = mutableListOf<net.sourceforge.kolmafia.request.MailAttachment>()
        val itemSpecs = if (itemPart.contains(',')) {
            itemPart.split(',').map { it.trim() }.filter { it.isNotBlank() }
        } else {
            listOf(itemPart)
        }
        for (spec in itemSpecs) {
            val match = Regex("^(\\d+)\\s+(.+)$", RegexOption.IGNORE_CASE).find(spec) ?: continue
            val qty = match.groupValues[1].toLongOrNull() ?: continue
            val name = match.groupValues[2].trim()
            if (name.equals("meat", ignoreCase = true)) {
                rt.print("Please use 'csend' if you need to transfer meat.")
                return null
            }
            val itemId = gameDatabase?.item(name)?.id
            if (itemId == null) {
                rt.print("Unknown item: $name")
                return null
            }
            val available = inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
            if (available < qty) {
                rt.print("[$qty $name] requested, but only $available available.")
                return null
            }
            attachments.add(net.sourceforge.kolmafia.request.MailAttachment(itemId, qty.toInt()))
        }
        if (attachments.size > SendMailRequest.MAX_ATTACHMENTS) {
            rt.print("Too many attachments.")
            return null
        }
        return attachments
    }

    internal fun cliChoice(choiceId: Int, option: Int) {
        val req = choiceRequest ?: return
        val db = questDatabase ?: return
        kotlinx.coroutines.runBlocking {
            req.choose(choiceId, option).onSuccess { html ->
                QuestLogSync.processResponse(html, db, questLogRequest, buildQuestSyncContext())
                QuestChoiceRules.apply(choiceId, html, db)
            }
        }
    }

    internal fun cliEquip(rest: String) {
        val spaceIdx = rest.indexOf(' ')
        if (spaceIdx > 0) {
            val firstToken = rest.substring(0, spaceIdx)
            val afterFirst = rest.substring(spaceIdx + 1).trim()
            val knownSlot = EquipmentSlot.entries.find { s ->
                s.apiKey.equals(firstToken, ignoreCase = true)
            } ?: if (firstToken.equals("familiar", ignoreCase = true)) {
                EquipmentSlot.FAMILIAR
            } else {
                null
            }
            if (knownSlot != null) {
                val item = inventoryManager?.state?.value?.items?.values
                    ?.find { it.name.equals(afterFirst, ignoreCase = true) }
                if (item != null) {
                    kotlinx.coroutines.runBlocking { inventoryManager?.equipItem(item, knownSlot.apiKey) }
                    return
                }
            }
        }
        val item = inventoryManager?.state?.value?.items?.values
            ?.find { it.name.equals(rest, ignoreCase = true) }
        if (item != null) {
            kotlinx.coroutines.runBlocking { inventoryManager?.equipItem(item, "default") }
        }
    }

    private fun visitKolPage(path: String, applyQuestHooks: Boolean = false) {
        val client = httpClient ?: return
        val db = questDatabase
        kotlinx.coroutines.runBlocking {
            try {
                val response = client.get("$KOL_BASE_URL/$path")
                if (!response.status.isSuccess()) return@runBlocking
                val html = response.bodyAsText()
                if (applyQuestHooks && db != null) {
                    QuestLogSync.processResponse(html, db, questLogRequest, buildQuestSyncContext(path))
                }
            } catch (_: Exception) {
                // best-effort page visit
            }
        }
    }

    internal fun uneffectByName(name: String) {
        val req = uneffectRequest ?: return
        val effect = effectManager?.state?.value?.effects
            ?.find { it.name.equals(name, ignoreCase = true) } ?: return
        kotlinx.coroutines.runBlocking { req.uneffect(effect.id) }
    }

    internal fun uneffectAll() {
        val req = uneffectRequest ?: return
        val effects = effectManager?.state?.value?.effects ?: return
        kotlinx.coroutines.runBlocking {
            for (effect in effects) {
                req.uneffect(effect.id).onFailure { /* best-effort */ }
            }
        }
    }

    internal fun buildDumpSummary(): String {
        val cs = character?.state?.value
        val loc = preferences?.getString(Preferences.LAST_LOCATION, "") ?: ""
        val goals = goalManager?.allGoalsAsStrings()?.joinToString(", ") ?: ""
        return buildString {
            if (cs != null) {
                append("${cs.name} L${cs.level}; ${cs.adventuresLeft} adv; ${cs.meat} meat")
            }
            if (loc.isNotBlank()) append("; loc=$loc")
            if (goals.isNotBlank()) append("; goals=[$goals]")
        }
    }

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
        registerCliOutput(scope)
        registerEnvironmentQueries(scope)
        registerUneffectActions(scope)
        registerQuestQueries(scope)
        registerChatQueries(scope)
        registerScriptFunctions(scope)
        registerSessionLog(scope)
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
            val location = resolveLocation(locName)
                ?: throw ScriptException("Unknown location: $locName")
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
            val location = resolveLocation(locName) ?: return@register AshValue.of(false)
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
            lastCliOutput.clear()
            val capturing = CliCapturingContext(runtime, lastCliOutput)
            dispatchCli(args[0].toString(), capturing)
            AshValue.of(true)
        }

    }

    /** Wraps an [AshRuntimeContext] to mirror [print] into [lastCliOutput]. */
    private class CliCapturingContext(
        private val delegate: AshRuntimeContext,
        private val buffer: StringBuilder,
    ) : AshRuntimeContext {
        override fun print(msg: String) {
            buffer.append(msg).append('\n')
            delegate.print(msg)
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
