package net.sourceforge.kolmafia.session

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Tracks acquisition goals for the current automation session.
 * Supports item goals by ID, item goals by name (case-insensitive), meat goals, and level goals.
 */
class GoalManager {
    private val _itemGoalIds   = mutableMapOf<Int, Int>()
    private val _itemGoalNames = mutableMapOf<String, Int>()  // stored lowercase+trimmed -> count
    private var meatGoal: Int?  = null
    private var levelGoal: Int? = null
    private var factoidGoal: String? = null
    private var factoidGoalCount: Int = 0
    private var choiceGoalId: Int? = null
    private var choiceAdventureCount: Int = 0
    private var floundryGoalCount: Int = 0
    private var autostopGoalCount: Int = 0
    private var substatsGoal: Boolean = false
    private val substatsCounts = IntArray(3)
    private var leprecondoGoalCount: Int = 0
    private var pseudoKind: GoalPseudoConditions.Kind? = null
    private var pseudoTarget: Int = 0
    private var resourceKind: ResourceKind? = null
    private var resourceTarget: Int = 0
    var outfitGoalActive: Boolean = false
        internal set

    enum class ResourceKind { HEALTH, MANA }

    data class ConditionContext(
        val characterState: CharacterState? = null,
        val preferences: Preferences? = null,
        val lastAdventure: String = "",
        val isEquipped: (String) -> Boolean = { false },
    )

    // ── Factoid goal (response text match) ────────────────────────────────────

    fun setFactoidGoal(text: String) { factoidGoal = text.trim().takeIf { it.isNotBlank() } }
    fun clearFactoidGoal() { factoidGoal = null }
    fun hasFactoidGoalSet(): Boolean = factoidGoal != null || hasFactoidCountGoal()
    fun matchesFactoid(responseText: String): Boolean {
        val goal = factoidGoal ?: return false
        return responseText.contains(goal, ignoreCase = true)
    }

    // ── ID-based item goals ───────────────────────────────────────────────────

    fun addItemGoal(itemId: Int, count: Int = 1) {
        if (count <= 0) {
            removeItemGoal(itemId)
            return
        }
        _itemGoalIds[itemId] = (_itemGoalIds[itemId] ?: 0) + count
    }
    fun removeItemGoal(itemId: Int) { _itemGoalIds.remove(itemId) }
    fun hasItemGoal(itemId: Int): Boolean = (_itemGoalIds[itemId] ?: 0) > 0
    fun itemGoalCount(itemId: Int): Int = _itemGoalIds[itemId] ?: 0
    fun itemGoalIds(): Set<Int> = _itemGoalIds.filterValues { it > 0 }.keys

    // ── Name-based item goals (case-insensitive) ──────────────────────────────

    fun addItemGoalByName(name: String, count: Int = 1) {
        val key = name.lowercase().trim()
        if (key.isEmpty()) return
        if (count <= 0) {
            removeItemGoalByName(name)
            return
        }
        _itemGoalNames[key] = (_itemGoalNames[key] ?: 0) + count
    }
    fun removeItemGoalByName(name: String) { _itemGoalNames.remove(name.lowercase().trim()) }
    fun hasItemGoalByName(name: String): Boolean = (_itemGoalNames[name.lowercase().trim()] ?: 0) > 0
    fun itemGoalNameCount(name: String): Int = _itemGoalNames[name.lowercase().trim()] ?: 0

    // ── Meat goal ─────────────────────────────────────────────────────────────

    fun setMeatGoal(meat: Int)  { meatGoal = meat }
    fun clearMeatGoal()         { meatGoal = null }
    /** Returns true when [currentMeat] meets or exceeds the configured meat goal. */
    fun hasMeatGoal(currentMeat: Int): Boolean = meatGoal?.let { currentMeat >= it } ?: false

    // ── Level goal ────────────────────────────────────────────────────────────

    fun setLevelGoal(level: Int)  { levelGoal = level }
    fun clearLevelGoal()          { levelGoal = null }
    /** Returns true when [currentLevel] meets or exceeds the configured level goal. */
    fun hasLevelGoal(currentLevel: Int): Boolean = levelGoal?.let { currentLevel >= it } ?: false

    // ── Phase 10 ASH helpers ──────────────────────────────────────────────────

    /** True if any item goal (by ID or name) is active. */
    fun hasItemGoals(): Boolean =
        _itemGoalIds.values.any { it > 0 } || _itemGoalNames.values.any { it > 0 }

    /** True if a meat goal has been set (regardless of current meat). */
    fun hasMeatGoalSet(): Boolean = meatGoal != null

    /** True if a level goal has been set (regardless of current level). */
    fun hasLevelGoalSet(): Boolean = levelGoal != null

    fun hasFactoidGoal(): Boolean = factoidGoal != null

    // ── Choice goal (stop after resolving a specific choice adventure) ────────

    fun setChoiceGoal(choiceId: Int) { choiceGoalId = choiceId }
    fun clearChoiceGoal() { choiceGoalId = null }
    fun hasChoiceGoal(choiceId: Int): Boolean = choiceGoalId == choiceId
    fun hasChoiceGoalSet(): Boolean = choiceGoalId != null

    fun setChoiceAdventureGoal(count: Int) { choiceAdventureCount = count.coerceAtLeast(0) }
    fun clearChoiceAdventureGoal() { choiceAdventureCount = 0 }
    fun hasChoiceAdventureGoal(): Boolean = choiceAdventureCount > 0
    fun noteChoiceAdventureCompleted() {
        if (choiceAdventureCount > 0) choiceAdventureCount--
    }

    fun setFactoidCountGoal(count: Int) {
        factoidGoalCount = count.coerceAtLeast(0)
        if (count > 0) factoidGoal = null
    }
    fun clearFactoidCountGoal() { factoidGoalCount = 0 }
    fun hasFactoidCountGoal(): Boolean = factoidGoalCount > 0
    /** Desktop GOAL_FACTOID progress — returns true when the count goal just reached zero. */
    fun noteFactoidLearned(count: Int = 1): Boolean {
        if (factoidGoalCount <= 0) return false
        factoidGoalCount = (factoidGoalCount - count).coerceAtLeast(0)
        return factoidGoalCount == 0
    }

    fun setFloundryGoal(count: Int) { floundryGoalCount = count.coerceAtLeast(0) }
    fun clearFloundryGoal() { floundryGoalCount = 0 }
    fun hasFloundryGoal(): Boolean = floundryGoalCount > 0
    fun noteFloundryFishCaught() {
        if (floundryGoalCount > 0) floundryGoalCount--
    }

    fun setAutostopGoal(count: Int = 1) { autostopGoalCount = count.coerceAtLeast(0) }
    fun clearAutostopGoal() { autostopGoalCount = 0 }
    fun hasAutostopGoal(): Boolean = autostopGoalCount > 0

    // ── Substats goal (stop when a substat is gained) ─────────────────────────

    fun setSubstatsGoal(enabled: Boolean = true) { substatsGoal = enabled }
    fun clearSubstatsGoal() {
        substatsGoal = false
        substatsCounts.fill(0)
    }
    fun hasSubstatsGoal(): Boolean = substatsGoal || substatsCounts.any { it > 0 }

    fun setPseudoGoal(kind: GoalPseudoConditions.Kind, target: Int) {
        pseudoKind = kind
        pseudoTarget = target.coerceAtLeast(0)
    }

    fun clearPseudoGoal() {
        pseudoKind = null
        pseudoTarget = 0
    }

    fun hasPseudoGoal(): Boolean = pseudoKind != null && pseudoTarget > 0

    fun hasPseudoGoalMet(preferences: Preferences?): Boolean {
        val kind = pseudoKind ?: return false
        val prefs = preferences ?: return false
        return GoalPseudoConditions.isMet(kind, pseudoTarget, prefs)
    }

    fun setResourceGoal(kind: ResourceKind, target: Int) {
        resourceKind = kind
        resourceTarget = target.coerceAtLeast(0)
    }

    fun clearResourceGoal() {
        resourceKind = null
        resourceTarget = 0
    }

    fun hasResourceGoal(): Boolean = resourceKind != null && resourceTarget > 0

    fun hasResourceGoalMet(state: CharacterState): Boolean = when (resourceKind) {
        ResourceKind.HEALTH -> state.currentHp >= resourceTarget
        ResourceKind.MANA -> state.currentMp >= resourceTarget
        null -> false
    }

    fun hasHealthGoal(): Boolean = resourceKind == ResourceKind.HEALTH && resourceTarget > 0

    fun hasManaGoal(): Boolean = resourceKind == ResourceKind.MANA && resourceTarget > 0
    fun setSubstatsCounts(muscle: Int, mysticality: Int, moxie: Int) {
        substatsCounts[0] = muscle.coerceAtLeast(0)
        substatsCounts[1] = mysticality.coerceAtLeast(0)
        substatsCounts[2] = moxie.coerceAtLeast(0)
        substatsGoal = substatsCounts.any { it > 0 }
    }
    fun noteSubstatGain(index: Int, amount: Int = 1) {
        if (!substatsGoal || index !in 0..2) return
        substatsCounts[index] = (substatsCounts[index] - amount).coerceAtLeast(0)
        if (substatsCounts.all { it == 0 }) clearSubstatsGoal()
    }
    fun matchesSubstats(responseText: String): Boolean =
        substatsGoal && responseText.contains("You gain", ignoreCase = true) &&
            Regex("""You gain \d+ \w+ \(\d+ exp\)""").containsMatchIn(responseText)

    /** Desktop [GoalManager.GOAL_LEPRECONDO] furniture-discovery progress. */
    fun setLeprecondoGoal(count: Int) { leprecondoGoalCount = count.coerceAtLeast(0) }
    fun clearLeprecondoGoal() { leprecondoGoalCount = 0 }
    fun hasLeprecondoGoal(): Boolean = leprecondoGoalCount > 0
    fun noteLeprecondoProgress() {
        if (leprecondoGoalCount > 0) leprecondoGoalCount--
    }

    /** Remove the first item goal matching [itemName] (case-insensitive). */
    fun removeGoal(itemName: String) { removeItemGoalByName(itemName) }

    /** Serialize all active goals as human-readable strings. */
    fun allGoalsAsStrings(): List<String> = buildList {
        _itemGoalIds.forEach { (id, count) -> add("item id:$id x$count") }
        _itemGoalNames.forEach { (name, count) ->
            add(if (count == 1) "item name:$name" else "item name:$name x$count")
        }
        meatGoal?.let  { add("meat:$it") }
        levelGoal?.let { add("level:$it") }
        factoidGoal?.let { add("factoid:$it") }
        if (factoidGoalCount > 0) add("factoid-count:$factoidGoalCount")
        choiceGoalId?.let { add("choice-id:$it") }
        if (choiceAdventureCount > 0) add("choice:$choiceAdventureCount")
        if (substatsGoal) add("substats:${substatsCounts.joinToString("/")}")
        if (leprecondoGoalCount > 0) add("leprecondo:$leprecondoGoalCount")
        if (floundryGoalCount > 0) add("floundry:$floundryGoalCount")
        if (autostopGoalCount > 0) add("autostop:$autostopGoalCount")
        pseudoKind?.let { add("pseudo:$it:$pseudoTarget") }
        resourceKind?.let { add("$it:$resourceTarget") }
    }

    /** Desktop GoalManager.getGoalCount — remaining count for count-based condition types. */
    fun goalCount(
        type: String,
        preferences: Preferences? = null,
        state: CharacterState? = null,
        inventoryCount: ((Int) -> Int)? = null,
    ): Int = when (type.lowercase().trim()) {
        "choice", "choiceadv", "choices" -> choiceAdventureCount
        "factoid", "factoids", "manuel" -> when {
            factoidGoalCount > 0 -> factoidGoalCount
            factoidGoal != null -> 1
            else -> 0
        }
        "floundry", "floundry fish" -> floundryGoalCount
        "leprecondo", "leprecondo furniture" -> leprecondoGoalCount
        "autostop" -> autostopGoalCount
        "meat" -> if (meatGoal != null && state != null) {
            (meatGoal!! - state.meat).coerceAtLeast(0)
        } else 0
        "level" -> if (levelGoal != null && state != null) {
            (levelGoal!! - state.level).coerceAtLeast(0)
        } else 0
        "item", "items" -> remainingItemGoalCount(inventoryCount)
        "substats" -> substatsCounts.sum()
        "health", "hp" -> if (resourceKind == ResourceKind.HEALTH && state != null) {
            (resourceTarget - state.currentHp).coerceAtLeast(0)
        } else 0
        "mana", "mp" -> if (resourceKind == ResourceKind.MANA && state != null) {
            (resourceTarget - state.currentMp).coerceAtLeast(0)
        } else 0
        "pseudo", "pirate insult", "pirate insults", "arena flyer ml", "chasm bridge" -> {
            val kind = pseudoKind
            val prefs = preferences
            if (kind == null || prefs == null) pseudoTarget
            else (pseudoTarget - GoalPseudoConditions.currentCount(kind, prefs)).coerceAtLeast(0)
        }
        else -> 0
    }

    /** Sum of remaining item goals after subtracting accessible inventory counts. */
    fun remainingItemGoalCount(inventoryCount: ((Int) -> Int)? = null): Int {
        var total = 0
        _itemGoalIds.forEach { (itemId, needed) ->
            val have = inventoryCount?.invoke(itemId) ?: 0
            total += (needed - have).coerceAtLeast(0)
        }
        _itemGoalNames.forEach { (name, needed) ->
            val itemId = net.sourceforge.kolmafia.data.ItemDatabase.getByName(name)?.id ?: return@forEach
            val have = inventoryCount?.invoke(itemId) ?: 0
            total += (needed - have).coerceAtLeast(0)
        }
        return total
    }

    /** Desktop AdventureResult.getConditionType parity for goal_exists(). */
    fun matchesConditionType(type: String): Boolean {
        val normalized = type.trim()
        return when {
            normalized.equals("item", ignoreCase = true) ->
                hasItemGoals() && !outfitGoalActive
            normalized.equals("outfit", ignoreCase = true) -> outfitGoalActive
            normalized.equals("meat", ignoreCase = true) -> hasMeatGoalSet()
            normalized.equals("level", ignoreCase = true) -> hasLevelGoalSet()
            normalized.equals("factoid", ignoreCase = true) ||
                normalized.equals("factoids", ignoreCase = true) ||
                normalized.equals("manuel", ignoreCase = true) -> hasFactoidGoalSet()
            normalized.equals("autostop", ignoreCase = true) -> hasAutostopGoal()
            normalized.equals("choice", ignoreCase = true) -> hasChoiceGoalSet()
            normalized.equals("choiceadv", ignoreCase = true) ||
                normalized.equals("choices", ignoreCase = true) ->
                hasChoiceAdventureGoal() || hasChoiceGoalSet()
            normalized.equals("floundry", ignoreCase = true) ||
                normalized.equals("floundry fish", ignoreCase = true) -> hasFloundryGoal()
            normalized.equals("leprecondo", ignoreCase = true) ||
                normalized.equals("leprecondo furniture", ignoreCase = true) -> hasLeprecondoGoal()
            normalized.equals("substats", ignoreCase = true) -> hasSubstatsGoal()
            normalized.equals("health", ignoreCase = true) ||
                normalized.equals("hp", ignoreCase = true) -> hasHealthGoal()
            normalized.equals("mana", ignoreCase = true) ||
                normalized.equals("mp", ignoreCase = true) -> hasManaGoal()
            normalized.equals("pseudo", ignoreCase = true) ||
                normalized.equals("pirate insult", ignoreCase = true) ||
                normalized.equals("pirate insults", ignoreCase = true) -> hasPseudoGoal()
            normalized.equals("arena flyer ml", ignoreCase = true) ->
                pseudoKind == GoalPseudoConditions.Kind.ARENA_FLYER_ML
            normalized.equals("chasm bridge progress", ignoreCase = true) ->
                pseudoKind == GoalPseudoConditions.Kind.CHASM_BRIDGE
            else -> false
        }
    }

    fun applyCondition(
        parsed: GoalConditionParser.ParsedCondition,
        mode: ConditionMode,
        context: ConditionContext = ConditionContext(),
    ) {
        when (parsed.kind) {
            GoalConditionParser.ParsedCondition.Kind.MEAT -> when (mode) {
                ConditionMode.SET -> setMeatGoal(parsed.count)
                ConditionMode.ADD -> setMeatGoal(parsed.count)
                ConditionMode.REMOVE -> clearMeatGoal()
            }
            GoalConditionParser.ParsedCondition.Kind.LEVEL -> when (mode) {
                ConditionMode.SET -> setLevelGoal(parsed.count)
                ConditionMode.ADD -> setLevelGoal(parsed.count)
                ConditionMode.REMOVE -> clearLevelGoal()
            }
            GoalConditionParser.ParsedCondition.Kind.LEVEL_SUBSTATS -> {
                val state = context.characterState
                if (state != null) {
                    val counts = SubstatCalculator.substatPointsForLevel(parsed.count, state)
                    when (mode) {
                        ConditionMode.SET, ConditionMode.ADD -> setSubstatsCounts(counts[0], counts[1], counts[2])
                        ConditionMode.REMOVE -> clearSubstatsGoal()
                    }
                } else when (mode) {
                    ConditionMode.SET, ConditionMode.ADD -> setLevelGoal(parsed.count)
                    ConditionMode.REMOVE -> clearLevelGoal()
                }
            }
            GoalConditionParser.ParsedCondition.Kind.SUBSTAT_POINTS -> {
                val state = context.characterState ?: return
                val index = parsed.statIndex ?: return
                val remaining = SubstatCalculator.remainingSubstatPoints(parsed.count, state, index)
                val counts = substatsCounts.copyOf()
                when (mode) {
                    ConditionMode.SET -> counts.fill(0)
                    ConditionMode.REMOVE -> {
                        counts[index] = 0
                        setSubstatsCounts(counts[0], counts[1], counts[2])
                        return
                    }
                    ConditionMode.ADD -> Unit
                }
                counts[index] = when (mode) {
                    ConditionMode.ADD -> counts[index] + remaining
                    else -> remaining
                }
                setSubstatsCounts(counts[0], counts[1], counts[2])
            }
            GoalConditionParser.ParsedCondition.Kind.HEALTH,
            GoalConditionParser.ParsedCondition.Kind.MANA,
            -> {
                val state = context.characterState ?: return
                val max = if (parsed.kind == GoalConditionParser.ParsedCondition.Kind.HEALTH) {
                    state.maxHp
                } else {
                    state.maxMp
                }
                val current = if (parsed.kind == GoalConditionParser.ParsedCondition.Kind.HEALTH) {
                    state.currentHp
                } else {
                    state.currentMp
                }
                val points = if (parsed.percent) {
                    (parsed.count.toDouble() * max / 100.0).toInt()
                } else {
                    parsed.count
                }
                val kind = if (parsed.kind == GoalConditionParser.ParsedCondition.Kind.HEALTH) {
                    ResourceKind.HEALTH
                } else {
                    ResourceKind.MANA
                }
                when (mode) {
                    ConditionMode.SET, ConditionMode.ADD -> setResourceGoal(kind, points.coerceAtLeast(current))
                    ConditionMode.REMOVE -> clearResourceGoal()
                }
            }
            GoalConditionParser.ParsedCondition.Kind.PIRATE_INSULT,
            GoalConditionParser.ParsedCondition.Kind.ARENA_FLYER_ML,
            GoalConditionParser.ParsedCondition.Kind.CHASM_BRIDGE,
            -> {
                val kind = parsed.pseudoKind ?: return
                when (mode) {
                    ConditionMode.SET, ConditionMode.ADD -> setPseudoGoal(kind, parsed.count)
                    ConditionMode.REMOVE -> clearPseudoGoal()
                }
            }
            GoalConditionParser.ParsedCondition.Kind.OUTFIT -> {
                val location = parsed.outfitLocation?.takeIf { it.isNotBlank() }
                    ?: context.lastAdventure
                if (location.isBlank()) return
                GoalOutfitConditions.addOutfitConditions(location, this, mode, context.isEquipped)
            }
            GoalConditionParser.ParsedCondition.Kind.CHOICE_ADVENTURES -> when (mode) {
                ConditionMode.SET -> setChoiceAdventureGoal(parsed.count)
                ConditionMode.ADD -> setChoiceAdventureGoal(choiceAdventureCount + parsed.count)
                ConditionMode.REMOVE -> clearChoiceAdventureGoal()
            }
            GoalConditionParser.ParsedCondition.Kind.CHOICE_ID -> when (mode) {
                ConditionMode.SET, ConditionMode.ADD -> parsed.choiceId?.let { setChoiceGoal(it) }
                ConditionMode.REMOVE -> clearChoiceGoal()
            }
            GoalConditionParser.ParsedCondition.Kind.FACTOID_TEXT -> when (mode) {
                ConditionMode.SET, ConditionMode.ADD -> parsed.text?.let { setFactoidGoal(it) }
                ConditionMode.REMOVE -> clearFactoidGoal()
            }
            GoalConditionParser.ParsedCondition.Kind.FACTOID_COUNT -> when (mode) {
                ConditionMode.SET -> setFactoidCountGoal(parsed.count)
                ConditionMode.ADD -> setFactoidCountGoal(factoidGoalCount + parsed.count)
                ConditionMode.REMOVE -> clearFactoidCountGoal()
            }
            GoalConditionParser.ParsedCondition.Kind.LEPRECONDO -> when (mode) {
                ConditionMode.SET -> setLeprecondoGoal(parsed.count)
                ConditionMode.ADD -> setLeprecondoGoal(leprecondoGoalCount + parsed.count)
                ConditionMode.REMOVE -> clearLeprecondoGoal()
            }
            GoalConditionParser.ParsedCondition.Kind.FLOUNDRY -> when (mode) {
                ConditionMode.SET -> setFloundryGoal(parsed.count)
                ConditionMode.ADD -> setFloundryGoal(floundryGoalCount + parsed.count)
                ConditionMode.REMOVE -> clearFloundryGoal()
            }
            GoalConditionParser.ParsedCondition.Kind.AUTOSTOP -> when (mode) {
                ConditionMode.SET -> setAutostopGoal(parsed.count)
                ConditionMode.ADD -> setAutostopGoal(autostopGoalCount + parsed.count)
                ConditionMode.REMOVE -> clearAutostopGoal()
            }
            GoalConditionParser.ParsedCondition.Kind.SUBSTATS -> when (mode) {
                ConditionMode.SET, ConditionMode.ADD -> setSubstatsGoal(true)
                ConditionMode.REMOVE -> clearSubstatsGoal()
            }
            GoalConditionParser.ParsedCondition.Kind.ITEM_NAME -> {
                val name = parsed.itemName ?: return
                when (mode) {
                    ConditionMode.SET -> {
                        _itemGoalNames.clear()
                        addItemGoalByName(name, parsed.count)
                    }
                    ConditionMode.ADD -> addItemGoalByName(name, parsed.count)
                    ConditionMode.REMOVE -> removeItemGoalByName(name)
                }
            }
            GoalConditionParser.ParsedCondition.Kind.ITEM_ID -> Unit
            GoalConditionParser.ParsedCondition.Kind.REMOVE -> Unit
        }
    }

    enum class ConditionMode { ADD, SET, REMOVE }

    fun noteItemProgress(itemId: Int, count: Int = 1) {
        val current = _itemGoalIds[itemId] ?: return
        if (current <= count) _itemGoalIds.remove(itemId) else _itemGoalIds[itemId] = current - count
    }

    fun noteItemProgressByName(name: String, count: Int = 1) {
        val key = name.lowercase().trim()
        val current = _itemGoalNames[key] ?: return
        if (current <= count) _itemGoalNames.remove(key) else _itemGoalNames[key] = current - count
    }

    fun hasAnyGoals(): Boolean = allGoalsAsStrings().isNotEmpty()

    // ── Clear all ─────────────────────────────────────────────────────────────

    fun clearGoals() {
        _itemGoalIds.clear()
        _itemGoalNames.clear()
        choiceGoalId = null
        choiceAdventureCount = 0
        factoidGoalCount = 0
        floundryGoalCount = 0
        autostopGoalCount = 0
        substatsGoal = false
        substatsCounts.fill(0)
        leprecondoGoalCount = 0
        pseudoKind = null
        pseudoTarget = 0
        resourceKind = null
        resourceTarget = 0
        meatGoal = null
        levelGoal = null
        factoidGoal = null
        outfitGoalActive = false
    }

    companion object {
        /** Desktop [GoalManager.checkAutoStop] — adventure loop reads [EncounterManager.pendingAutoStop]. */
        fun checkAutoStop(@Suppress("UNUSED_PARAMETER") message: String) {
            // No-op: EncounterManager.recognizeEncounter already sets pendingAutoStop.
        }
    }

    data class GoalSnapshot(
        val itemGoalIds: Map<Int, Int>,
        val itemGoalNames: Map<String, Int>,
        val meatGoal: Int?,
        val levelGoal: Int?,
        val factoidGoal: String?,
        val factoidGoalCount: Int,
        val choiceGoalId: Int?,
        val choiceAdventureCount: Int,
        val floundryGoalCount: Int,
        val autostopGoalCount: Int,
        val substatsGoal: Boolean,
        val substatsCounts: IntArray,
        val leprecondoGoalCount: Int,
        val pseudoKind: GoalPseudoConditions.Kind?,
        val pseudoTarget: Int,
        val resourceKind: ResourceKind?,
        val resourceTarget: Int,
    )

    fun captureSnapshot(): GoalSnapshot = GoalSnapshot(
        itemGoalIds = _itemGoalIds.toMap(),
        itemGoalNames = _itemGoalNames.toMap(),
        meatGoal = meatGoal,
        levelGoal = levelGoal,
        factoidGoal = factoidGoal,
        factoidGoalCount = factoidGoalCount,
        choiceGoalId = choiceGoalId,
        choiceAdventureCount = choiceAdventureCount,
        floundryGoalCount = floundryGoalCount,
        autostopGoalCount = autostopGoalCount,
        substatsGoal = substatsGoal,
        substatsCounts = substatsCounts.copyOf(),
        leprecondoGoalCount = leprecondoGoalCount,
        pseudoKind = pseudoKind,
        pseudoTarget = pseudoTarget,
        resourceKind = resourceKind,
        resourceTarget = resourceTarget,
    )

    fun restoreSnapshot(snapshot: GoalSnapshot) {
        clearGoals()
        snapshot.itemGoalIds.forEach { (id, count) -> addItemGoal(id, count) }
        snapshot.itemGoalNames.forEach { (name, count) -> addItemGoalByName(name, count) }
        snapshot.meatGoal?.let { setMeatGoal(it) }
        snapshot.levelGoal?.let { setLevelGoal(it) }
        snapshot.factoidGoal?.let { setFactoidGoal(it) }
        factoidGoalCount = snapshot.factoidGoalCount
        snapshot.choiceGoalId?.let { setChoiceGoal(it) }
        choiceAdventureCount = snapshot.choiceAdventureCount
        floundryGoalCount = snapshot.floundryGoalCount
        autostopGoalCount = snapshot.autostopGoalCount
        if (snapshot.substatsGoal) {
            setSubstatsCounts(snapshot.substatsCounts[0], snapshot.substatsCounts[1], snapshot.substatsCounts[2])
        }
        leprecondoGoalCount = snapshot.leprecondoGoalCount
        pseudoKind = snapshot.pseudoKind
        pseudoTarget = snapshot.pseudoTarget
        resourceKind = snapshot.resourceKind
        resourceTarget = snapshot.resourceTarget
    }

    /**
     * Desktop [GoalManager.makeSideTrip] — temporarily pursue [itemId] at [location], then restore goals.
     * [itemCount] supplies the current inventory count for [itemId] after the loop (goal may still be set).
     */
    suspend fun runSideTripForItem(
        adventureManager: AdventureManager,
        location: AdventureLocation,
        itemId: Int,
        maxTurns: Int,
        scope: CoroutineScope,
        itemCount: () -> Int,
    ): Boolean {
        if (maxTurns <= 0) return false

        val snapshot = captureSnapshot()
        val initialCount = itemCount()
        clearGoals()
        addItemGoal(itemId)

        val job: Job = adventureManager.runAdventures(location, maxTurns, scope)
        joinAll(job)

        val obtained = itemCount() > initialCount
        restoreSnapshot(snapshot)
        return obtained
    }
}
