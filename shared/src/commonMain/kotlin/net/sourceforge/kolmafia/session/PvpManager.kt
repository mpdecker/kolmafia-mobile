package net.sourceforge.kolmafia.session

import io.ktor.client.HttpClient
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.PeeVPeeRequest
import net.sourceforge.kolmafia.request.ProfileRequest

/**
 * Desktop [net.sourceforge.kolmafia.session.PvpManager] stance cache + random/directed fight loops.
 */
object PvpManager {
    private val optionToStanceInternal = linkedMapOf<Int, String>()
    private val stanceToOptionInternal = linkedMapOf<String, Int>()
    private val canonicalStanceToOptionInternal = linkedMapOf<String, Int>()
    private var canonicalStances: Array<String> = emptyArray()

    var stancesKnown: Boolean = false
        private set

    var noFight: Boolean = false

    var abortReason: String? = null

    val optionToStance: Map<Int, String>
        get() = optionToStanceInternal

    val stanceToOption: Map<String, Int>
        get() = stanceToOptionInternal

    private val stanceDropdownPattern =
        Regex("""<select name="stance">.*?</select>""", RegexOption.DOT_MATCHES_ALL)
    private val stanceOptionPattern =
        Regex("""<option value="(\d*)" (?:selected)?>(.*?)</option>""")

    fun parseStances(responseText: String) {
        val dropdown = stanceDropdownPattern.find(responseText) ?: return
        optionToStanceInternal.clear()
        stanceToOptionInternal.clear()
        canonicalStanceToOptionInternal.clear()
        val canonical = mutableListOf<String>()
        for (match in stanceOptionPattern.findAll(dropdown.value)) {
            val option = match.groupValues[1].toIntOrNull() ?: 0
            val stance = match.groupValues[2]
            optionToStanceInternal[option] = stance
            stanceToOptionInternal[stance] = option
            val canonicalStance = canonicalName(stance)
            canonicalStanceToOptionInternal[canonicalStance] = option
            canonical.add(canonicalStance)
        }
        canonical.sort()
        canonicalStances = canonical.toTypedArray()
        stancesKnown = optionToStanceInternal.isNotEmpty()
    }

    fun findStance(stanceName: String): Int {
        val matches = matchingCanonicalNames(stanceName)
        if (matches.size != 1) return -1
        return canonicalStanceToOptionInternal[matches[0]] ?: -1
    }

    fun findStance(stance: Int): String? = optionToStanceInternal[stance]

    suspend fun checkStances(
        client: HttpClient?,
        character: KoLCharacter?,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
        inventoryManager: InventoryManager? = null,
    ): Boolean {
        if (!stancesKnown) {
            if (client == null) return false
            PeeVPeeRequest.visitFight(client, character, preferences, sessionLogger, inventoryManager)
        }
        return stancesKnown
    }

    suspend fun checkHippyStone(
        client: HttpClient?,
        character: KoLCharacter?,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
        inventoryManager: InventoryManager? = null,
    ): Boolean {
        if (character?.state?.value?.hippyStoneBroken == true) return true
        if (client == null) return false
        PeeVPeeRequest.smashStone(client, character, preferences, sessionLogger, inventoryManager)
        return character?.state?.value?.hippyStoneBroken == true
    }

    suspend fun executePvpRequest(
        attacks: Int,
        mission: String,
        stance: Int,
        tougher: Boolean,
        client: HttpClient?,
        character: KoLCharacter?,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
        cliExecutor: ((String) -> Unit)? = null,
        print: (String) -> Unit = {},
        inventoryManager: InventoryManager? = null,
    ) {
        abortReason = null
        noFight = false
        if (client == null || character == null) {
            print("HTTP client is not available.")
            return
        }
        if (!checkHippyStone(client, character, preferences, sessionLogger, inventoryManager)) {
            print("This feature is not available to hippies.")
            return
        }

        val availableFights = character.state.value.pvpFightsLeft
        val totalFights = if (attacks <= 0 || attacks > availableFights) availableFights else attacks
        var fightsCompleted = 0
        while (fightsCompleted < totalFights) {
            if (abortReason != null) break
            if (!runBeforePvpScript(preferences, cliExecutor)) break
            print("Attack ${fightsCompleted + 1} of $totalFights")
            val result = PeeVPeeRequest.fight(
                client = client,
                opponent = "",
                stance = stance,
                mission = mission,
                tougher = tougher,
                character = character,
                preferences = preferences,
                sessionLogger = sessionLogger,
                inventoryManager = inventoryManager,
                print = print,
            )
            if (result.isFailure) {
                abortReason = result.exceptionOrNull()?.message ?: "PvP fight failed"
                break
            }
            if (abortReason != null) break
            if (noFight) {
                noFight = false
            } else {
                fightsCompleted++
            }
            if (character.state.value.pvpFightsLeft <= 0) break
        }
        abortReason?.let { print(it) }
        print("You have ${character.state.value.pvpFightsLeft} attacks remaining.")
    }

    suspend fun executeDirectedPvpRequest(
        targets: List<ProfileRequest>,
        mission: String,
        stance: Int,
        client: HttpClient?,
        character: KoLCharacter?,
        preferences: Preferences? = null,
        sessionLogger: SessionLogger? = null,
        cliExecutor: ((String) -> Unit)? = null,
        print: (String) -> Unit = {},
        inventoryManager: InventoryManager? = null,
    ) {
        abortReason = null
        noFight = false
        if (client == null || character == null) {
            print("HTTP client is not available.")
            return
        }
        if (!checkHippyStone(client, character, preferences, sessionLogger, inventoryManager)) {
            print("This feature is not available to hippies.")
            return
        }

        val canInteract = character.state.value.let { !it.isHardcore && !it.isInRonin }
        when (mission) {
            "lootwhatever" -> {
                if (!canInteract) {
                    print("Cannot attack for loot if you can't interact")
                    return
                }
            }
            "flowers", "fame" -> Unit
            else -> {
                print("Unknown mission: '$mission'")
                return
            }
        }

        val victories = preferences?.getString("currentPvpVictories", "").orEmpty()
        for (target in targets) {
            if (abortReason != null) break
            if (character.state.value.pvpFightsLeft <= 0) break
            val targetName = target.playerName
            if (victories.contains(targetName)) continue
            if (targetName.lowercase().startsWith("devster")) continue
            if (!runBeforePvpScript(preferences, cliExecutor)) break
            val realMission = if (canInteract && !target.canInteract) "flowers" else mission
            print("Attacking $targetName...")
            val result = PeeVPeeRequest.fight(
                client = client,
                opponent = targetName,
                stance = stance,
                mission = realMission,
                tougher = false,
                character = character,
                preferences = preferences,
                sessionLogger = sessionLogger,
                ranked = "0",
                inventoryManager = inventoryManager,
                print = print,
            )
            if (result.isFailure) {
                abortReason = result.exceptionOrNull()?.message ?: "PvP fight failed"
                break
            }
            val html = result.getOrNull().orEmpty()
            if (html.contains("lost some dignity in the attempt")) {
                val message = "You lost to $targetName."
                print(message)
                abortReason = message
                break
            }
        }
        abortReason?.let { reason ->
            if (!reason.startsWith("You lost to ")) print(reason)
        }
    }

    internal fun runBeforePvpScript(
        preferences: Preferences?,
        cliExecutor: ((String) -> Unit)?,
    ): Boolean {
        val script = preferences?.getString("beforePVPScript", "")?.trim().orEmpty()
        if (script.isEmpty()) return true
        cliExecutor?.invoke(script)
        return abortReason == null
    }

    fun reset() {
        optionToStanceInternal.clear()
        stanceToOptionInternal.clear()
        canonicalStanceToOptionInternal.clear()
        canonicalStances = emptyArray()
        stancesKnown = false
        noFight = false
        abortReason = null
    }

    internal fun resetForTest() = reset()

    private fun matchingCanonicalNames(stanceName: String): List<String> {
        val search = canonicalName(stanceName)
        if (search.isEmpty()) return emptyList()
        canonicalStanceToOptionInternal[search]?.let { return listOf(search) }
        return canonicalStances.filter { it.contains(search) }
    }

    private fun canonicalName(name: String): String = name.trim().lowercase()
}
