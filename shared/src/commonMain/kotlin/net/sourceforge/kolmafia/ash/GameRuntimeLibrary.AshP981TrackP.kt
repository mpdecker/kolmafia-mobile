package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.effect.EffectManager

/**
 * AshP981–984 Track P — Restriction / removability residuals.
 *
 * Phase 981: is_unrestricted(item), is_unrestricted(skill), is_unrestricted(familiar),
 *            is_unrestricted(string)
 * Phase 982: is_trendy, is_thrifty
 * Phase 983: is_removable(effect), is_shruggable(effect)
 * Phase 984: is_goal(item)
 */
internal fun GameRuntimeLibrary.registerAshP981TrackPBatch(scope: AshScope) {
    // ── Phase 981: is_unrestricted ──────────────────────────────────
    regFn(scope, "is_unrestricted", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, args ->
        val name = args[0].toString()
        val db = gameDatabase
        val item = db?.item(name)
        AshValue.of(item != null)
    }

    regFn(scope, "is_unrestricted", AshType.BOOLEAN,
        listOf("sk" to AshType.SKILL)) { _, args ->
        val name = args[0].toString()
        val skill = SkillDefinitionDatabase.getByName(name)
        AshValue.of(skill != null)
    }

    regFn(scope, "is_unrestricted", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        val race = args[0].toString()
        val db = gameDatabase
        val familiar = db?.familiar(race)
        AshValue.of(familiar != null)
    }

    regFn(scope, "is_unrestricted", AshType.BOOLEAN,
        listOf("key" to AshType.STRING)) { _, _ ->
        AshValue.TRUE
    }

    // ── Phase 982: is_trendy / is_thrifty ──────────────────────────
    regFn(scope, "is_trendy", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, _ ->
        AshValue.TRUE
    }

    regFn(scope, "is_thrifty", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, _ ->
        AshValue.TRUE
    }

    // ── Phase 983: is_removable / is_shruggable ─────────────────────
    regFn(scope, "is_removable", AshType.BOOLEAN,
        listOf("eff" to AshType.EFFECT)) { _, args ->
        val name = args[0].toString()
        val em = effectManager ?: return@regFn AshValue.TRUE
        val effects = em.state.value.effects
        val active = effects.firstOrNull { it.name.equals(name, ignoreCase = true) }
        AshValue.of(active == null || !isIntrinsicEffect(name))
    }

    regFn(scope, "is_shruggable", AshType.BOOLEAN,
        listOf("eff" to AshType.EFFECT)) { _, args ->
        val name = args[0].toString()
        AshValue.of(!isIntrinsicEffect(name))
    }

    // ── Phase 984: is_goal ─────────────────────────────────────────
    regFn(scope, "is_goal", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, args ->
        val name = args[0].toString()
        val goals = preferences?.getString("adventureGoals", "")?.takeIf { it.isNotBlank() }
        AshValue.of(goals != null && goals.contains(name, ignoreCase = true))
    }
}

private fun isIntrinsicEffect(name: String): Boolean {
    val lc = name.lowercase()
    return lc.contains("intrinsic") || lc.contains("form of")
}
