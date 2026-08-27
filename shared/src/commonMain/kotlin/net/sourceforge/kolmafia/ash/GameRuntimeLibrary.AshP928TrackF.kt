package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.CandyDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.SkillCosts
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase

/**
 * AshP928–934 Track F — Candy synthesis + skill costs.
 *
 * Phase 928: sweet_synthesis / sweet_synthesis_result / candy_for_tier (live via Track J helpers)
 * Phase 929–934: adv_cost / hp_cost / soulsauce_cost / thunder_cost / rain_cost /
 *                lightning_cost / fuel_cost / meat_cost / turns_per_cast
 */
internal fun GameRuntimeLibrary.registerAshP928TrackFBatch(scope: AshScope) {
    // ── Phase 928: sweet_synthesis (live) ───────────────────────────
    regFn(scope, "sweet_synthesis", AshType.BOOLEAN,
        listOf("item1" to AshType.ITEM, "item2" to AshType.ITEM)) { _, args ->
        val id1 = gameDatabase?.item(args[0].toString())?.id
            ?: ItemDatabase.getByName(args[0].toString())?.id ?: 0
        val id2 = gameDatabase?.item(args[1].toString())?.id
            ?: ItemDatabase.getByName(args[1].toString())?.id ?: 0
        AshValue.of(runSweetSynthesisPair(id1, id2, 1))
    }

    regFn(scope, "sweet_synthesis", AshType.BOOLEAN,
        listOf("effect" to AshType.EFFECT)) { _, args ->
        AshValue.of(runSweetSynthesisEffect(args[0].toString(), 1))
    }

    regFn(scope, "sweet_synthesis", AshType.BOOLEAN,
        listOf("count" to AshType.INT, "effect" to AshType.EFFECT)) { _, args ->
        AshValue.of(runSweetSynthesisEffect(args[1].toString(), args[0].toLong().toInt()))
    }

    regFn(scope, "sweet_synthesis_result", AshType.EFFECT,
        listOf("item1" to AshType.ITEM, "item2" to AshType.ITEM)) { _, args ->
        val item1Name = args[0].toString()
        val item2Name = args[1].toString()
        val item1Id = gameDatabase?.item(item1Name)?.id
            ?: ItemDatabase.getByName(item1Name)?.id
            ?: return@regFn AshValue.effect("")
        val item2Id = gameDatabase?.item(item2Name)?.id
            ?: ItemDatabase.getByName(item2Name)?.id
            ?: return@regFn AshValue.effect("")
        val effectId = CandyDatabase.synthesisResult(item1Id, item2Id)
        if (effectId > 0) {
            val effectName = gameDatabase?.effect(effectId)?.name ?: ""
            AshValue.effect(effectName)
        } else AshValue.effect("")
    }

    val intArrayType = AggregateType(AshType.INT, AshType.ITEM)
    regFn(scope, "candy_for_tier", intArrayType,
        listOf("tier" to AshType.INT)) { _, args ->
        val tier = args[0].toLong().toInt()
        val result = AggregateValue(intArrayType)
        val candies = CandyDatabase.candyForTier(tier)
        candies.forEachIndexed { i, itemId ->
            val name = gameDatabase?.item(itemId)?.name ?: return@forEachIndexed
            result[AshValue.of(i)] = AshValue.item(name)
        }
        result
    }

    // ── Phase 929–934: Skill cost functions ─────────────────────────
    regFn(scope, "adv_cost", AshType.INT, listOf("skill" to AshType.SKILL)) { _, args ->
        val skillId = resolveSkillId(args[0].toString())
        AshValue.of(SkillCosts.getAdventureCost(skillId).toLong())
    }

    regFn(scope, "soulsauce_cost", AshType.INT, listOf("skill" to AshType.SKILL)) { _, args ->
        val skillId = resolveSkillId(args[0].toString())
        AshValue.of(SkillCosts.getSoulsauceCost(skillId).toLong())
    }

    regFn(scope, "thunder_cost", AshType.INT, listOf("skill" to AshType.SKILL)) { _, args ->
        val skillId = resolveSkillId(args[0].toString())
        AshValue.of(SkillCosts.getThunderCost(skillId).toLong())
    }

    regFn(scope, "rain_cost", AshType.INT, listOf("skill" to AshType.SKILL)) { _, args ->
        val skillId = resolveSkillId(args[0].toString())
        AshValue.of(SkillCosts.getRainCost(skillId).toLong())
    }

    regFn(scope, "lightning_cost", AshType.INT, listOf("skill" to AshType.SKILL)) { _, args ->
        val skillId = resolveSkillId(args[0].toString())
        AshValue.of(SkillCosts.getLightningCost(skillId).toLong())
    }

    regFn(scope, "fuel_cost", AshType.INT, listOf("skill" to AshType.SKILL)) { _, args ->
        val skillId = resolveSkillId(args[0].toString())
        AshValue.of(SkillCosts.getFuelCost(skillId).toLong())
    }

    regFn(scope, "hp_cost", AshType.INT, listOf("skill" to AshType.SKILL)) { _, args ->
        val skillId = resolveSkillId(args[0].toString())
        AshValue.of(SkillCosts.getHPCost(skillId).toLong())
    }

    regFn(scope, "meat_cost", AshType.INT, listOf("skill" to AshType.SKILL)) { _, args ->
        val skillId = resolveSkillId(args[0].toString())
        AshValue.of(SkillCosts.getMeatCost(skillId).toLong())
    }

    regFn(scope, "turns_per_cast", AshType.INT, listOf("skill" to AshType.SKILL)) { _, args ->
        val name = args[0].toString()
        val def = SkillDefinitionDatabase.getByName(name)
        AshValue.of((def?.duration ?: 0).toLong())
    }
}

private fun GameRuntimeLibrary.resolveSkillId(name: String): Int =
    gameDatabase?.skill(name)?.id ?: SkillDefinitionDatabase.getByName(name)?.id ?: 0
