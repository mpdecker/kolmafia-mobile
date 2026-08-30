package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.banish.BanishManager
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterClass
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.CharpaneValhallaSync
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.character.ZodiacSign
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.ConsumableDatabase
import net.sourceforge.kolmafia.data.DefaultsDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.track.TrackManager

/**
 * Desktop ValhallaManager headless port (Phases 3306–3320).
 * Orchestrates pre/post ascension housekeeping without Swing UI.
 */
object ValhallaManager {

    private val USABLE_ITEM_NAMES = listOf(
        "gates scroll", "fisherman's sack", "boneragon chest",
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

    private val FREEPULL_ITEM_NAMES = listOf("VIP key", "cursed keg", "cursed microwave")

    data class AscensionDeps(
        val preferences: Preferences?,
        val character: KoLCharacter?,
        val inventoryCount: (Int) -> Int = { 0 },
        val useItem: suspend (Int, Int) -> Unit = { _, _ -> },
        val autosell: suspend (List<Pair<Int, Int>>) -> Unit = {},
        val harvestGarden: suspend () -> Unit = {},
        val harvestMushrooms: suspend () -> Unit = {},
        val executeScript: suspend (String) -> Unit = {},
        val pullFromStorage: suspend (Int, Int) -> Unit = { _, _ -> },
        val visitCafeMenu: suspend (String) -> Unit = {},
        val resetCafeMenu: suspend (String) -> Unit = {},
        val sessionLog: (String) -> Unit = {},
    )

    /** Desktop ValhallaManager.preAscension — quest item cleanup + user script. */
    suspend fun preAscension(deps: AscensionDeps) {
        for (name in USABLE_ITEM_NAMES) {
            val itemId = ItemDatabase.getByName(name)?.id ?: continue
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
        val leftArm = deps.inventoryCount(118)
        val rightArm = deps.inventoryCount(119)
        val armBox = deps.inventoryCount(120)
        if (leftArm > 0 && rightArm > 0 && armBox <= 0) {
            deps.useItem(118, 1)
        }
        val script = deps.preferences?.getString("preAscensionScript", "").orEmpty()
        if (script.isNotBlank()) deps.executeScript(script)
    }

    /** Desktop ValhallaManager.onAscension — character/pref/counter reset. */
    fun onAscension(
        character: KoLCharacter?,
        preferences: Preferences?,
        banishManager: BanishManager? = null,
    ) {
        character?.reset()
        preferences?.let { prefs ->
            prefs.setInt("knownAscensions", prefs.getInt("knownAscensions", 0) + 1)
            prefs.setInt("ascensionsToday", prefs.getInt("ascensionsToday", 0) + 1)
            prefs.setInt("lastBreakfast", -1)
            prefs.setInt("currentRun", 0)
            resetPerAscensionCounters(prefs, banishManager)
            BadMoonManager.validateBadMoon(prefs, prefs.getInt("knownAscensions", 0))
        }
        CharpaneValhallaSync.reset()
    }

    /** Desktop ValhallaManager.postAscension — refresh + user script + free pulls. */
    suspend fun postAscension(deps: AscensionDeps) {
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
        val script = deps.preferences?.getString("postAscensionScript", "").orEmpty()
        if (script.isNotBlank()) deps.executeScript(script)
        pullFreeItems(deps)
    }

    fun resetPerAscensionCounters(preferences: Preferences, banishManager: BanishManager? = null) {
        DefaultsDatabase.resetOnAscensionPrefs(preferences)
        TrackManager.resetAscension(preferences)
        banishManager?.resetRollover()
    }

    private suspend fun pullFreeItems(deps: AscensionDeps) {
        for (name in FREEPULL_ITEM_NAMES) {
            val itemId = ItemDatabase.getByName(name)?.id ?: continue
            if (deps.inventoryCount(itemId) > 0) continue
            deps.pullFromStorage(itemId, 1)
        }
    }

    private suspend fun resetMoonsignCafes(deps: AscensionDeps) {
        val state = deps.character?.state?.value ?: return
        val inBadMoon = BadMoonManager.inBadMoon(state)
        if (inBadMoon) {
            deps.visitCafeMenu("hellkitchen")
        } else {
            deps.resetCafeMenu("hellkitchen")
        }
        if (!inBadMoon && state.canEat && canadiaAvailable(state)) {
            deps.visitCafeMenu("chezsnootee")
        } else if (!state.canEat || !canadiaAvailable(state)) {
            deps.resetCafeMenu("chezsnootee")
        }
        if (!inBadMoon && state.canDrink && gnomadsAvailable(state)) {
            deps.visitCafeMenu("microbrewery")
        } else if (!state.canDrink || !gnomadsAvailable(state)) {
            deps.resetCafeMenu("microbrewery")
        }
    }

    private fun canadiaAvailable(state: CharacterState): Boolean =
        state.ascensionPath != AscensionPath.BEES_HATE_YOU

    private fun gnomadsAvailable(state: CharacterState): Boolean =
        state.level >= 12

    fun logNewAscension(deps: AscensionDeps) {
        val state = deps.character?.state?.value ?: return
        val prefs = deps.preferences
        val ascNum = prefs?.getInt("knownAscensions", state.ascensionNumber) ?: state.ascensionNumber
        val hardcore = state.isHardcore
        val path = state.ascensionPath.apiName
        val className = CharacterClass.fromId(state.characterClass).displayName
        val sign = ZodiacSign.find(state.zodiacSign)?.name ?: state.zodiacSign
        deps.sessionLog("")
        deps.sessionLog("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
        deps.sessionLog("	   Beginning New Ascension	     ")
        deps.sessionLog("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
        deps.sessionLog("Ascension #$ascNum:")
        deps.sessionLog("${if (hardcore) "Hardcore" else "Softcore"} ${if (path == "None") "No-Path" else path} $className")
        deps.sessionLog(sign)
        deps.sessionLog("=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=")
        deps.sessionLog("")
    }
}
