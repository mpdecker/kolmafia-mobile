package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.ModifierDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.familiar.FamiliarData
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.familiar.FamiliarRequest
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.modifiers.DoubleModifier
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.CakeArenaRequest
import net.sourceforge.kolmafia.request.EquipmentRequest
import net.sourceforge.kolmafia.request.UseItemRequest
import net.sourceforge.kolmafia.skill.SkillManager
import net.sourceforge.kolmafia.session.CakeArenaManager.ArenaOpponent

/**
 * Desktop FamiliarTrainingFrame headless engine — Goals, FamiliarStatus,
 * levelFamiliar / trainFamiliar / fightMatch / buffFamiliar
 * (Phases 3246–3275). Excludes Swing UI panels.
 */
object FamiliarTrainingManager {

    enum class Goal { BASE, BUFFED, TURNS, LEARN }

    const val EMPATHY_SKILL_ID = 2009
    const val LEASH_SKILL_ID = 3010
    const val EMPATHY_EFFECT = "Empathy"
    const val LEASH_EFFECT = "Leash of Linguini"
    const val BESTIAL_EFFECT = "Bestial Sympathy"
    const val BLACK_TONGUE = "Black Tongue"
    const val GREEN_GLOW = "Healthy Green Glow"
    const val GREEN_HEART = "Heart of Green"
    const val GREEN_TONGUE = "Green Tongue"
    const val HEAVY_PETTING = "Heavy Petting"
    const val WORST_ENEMY = "Man's Worst Enemy"

    private val TINY_PLASTIC_NORMAL = intArrayOf(
        969, 970, 971, 972, 973, 974, 975, 976, 977, 978, 979, 980, 981, 982, 983, 984, 985, 986,
        987, 988,
    )
    private val TINY_PLASTIC_CRIMBO = intArrayOf(1377, 1378, 2201, 2202)

    private val FAMILIAR_ITEM_WEIGHTS = listOf(
        ItemPool.PET_SWEATER to 10,
        ItemPool.SUGAR_SHIELD to 10,
        ItemPool.PUMPKIN_BUCKET to 5,
        ItemPool.MAYFLOWER_BOUQUET to 5,
        ItemPool.FIREWORKS to 5,
        ItemPool.LEAD_NECKLACE to 3,
        ItemPool.RAT_BALLOON to -3,
        ItemPool.DAS_BOOT to -10,
        ItemPool.BATHYSPHERE to -20,
    )

    private const val DODECAPEDE_ID = 38
    private const val CHAMELEON_ID = 9
    private const val DOUBLE_FISTED_SKILL_ID = 1017

    @Volatile
    var stop: Boolean = false
    private var losses: Int = 0
    private val resultLines = mutableListOf<String>()

    fun clearResults() {
        resultLines.clear()
    }

    fun getResults(): List<String> = resultLines.toList()

    fun statusMessage(message: String) {
        resultLines.add(message)
    }

    data class TrainingDeps(
        val cakeArenaRequest: CakeArenaRequest,
        val character: KoLCharacter?,
        val familiarManager: FamiliarManager?,
        val inventory: InventoryManager?,
        val preferences: Preferences?,
        val effectManager: EffectManager? = null,
        val skillManager: SkillManager? = null,
        val equipmentRequest: EquipmentRequest? = null,
        val familiarRequest: FamiliarRequest? = null,
        val useItemRequest: UseItemRequest? = null,
        val sessionLogger: SessionLogger? = null,
        val permitsContinue: () -> Boolean = { true },
    )

    suspend fun levelFamiliar(
        goal: Int,
        type: Goal,
        deps: TrainingDeps,
        debug: Boolean = false,
    ): Boolean {
        clearResults()
        stop = false

        val familiar = deps.familiarManager?.state?.value?.activeFamiliar
        if (familiar == null || familiar.id <= 0) {
            statusMessage("No familiar selected to train.")
            return false
        }
        if (!FamiliarDefinitionDatabase.isTrainable(familiar.id)) {
            statusMessage("Don't know how to train a ${familiar.race} yet.")
            return false
        }

        if (!trainFamiliar(goal, type, deps, debug)) return false

        val result = if (type == Goal.BUFFED) buffFamiliar(goal, deps) else true
        statusMessage("Training session completed.")
        return result
    }

    private suspend fun trainFamiliar(
        goal: Int,
        type: Goal,
        deps: TrainingDeps,
        debug: Boolean,
    ): Boolean {
        val familiar = deps.familiarManager?.state?.value?.activeFamiliar ?: return false
        val status = FamiliarStatus(deps)
        printFamiliar(status, goal, type)

        var opponents = CakeArenaManager.getOpponentList()
        if (opponents.isEmpty()) {
            deps.cakeArenaRequest.visit()
            opponents = CakeArenaManager.getOpponentList()
        }
        printOpponents(opponents)
        val tool = FamiliarTool(opponents)

        RequestLogger.updateSessionLog("Starting training session...", deps.sessionLogger)
        losses = 0
        while (!goalMet(status, goal, type) && losses < 5) {
            if (stop || !deps.permitsContinue()) {
                statusMessage("Training session aborted.")
                return false
            }
            val adventures = deps.character?.state?.value?.adventuresLeft ?: 0
            if (adventures < 1) {
                statusMessage("Training stopped: out of adventures.")
                return false
            }
            val meat = deps.character?.state?.value?.meat ?: 0
            if (meat < 100) {
                statusMessage("Training stopped: out of meat.")
                return false
            }

            val active = deps.familiarManager.state.value.activeFamiliar
            if (active?.id != familiar.id) {
                deps.familiarManager.setFamiliar(familiar.race)
            }

            val weights = status.getWeights()
            val opponent = tool.bestOpponent(familiar.id, weights)
            if (opponent == null) {
                statusMessage("Couldn't choose a suitable opponent.")
                return false
            }

            status.changeGear(tool.bestWeight())
            if (debug) break

            val xp = fightMatch(status, tool, opponent, tool.bestMatch(), ignoreCounters = false, deps)
            if (xp <= 0) losses++ else losses = 0
        }

        if (losses >= 5) {
            statusMessage("Too many consecutive losses.")
            return false
        }
        return true
    }

    suspend fun buffFamiliar(weight: Int, deps: TrainingDeps): Boolean {
        val familiar = deps.familiarManager?.state?.value?.activeFamiliar
        if (familiar == null || familiar.id <= 0) {
            statusMessage("You don't have a familiar equipped.")
            return false
        }
        if (modifiedWeight(familiar, deps) >= weight) return true

        val status = FamiliarStatus(deps)
        val weights = status.getWeights().sorted()
        if (weights.isNotEmpty()) {
            status.changeGear(weights.last())
            if (modifiedWeight(
                    deps.familiarManager.state.value.activeFamiliar ?: familiar,
                    deps,
                ) >= weight
            ) {
                return true
            }
        }

        // Cast leash / empathy when available and inactive
        if (status.leashAvailable && status.leashActive == 0) {
            castSkill(LEASH_SKILL_ID, deps)
            if (modifiedWeight(deps.familiarManager.state.value.activeFamiliar ?: familiar, deps) >= weight) {
                return true
            }
        }
        if (status.empathyAvailable && status.empathyActive == 0) {
            castSkill(EMPATHY_SKILL_ID, deps)
            if (modifiedWeight(deps.familiarManager.state.value.activeFamiliar ?: familiar, deps) >= weight) {
                return true
            }
        }

        // Item buffs in desktop priority order
        val itemBuffs = listOf(
            ItemPool.GREEN_CANDY,
            ItemPool.HALF_ORCHID,
            ItemPool.PET_BUFFING_SPRAY,
            ItemPool.GREEN_SNOWCONE,
            ItemPool.BLACK_SNOWCONE,
            ItemPool.SPIKY_COLLAR,
        )
        for (itemId in itemBuffs) {
            if ((deps.inventory?.getCount(itemId) ?: 0) <= 0) continue
            deps.useItemRequest?.use(itemId, 1)
            if (modifiedWeight(deps.familiarManager.state.value.activeFamiliar ?: familiar, deps) >= weight) {
                return true
            }
        }

        statusMessage("Can't buff and equip familiar to reach $weight lbs.")
        return false
    }

    /**
     * Optional lite learnFamiliarParameters — runs [trials] fights per contest rank
     * without debug UI; returns derived skill ranks or null on abort.
     */
    suspend fun learnFamiliarParameters(trials: Int, deps: TrainingDeps): IntArray? {
        clearResults()
        stop = false
        val familiar = deps.familiarManager?.state?.value?.activeFamiliar
        if (familiar == null || familiar.id <= 0) {
            statusMessage("No familiar selected to train.")
            return null
        }
        val events = 12 * trials
        if ((deps.character?.state?.value?.adventuresLeft ?: 0) < events) {
            statusMessage("You need to have at least $events adventures available.")
            return null
        }
        if ((deps.character?.state?.value?.meat ?: 0) < 100 * events) {
            statusMessage("You need to have at least ${100 * events} meat available.")
            return null
        }

        val status = FamiliarStatus(deps)
        printFamiliar(status, trials, Goal.LEARN)
        var opponents = CakeArenaManager.getOpponentList()
        if (opponents.isEmpty()) {
            deps.cakeArenaRequest.visit()
            opponents = CakeArenaManager.getOpponentList()
        }
        val tool = FamiliarTool(opponents)
        val xp = Array(4) { IntArray(3) }
        val suckage = BooleanArray(4)
        var skills = IntArray(4)
        for (trial in 1..trials) {
            if (!deps.permitsContinue() || stop) return null
            skills = learnTrial(trial, status, tool, xp, suckage, deps) ?: return null
        }
        return skills
    }

    private suspend fun learnTrial(
        trial: Int,
        status: FamiliarStatus,
        tool: FamiliarTool,
        xp: Array<IntArray>,
        suckage: BooleanArray,
        deps: TrainingDeps,
    ): IntArray? {
        val test = IntArray(4)
        for (contest in 0 until 4) {
            test.fill(0)
            for (rank in 0 until 3) {
                if (suckage[contest]) continue
                if (stop || !deps.permitsContinue()) {
                    statusMessage("Training session aborted.")
                    return null
                }
                test[contest] = rank + 1
                val weights = status.getWeights()
                val opponent = tool.bestOpponent(test, weights)
                if (opponent == null) {
                    statusMessage("Couldn't choose a suitable opponent.")
                    return null
                }
                var match = tool.bestMatch()
                if (match != contest + 1) match = contest + 1
                status.changeGear(tool.bestWeight())
                val trialXp = fightMatch(status, tool, opponent, match, ignoreCounters = true, deps)
                if (trialXp < 0) suckage[contest] = true
                else xp[contest][rank] += trialXp
            }
        }
        return deriveSkills(xp, suckage)
    }

    private fun deriveSkills(xp: Array<IntArray>, suckage: BooleanArray): IntArray {
        val skills = IntArray(4)
        for (contest in 0 until 4) {
            var bestXp = 0
            var bestRank = 0
            for (rank in 0 until 3) {
                val rankXp = if (suckage[contest]) 0 else xp[contest][rank]
                if (rankXp > bestXp) {
                    bestXp = rankXp
                    bestRank = rank + 1
                }
            }
            skills[contest] = bestRank
        }
        return skills
    }

    private suspend fun fightMatch(
        status: FamiliarStatus,
        tool: FamiliarTool,
        opponent: ArenaOpponent,
        match: Int,
        ignoreCounters: Boolean,
        deps: TrainingDeps,
    ): Int {
        if (!deps.permitsContinue()) return 0
        printMatch(status, opponent, tool, match)
        val result = deps.cakeArenaRequest.fight(opponent.id, match, ignoreCounters)
        val html = result.getOrNull() ?: return 0
        val xp = CakeArenaRequest.earnedXP(html)
        status.processMatchResult(html, xp)
        return if (deps.cakeArenaRequest.badContest()) -1 else xp
    }

    private fun goalMet(status: FamiliarStatus, goal: Int, type: Goal): Boolean = when (type) {
        Goal.BASE -> status.baseWeight() >= goal
        Goal.BUFFED -> status.maxWeight(buffs = true) >= goal
        Goal.TURNS -> status.turnsUsed() >= goal
        Goal.LEARN -> false
    }

    private fun printFamiliar(status: FamiliarStatus, goal: Int, type: Goal) {
        val familiar = status.familiar
        val hope = when (type) {
            Goal.BASE -> " to $goal lbs. base weight"
            Goal.BUFFED -> " to $goal lbs. buffed weight"
            Goal.TURNS -> " for $goal turns"
            Goal.LEARN -> " for $goal iterations to learn arena strengths"
        }
        statusMessage("Training ${familiar.name} the ${familiar.weight} lb. ${familiar.race}$hope.")
    }

    private fun printOpponents(opponents: List<ArenaOpponent>) {
        statusMessage("Opponents:")
        for (o in opponents) {
            statusMessage("${o.name} the ${o.weight} lb. ${o.race}")
        }
    }

    private fun printMatch(
        status: FamiliarStatus,
        opponent: ArenaOpponent,
        tool: FamiliarTool,
        match: Int,
    ) {
        val familiar = status.familiar
        val weight = tool.bestWeight()
        val diff = tool.difference()
        val round = status.turnsUsed() + 1
        val opt = if (diff != 0) "; optimum = ${weight - diff} lbs." else ""
        statusMessage(
            "Round $round: ${familiar.name} ($weight lbs$opt) vs. ${opponent.name} in the " +
                "${CakeArenaManager.eventIdToName(match)} event.",
        )
        RequestLogger.updateSessionLog(
            "Round $round: ${familiar.name} vs. ${opponent.name}...",
            null,
        )
    }

    private suspend fun castSkill(skillId: Int, deps: TrainingDeps) {
        val skill = deps.skillManager?.state?.value?.skills?.firstOrNull { it.id == skillId }
            ?: return
        deps.skillManager.cast(skill, 1)
    }

    private fun modifiedWeight(familiar: FamiliarData, deps: TrainingDeps): Int {
        // Prefer live character familiar weight when available
        val base = deps.character?.state?.value?.familiarWeight?.takeIf { it > 0 }
            ?: familiar.weight
        return base.coerceAtLeast(1)
    }

    private fun effectTurns(deps: TrainingDeps, name: String): Int =
        deps.effectManager?.state?.value?.effects
            ?.firstOrNull { it.name.equals(name, ignoreCase = true) }
            ?.duration
            ?: 0

    private fun hasSkill(deps: TrainingDeps, skillId: Int): Boolean =
        deps.skillManager?.state?.value?.skills?.any { it.id == skillId } == true

    private fun itemAvailable(deps: TrainingDeps, itemId: Int): Boolean =
        (deps.inventory?.getCount(itemId) ?: 0) > 0

    private fun itemWeightModifier(itemId: Int): Int {
        val name = ItemDatabase.getItemName(itemId)
        if (name.isEmpty()) return 0
        val entry = ModifierDatabase.getItem(name) ?: return 0
        return ModifierParser.parse(entry.modifiers)
            .get(DoubleModifier.FAMILIAR_WEIGHT)
            .toInt()
    }

    /**
     * Tracks weight modifiers, buff availability, and training turn count.
     * Gear swap is a lite familiar-slot + hat approximation of desktop GearSet.
     */
    class FamiliarStatus(private val deps: TrainingDeps) {
        var familiar: FamiliarData =
            deps.familiarManager?.state?.value?.activeFamiliar
                ?: FamiliarData(0, "", "", 0, 0, 0)
            private set

        private var turns: Int = 0

        var sympathyAvailable: Boolean = false
            private set
        var empathyAvailable: Boolean = false
            private set
        var leashAvailable: Boolean = false
            private set
        var bestialAvailable: Boolean = false
            private set
        var blackConeAvailable: Boolean = false
            private set
        var greenConeAvailable: Boolean = false
            private set
        var greenHeartAvailable: Boolean = false
            private set
        var heavyPettingAvailable: Boolean = false
            private set
        var worstEnemyAvailable: Boolean = false
            private set

        var empathyActive: Int = 0
            private set
        var leashActive: Int = 0
            private set
        var bestialActive: Int = 0
            private set
        var blackTongueActive: Int = 0
            private set
        var greenGlowActive: Int = 0
            private set
        var greenHeartActive: Int = 0
            private set
        var greenTongueActive: Int = 0
            private set
        var heavyPettingActive: Int = 0
            private set
        var worstEnemyActive: Int = 0
            private set

        var leadNecklace: Boolean = false
            private set
        var pumpkinBucket: Boolean = false
            private set
        var flowerBouquet: Boolean = false
            private set
        var boxFireworks: Boolean = false
            private set
        var petSweater: Boolean = false
            private set
        var sugarShield: Boolean = false
            private set
        var ratHeadBalloon: Boolean = false
            private set
        var bathysphere: Boolean = false
            private set
        var dasBoot: Boolean = false
            private set
        var doppelganger: Boolean = false
            private set
        var pithHelmet: Boolean = false
            private set
        var crumpledFedora: Boolean = false
            private set
        var tpCount: Int = 0
            private set
        var whipCount: Int = 0
            private set
        var specWeight: Int = 0
            private set
        var familiarItemId: Int = 0
            private set

        private val weights = sortedSetOf<Int>()

        init {
            updateStatus()
        }

        fun updateStatus() {
            familiar = deps.familiarManager?.state?.value?.activeFamiliar ?: familiar
            checkSkills()
            checkEquipment()
        }

        private fun checkSkills() {
            sympathyAvailable =
                deps.preferences?.getBoolean("hasAmphibianSympathy", false) == true
            empathyAvailable = hasSkill(deps, EMPATHY_SKILL_ID)
            leashAvailable = hasSkill(deps, LEASH_SKILL_ID)
            bestialAvailable = itemAvailable(deps, ItemPool.HALF_ORCHID)
            blackConeAvailable = itemAvailable(deps, ItemPool.BLACK_SNOWCONE)
            greenConeAvailable = itemAvailable(deps, ItemPool.GREEN_SNOWCONE)
            greenHeartAvailable = itemAvailable(deps, ItemPool.GREEN_CANDY)
            heavyPettingAvailable = itemAvailable(deps, ItemPool.PET_BUFFING_SPRAY)
            worstEnemyAvailable = itemAvailable(deps, ItemPool.SPIKY_COLLAR)

            empathyActive = effectTurns(deps, EMPATHY_EFFECT)
            leashActive = effectTurns(deps, LEASH_EFFECT)
            bestialActive = effectTurns(deps, BESTIAL_EFFECT)
            blackTongueActive = effectTurns(deps, BLACK_TONGUE)
            greenGlowActive = effectTurns(deps, GREEN_GLOW)
            greenHeartActive = effectTurns(deps, GREEN_HEART)
            greenTongueActive = effectTurns(deps, GREEN_TONGUE)
            heavyPettingActive = effectTurns(deps, HEAVY_PETTING)
            worstEnemyActive = effectTurns(deps, WORST_ENEMY)
        }

        private fun checkEquipment() {
            leadNecklace = false
            pumpkinBucket = false
            flowerBouquet = false
            boxFireworks = false
            petSweater = false
            sugarShield = false
            ratHeadBalloon = false
            bathysphere = false
            dasBoot = false
            doppelganger = false
            pithHelmet = false
            crumpledFedora = false
            tpCount = 0
            whipCount = 0
            specWeight = 0
            familiarItemId = 0

            val inv = deps.inventory
            val equip = deps.character?.state?.value?.equipment.orEmpty()
            val famItemName = equip[EquipmentSlot.FAMILIAR].orEmpty()
            val hatName = equip[EquipmentSlot.HAT].orEmpty()

            fun named(itemId: Int): Boolean {
                val name = ItemDatabase.getItemName(itemId)
                return name.isNotEmpty() && (
                    famItemName.equals(name, ignoreCase = true) ||
                        (inv?.getCount(itemId) ?: 0) > 0
                    )
            }

            leadNecklace = named(ItemPool.LEAD_NECKLACE)
            pumpkinBucket = named(ItemPool.PUMPKIN_BUCKET)
            flowerBouquet = named(ItemPool.MAYFLOWER_BOUQUET)
            boxFireworks = named(ItemPool.FIREWORKS)
            petSweater = named(ItemPool.PET_SWEATER)
            sugarShield = named(ItemPool.SUGAR_SHIELD)
            ratHeadBalloon = named(ItemPool.RAT_BALLOON)
            bathysphere = named(ItemPool.BATHYSPHERE)
            dasBoot = named(ItemPool.DAS_BOOT)
            doppelganger = named(ItemPool.FAMILIAR_DOPPELGANGER)

            val pithName = ItemDatabase.getItemName(ItemPool.PLEXIGLASS_PITH_HELMET)
            pithHelmet = hatName.equals(pithName, ignoreCase = true) ||
                (inv?.getCount(ItemPool.PLEXIGLASS_PITH_HELMET) ?: 0) > 0
            val fedoraName = ItemDatabase.getItemName(ItemPool.CRUMPLED_FELT_FEDORA)
            crumpledFedora = hatName.equals(fedoraName, ignoreCase = true) ||
                (inv?.getCount(ItemPool.CRUMPLED_FELT_FEDORA) ?: 0) > 0

            whipCount = listOf(EquipmentSlot.WEAPON, EquipmentSlot.OFFHAND).count { slot ->
                val n = equip[slot].orEmpty()
                n.equals(ItemDatabase.getItemName(ItemPool.BAR_WHIP), ignoreCase = true)
            }
            val maxWhip = if (hasSkill(deps, DOUBLE_FISTED_SKILL_ID)) 2 else 1
            whipCount = (whipCount + (inv?.getCount(ItemPool.BAR_WHIP) ?: 0)).coerceAtMost(maxWhip)

            for (id in TINY_PLASTIC_NORMAL + TINY_PLASTIC_CRIMBO) {
                val qty = inv?.getCount(id) ?: 0
                tpCount = (tpCount + qty).coerceAtMost(3)
            }

            val hatchling = FamiliarDefinitionDatabase.getById(familiar.id)?.hatchlingItem.orEmpty()
            if (hatchling.isNotEmpty()) {
                val id = ItemDatabase.getByName(hatchling)?.id ?: 0
                if (id > 0) {
                    familiarItemId = id
                    specWeight = itemWeightModifier(id)
                }
            }

            for (owned in deps.familiarManager?.state?.value?.ownedFamiliars.orEmpty()) {
                val eid = owned.equipment?.itemId ?: continue
                when (eid) {
                    ItemPool.LEAD_NECKLACE -> leadNecklace = true
                    ItemPool.RAT_BALLOON -> ratHeadBalloon = true
                    ItemPool.BATHYSPHERE -> bathysphere = true
                    ItemPool.DAS_BOOT -> dasBoot = true
                    ItemPool.PUMPKIN_BUCKET -> pumpkinBucket = true
                    ItemPool.MAYFLOWER_BOUQUET -> flowerBouquet = true
                    ItemPool.FAMILIAR_DOPPELGANGER -> doppelganger = true
                    ItemPool.FIREWORKS -> boxFireworks = true
                    ItemPool.PET_SWEATER -> petSweater = true
                    ItemPool.SUGAR_SHIELD -> sugarShield = true
                }
            }
        }

        fun getWeights(): IntArray {
            weights.clear()
            var weight = familiar.weight
            if (sympathyAvailable) {
                weight += if (familiar.id == DODECAPEDE_ID) -5 else 5
            }
            if (empathyActive > 0) weight += 5
            if (leashActive > 0) weight += 5
            if (greenTongueActive > 0 || blackTongueActive > 0) weight += 5
            if (bestialActive > 0) weight += 3
            if (greenGlowActive > 0) weight += 10
            if (greenHeartActive > 0) weight += 3
            if (heavyPettingActive > 0) weight += 5
            if (worstEnemyActive > 0) weight += 5
            getItemWeights(weight)
            return weights.toIntArray()
        }

        private fun getItemWeights(weight: Int) {
            getAccessoryWeights(weight)
            if (familiar.id == CHAMELEON_ID || doppelganger) return
            if (specWeight != 0) getAccessoryWeights(weight + specWeight)
            if (petSweater) getAccessoryWeights(weight + 10)
            if (sugarShield) getAccessoryWeights(weight + 10)
            if (pumpkinBucket) getAccessoryWeights(weight + 5)
            if (flowerBouquet) getAccessoryWeights(weight + 5)
            if (boxFireworks) getAccessoryWeights(weight + 5)
            if (leadNecklace) getAccessoryWeights(weight + 3)
            if (ratHeadBalloon) getAccessoryWeights(weight - 3)
            if (dasBoot) getAccessoryWeights(weight - 10)
            if (bathysphere) getAccessoryWeights(weight - 20)
        }

        private fun getAccessoryWeights(weight: Int) {
            for (i in 0..tpCount) getWhipWeights(weight + i)
        }

        private fun getWhipWeights(weight: Int) {
            for (i in 0..whipCount) getHatWeights(weight + i * 2)
        }

        private fun getHatWeights(weight: Int) {
            if (pithHelmet) weights.add(maxOf(weight + 5, 1))
            if (crumpledFedora) weights.add(maxOf(weight + 10, 1))
            weights.add(maxOf(weight, 1))
        }

        /** Lite gear swap: pick familiar item (+ optional hat) closest to [targetWeight]. */
        suspend fun changeGear(targetWeight: Int) {
            if (doppelganger) {
                deps.familiarRequest?.equipFamiliarItem(ItemPool.FAMILIAR_DOPPELGANGER)
                return
            }
            val currentWeights = getWeights()
            if (targetWeight in currentWeights.toList()) {
                // Already achievable with current setup — try to equip matching familiar item
            }

            val candidates = mutableListOf<Pair<Int, Int>>() // itemId to delta
            candidates.add(0 to 0)
            if (familiarItemId > 0 && specWeight != 0) candidates.add(familiarItemId to specWeight)
            for ((id, delta) in FAMILIAR_ITEM_WEIGHTS) {
                val available = itemAvailable(deps, id) || when (id) {
                    ItemPool.LEAD_NECKLACE -> leadNecklace
                    ItemPool.PUMPKIN_BUCKET -> pumpkinBucket
                    ItemPool.MAYFLOWER_BOUQUET -> flowerBouquet
                    ItemPool.FIREWORKS -> boxFireworks
                    ItemPool.PET_SWEATER -> petSweater
                    ItemPool.SUGAR_SHIELD -> sugarShield
                    ItemPool.BATHYSPHERE -> bathysphere
                    ItemPool.DAS_BOOT -> dasBoot
                    ItemPool.RAT_BALLOON -> ratHeadBalloon
                    else -> false
                }
                if (available) candidates.add(id to delta)
            }

            var bestId = 0
            var bestDiff = Int.MAX_VALUE
            val base = familiar.weight +
                (if (sympathyAvailable) if (familiar.id == DODECAPEDE_ID) -5 else 5 else 0) +
                (if (empathyActive > 0) 5 else 0) +
                (if (leashActive > 0) 5 else 0)

            for ((id, delta) in candidates) {
                val w = maxOf(base + delta, 1)
                val diff = kotlin.math.abs(w - targetWeight)
                if (diff < bestDiff) {
                    bestDiff = diff
                    bestId = id
                }
            }
            if (bestId > 0) {
                deps.familiarRequest?.equipFamiliarItem(bestId)
                statusMessage("Putting on ${ItemDatabase.getItemName(bestId)}")
            }

            val needMore = targetWeight - (base + (candidates.firstOrNull { it.first == bestId }?.second ?: 0))
            when {
                needMore >= 10 && crumpledFedora ->
                    deps.equipmentRequest?.equipItem(ItemPool.CRUMPLED_FELT_FEDORA, EquipmentSlot.HAT)
                needMore >= 5 && pithHelmet ->
                    deps.equipmentRequest?.equipItem(ItemPool.PLEXIGLASS_PITH_HELMET, EquipmentSlot.HAT)
            }
            updateStatus()
        }

        fun processMatchResult(response: String, xp: Int) {
            if (!response.contains("You enter")) return
            val message = if (xp > 0) {
                "${familiar.name} gains $xp experience" +
                    if (response.contains("gains a pound")) " and a pound." else "."
            } else {
                "${familiar.name} lost."
            }
            statusMessage(message)

            val prize = Regex("""You (?:win a prize|acquire an item): <b>(.*?)</b>""")
                .find(response)?.groupValues?.get(1)
                ?: Regex("""steals? an item: <b>(.*?)</b>""")
                    .find(response)?.groupValues?.get(1)
            if (prize != null) {
                statusMessage("You win a prize: $prize.")
                if (prize.equals("lead necklace", ignoreCase = true)) leadNecklace = true
                else if (specWeight > 0 && familiarItemId > 0) {
                    // familiar item prize
                }
            }

            turns++
            if (leashActive > 0) leashActive--
            if (empathyActive > 0) empathyActive--
            if (bestialActive > 0) bestialActive--
            if (blackTongueActive > 0) blackTongueActive--
            if (greenGlowActive > 0) greenGlowActive--
            if (greenHeartActive > 0) greenHeartActive--
            if (greenTongueActive > 0) greenTongueActive--
            if (heavyPettingActive > 0) heavyPettingActive--
            if (worstEnemyActive > 0) worstEnemyActive--

            // Refresh familiar snapshot after XP apply from CakeArenaRequest
            familiar = deps.familiarManager?.state?.value?.activeFamiliar ?: familiar
        }

        fun turnsUsed(): Int = turns

        fun baseWeight(): Int = familiar.weight

        fun maxWeight(buffs: Boolean): Int {
            var weight = familiar.weight
            if (sympathyAvailable) {
                weight += if (familiar.id == DODECAPEDE_ID) -5 else 5
            }
            if (buffs) {
                if (leashAvailable || leashActive > 0) weight += 5
                if (empathyAvailable || empathyActive > 0) weight += 5
                if (bestialAvailable || bestialActive > 0) weight += 3
                if (greenConeAvailable || greenTongueActive > 0 ||
                    blackConeAvailable || blackTongueActive > 0
                ) {
                    weight += 5
                }
                if (greenGlowActive > 0) weight += 10
                if (greenHeartAvailable || greenHeartActive > 0) weight += 3
                if (heavyPettingAvailable || heavyPettingActive > 0) weight += 5
                if (worstEnemyAvailable || worstEnemyActive > 0) weight += 5
            } else {
                if (leashActive > 0) weight += 5
                if (empathyActive > 0) weight += 5
                if (bestialActive > 0) weight += 3
                if (greenGlowActive > 0) weight += 10
                if (greenHeartActive > 0) weight += 3
                if (greenTongueActive > 0 || blackTongueActive > 0) weight += 5
                if (heavyPettingActive > 0) weight += 5
                if (worstEnemyActive > 0) weight += 5
            }
            when {
                pumpkinBucket -> weight += 5
                flowerBouquet -> weight += 5
                boxFireworks -> weight += 5
                petSweater -> weight += 10
                sugarShield -> weight += 10
                specWeight > 3 -> weight += specWeight
                leadNecklace -> weight += 3
            }
            weight += tpCount
            weight += 2 * whipCount
            if (crumpledFedora) weight += 10
            else if (pithHelmet) weight += 5
            return maxOf(weight, 1)
        }
    }
}
