package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.CharpaneValhallaSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.data.AdventureQueueDatabase
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.DefaultsDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.IslandWarResetSync
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.track.TrackManager
import kotlin.math.min

/**
 * Desktop ValhallaManager headless port (Phases 3306–3320, residual 7451–7510).
 * Orchestrates pre/post ascension housekeeping without Swing UI or a full HTTP storm.
 */
object ValhallaManager {

    val USABLE_ITEM_IDS = intArrayOf(
        ItemPool.GATES_SCROLL,
        ItemPool.FISHERMANS_SACK,
        ItemPool.BONERDAGON_CHEST,
    )

    val FREEPULL_ITEM_IDS = intArrayOf(
        ItemPool.VIP_LOUNGE_KEY,
        ItemPool.CURSED_KEG,
        ItemPool.CURSED_MICROWAVE,
    )

    private val AUTOSELL_ITEM_NAMES = listOf(
        "small laminated card", "little laminated card", "notbig laminated card", "unlarge laminated card",
        "dwarvish document", "dwarvish paper", "dwarvish parchment", "cultist robe",
        "creased paper strip", "crinkled paper strip", "crumpled paper strip", "folded paper strip",
        "ragged paper strip", "ripped paper strip", "rumpled paper strip", "torn paper strip",
        "rave visor", "baggy rave pants", "pacifier necklace", "glowstick on a string",
        "candy necklace", "teddybear backpack",
        "vial of red slime", "vial of yellow slime", "vial of blue slime", "vial of orange slime",
        "vial of green slime", "vial of violet slime", "vial of vermilion slime", "vial of amber slime",
        "vial of chartreuse slime", "vial of teal slime", "vial of indigo slime", "vial of purple slime",
        "vial of brown slime", "fish oil smoke bomb", "vial of squid ink", "potion of fishy speed",
        "autopsy tweezers", "gnomish ear", "gnomish lung", "gnomish elbow", "gnomish knee", "gnomish foot",
    )

    data class AscensionDeps(
        val preferences: Preferences?,
        val character: KoLCharacter?,
        val inventoryCount: (Int) -> Int = { 0 },
        val useItem: (Int, Int) -> Unit = { _, _ -> },
        val autosell: (List<Pair<Int, Int>>) -> Unit = {},
        val harvestGarden: () -> Unit = {},
        val harvestMushrooms: () -> Unit = {},
        val executeScript: (String) -> Unit = {},
        val pullFromStorage: (Int, Int) -> Unit = { _, _ -> },
        val visitCafeMenu: (String) -> Unit = {},
        val resetCafeMenu: (String) -> Unit = {},
        val sessionLog: (String) -> Unit = {},
        val visitPyro: () -> Unit = {},
        val visitCouncil: () -> Unit = {},
        val visitPlace: (String) -> Unit = {},
        val updateInventory: () -> Unit = {},
        val visitLounge: () -> Unit = {},
        val visitLoungeFloor2: () -> Unit = {},
        val banishManager: BanishManager? = null,
        val questDatabase: QuestDatabase? = null,
        val adventureSpentReset: () -> Unit = {},
    )

    /** Desktop ValhallaManager.preAscension — quest item cleanup + user script. */
    fun preAscension(deps: AscensionDeps) {
        if (deps.inventoryCount(ItemPool.GUNPOWDER) > 0) {
            deps.visitPyro()
        }
        for (itemId in USABLE_ITEM_IDS) {
            val count = deps.inventoryCount(itemId)
            if (count > 0) deps.useItem(itemId, count)
        }
        val autosell = AUTOSELL_ITEM_NAMES.mapNotNull { name ->
            val itemId = ItemDatabase.getByName(name)?.id ?: return@mapNotNull null
            val count = deps.inventoryCount(itemId)
            if (count > 0) itemId to count else null
        }
        if (autosell.isNotEmpty()) deps.autosell(autosell)
        deps.harvestGarden()
        deps.harvestMushrooms()
        val leftArm = deps.inventoryCount(ItemPool.LEFT_BEAR_ARM)
        val rightArm = deps.inventoryCount(ItemPool.RIGHT_BEAR_ARM)
        val armBox = deps.inventoryCount(ItemPool.BOX_OF_BEAR_ARM)
        if (leftArm > 0 && rightArm > 0 && armBox <= 0) {
            deps.useItem(ItemPool.LEFT_BEAR_ARM, 1)
        }
        val script = deps.preferences?.getString("preAscensionScript", "").orEmpty()
        if (script.isNotBlank()) deps.executeScript(script)
    }

    /**
     * Desktop GenericRequest: `ascend.php` + `action=ascend` sets lastBreakfast=0
     * so the first afterlife visit runs [onAscension].
     */
    fun noteGashJump(preferences: Preferences?) {
        preferences?.setInt("lastBreakfast", 0)
    }

    /** Desktop ValhallaManager.onAscension — character/pref/counter reset. */
    fun onAscension(
        character: KoLCharacter?,
        preferences: Preferences?,
        banishManager: BanishManager? = null,
        questDatabase: QuestDatabase? = null,
        adventureSpentReset: () -> Unit = {},
    ) {
        character?.reset()
        preferences?.let { prefs ->
            prefs.setInt("knownAscensions", prefs.getInt("knownAscensions", 0) + 1)
            prefs.setInt("ascensionsToday", prefs.getInt("ascensionsToday", 0) + 1)
            prefs.setInt("lastBreakfast", -1)
            prefs.setInt("currentRun", 0)
            prefs.setInt("lastGuildStoreOpen", -1)
            resetPerAscensionCounters(
                prefs,
                banishManager,
                questDatabase,
                adventureSpentReset,
            )
            BadMoonManager.validateBadMoon(prefs, prefs.getInt("knownAscensions", 0))
        }
        CharpaneValhallaSync.reset()
    }

    /** Desktop ValhallaManager.postAscension — refresh + user script + free pulls. */
    fun postAscension(deps: AscensionDeps) {
        CharpaneValhallaSync.reset()
        resetMoonsignCafes(deps)
        ConcoctionDatabase.markRefreshNeeded()
        ConsumableDatabase.resetOverrides()
        deps.preferences?.apply {
            setString("mood", "apathetic")
            setFloat("hpAutoRecovery", -0.05f)
            setFloat("mpAutoRecovery", -0.05f)
        }
        logNewAscension(deps)
        startPathWindows(deps)
        val script = deps.preferences?.getString("postAscensionScript", "").orEmpty()
        if (script.isNotBlank()) deps.executeScript(script)
        pullFreeItems(deps)
        if (deps.preferences?.getBoolean("autoQuest", false) == true) {
            deps.useItem(ItemPool.SPOOKYRAVEN_TELEGRAM, 1)
        }
        deps.visitLounge()
        deps.visitLoungeFloor2()
    }

    /**
     * Desktop GenericRequest afterlife redirect: choice.php defers via
     * [ChoiceCombatAshState.ascendAfterChoice]; otherwise [postAscension] runs now.
     */
    fun handleAfterlifeRedirect(redirectLocation: String?, deps: AscensionDeps) {
        val redirect = redirectLocation.orEmpty()
        if (redirect.startsWith("choice.php") || redirect.contains("choice.php")) {
            ChoiceCombatAshState.ascendAfterChoice()
        } else {
            postAscension(deps)
        }
    }

    fun resetPerAscensionCounters(
        preferences: Preferences,
        banishManager: BanishManager? = null,
        questDatabase: QuestDatabase? = null,
        adventureSpentReset: () -> Unit = {},
    ) {
        DefaultsDatabase.resetOnAscensionPrefs(preferences)
        TrackManager.resetAscension(preferences)
        banishManager?.resetAscension()
        questDatabase?.resetQuests()
        IslandWarResetSync.resetIsland(preferences)
        BugbearManager.resetStatus(preferences)
        TurnCounter.clearCounters(preferences)
        AdventureQueueDatabase.resetQueue()
        adventureSpentReset()
    }

    private fun pullFreeItems(deps: AscensionDeps) {
        for (itemId in FREEPULL_ITEM_IDS) {
            if (deps.inventoryCount(itemId) > 0) continue
            deps.pullFromStorage(itemId, 1)
        }
    }

    private fun resetMoonsignCafes(deps: AscensionDeps) {
        val state = deps.character?.state?.value ?: return
        val inBadMoon = BadMoonManager.inBadMoon(state)
        if (inBadMoon) {
            deps.visitCafeMenu("hellkitchen")
        } else {
            deps.resetCafeMenu("hellkitchen")
        }
        if (!inBadMoon && state.ascensionPath.canEat && canadiaAvailable(state)) {
            deps.visitCafeMenu("chezsnootee")
        } else if (!state.ascensionPath.canEat || !canadiaAvailable(state)) {
            deps.resetCafeMenu("chezsnootee")
        }
        if (!inBadMoon && state.ascensionPath.canDrink && gnomadsAvailable(state)) {
            deps.visitCafeMenu("microbrewery")
        } else if (!state.ascensionPath.canDrink || !gnomadsAvailable(state)) {
            deps.resetCafeMenu("microbrewery")
        }
    }

    /** Desktop [KoLCharacter.canadiaAvailable] — Canadia moonsigns, not KoE. */
    internal fun canadiaAvailable(state: CharacterState): Boolean {
        if (state.isKingdomOfExploathing) return false
        val sign = ZodiacSign.find(state.zodiacSign) ?: return false
        return sign == ZodiacSign.PLATYPUS ||
            sign == ZodiacSign.OPOSSUM ||
            sign == ZodiacSign.MARMOT
    }

    /** Desktop [KoLCharacter.gnomadsAvailable] — Gnomads moonsigns, not KoE. */
    internal fun gnomadsAvailable(state: CharacterState): Boolean {
        if (state.isKingdomOfExploathing) return false
        val sign = ZodiacSign.find(state.zodiacSign) ?: return false
        return sign == ZodiacSign.WOMBAT ||
            sign == ZodiacSign.BLENDER ||
            sign == ZodiacSign.PACKRAT
    }

    private fun startPathWindows(deps: AscensionDeps) {
        val prefs = deps.preferences ?: return
        val state = deps.character?.state?.value ?: return
        val run = prefs.getInt("currentRun", 0)
        when (state.ascensionPath) {
            AscensionPath.HEAVY_RAINS -> {
                TurnCounter.startCounting(prefs, run, 8, "Rain Monster window begin loc=*", "lparen.gif")
                TurnCounter.startCounting(prefs, run, 10, "Rain Monster window end loc=*", "rparen.gif")
            }
            AscensionPath.AVATAR_OF_WEST_OF_LOATHING -> {
                TurnCounter.startCounting(prefs, run, 5, "WoL Monster window begin loc=*", "lparen.gif")
                TurnCounter.startCounting(prefs, run, 10, "WoL Monster window end loc=*", "rparen.gif")
            }
            AscensionPath.THE_SOURCE -> {
                prefs.setInt("sourceEnlightenment", min(prefs.getInt("sourcePoints", 0), 11))
            }
            AscensionPath.KINGDOM_OF_EXPLOATHING -> {
                deps.visitCouncil()
                deps.visitPlace("manor1")
                deps.updateInventory()
            }
            else -> Unit
        }
    }

    fun logNewAscension(deps: AscensionDeps) {
        val state = deps.character?.state?.value ?: return
        val prefs = deps.preferences
        val ascNum = prefs?.getInt("knownAscensions", state.ascensionNumber) ?: state.ascensionNumber
        val hardcore = state.isHardcore
        val path = state.ascensionPath.apiName
        val className = CharacterClass.fromId(state.characterClass).displayName
        val sign = ZodiacSign.find(state.zodiacSign)?.signName ?: state.zodiacSign
        val pathLabel = if (path == "None") "No-Path" else path
        deps.sessionLog("")
        deps.sessionLog("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
        deps.sessionLog("	   Beginning New Ascension	     ")
        deps.sessionLog("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
        deps.sessionLog("Ascension #$ascNum:")
        deps.sessionLog("${if (hardcore) "Hardcore" else "Softcore"} $pathLabel $className")
        deps.sessionLog(sign)
        deps.sessionLog("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
        deps.sessionLog("")
    }
}
