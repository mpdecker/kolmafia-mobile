package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.data.RestrictedItemType
import net.sourceforge.kolmafia.data.SkillDefinitionDatabase
import net.sourceforge.kolmafia.request.StandardRequest
import net.sourceforge.kolmafia.request.ThriftyRequest
import net.sourceforge.kolmafia.request.TrendyRequest
import net.sourceforge.kolmafia.request.MonsterManuelRequest
import net.sourceforge.kolmafia.request.UneffectRequest
import net.sourceforge.kolmafia.request.UneffectSkillEffectMap
import net.sourceforge.kolmafia.session.MonsterManuelManager

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
    val state = { character?.state?.value }
    fun isUnrestricted(type: RestrictedItemType, key: String): Boolean =
        StandardRequest.isNotRestricted(type, key, state())
    fun isTrendy(type: RestrictedItemType, key: String): Boolean =
        TrendyRequest.isTrendy(type, key)
    fun isThrifty(type: RestrictedItemType, key: String): Boolean =
        // Before the first thrifty response, the desktop request is optimistic
        // while it schedules its refresh. Preserve that behavior headlessly.
        if (ThriftyRequest.isInitialized()) ThriftyRequest.isAllowed(type, key) else true

    // ── Phase 981: is_unrestricted ──────────────────────────────────
    regFn(scope, "is_unrestricted", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, args ->
        val name = args[0].toString()
        AshValue.of(isUnrestricted(RestrictedItemType.ITEMS, name))
    }

    regFn(scope, "is_unrestricted", AshType.BOOLEAN,
        listOf("sk" to AshType.SKILL)) { _, args ->
        val name = args[0].toString()
        AshValue.of(isUnrestricted(RestrictedItemType.SKILLS, name))
    }

    regFn(scope, "is_unrestricted", AshType.BOOLEAN,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        val race = args[0].toString()
        AshValue.of(isUnrestricted(RestrictedItemType.FAMILIARS, race))
    }

    regFn(scope, "is_unrestricted", AshType.BOOLEAN,
        listOf("key" to AshType.STRING)) { _, args ->
        val key = args[0].toString()
        AshValue.of(
            isUnrestricted(RestrictedItemType.ITEMS, key) &&
                isUnrestricted(RestrictedItemType.BOOKSHELF_BOOKS, key) &&
                isUnrestricted(RestrictedItemType.SKILLS, key) &&
                isUnrestricted(RestrictedItemType.FAMILIARS, key) &&
                isUnrestricted(RestrictedItemType.CLAN_ITEMS, key),
        )
    }

    // ── Phase 982: is_trendy / is_thrifty ──────────────────────────
    regFn(scope, "is_trendy", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, args ->
        AshValue.of(isTrendy(RestrictedItemType.ITEMS, args[0].toString()))
    }

    regFn(scope, "is_trendy", AshType.BOOLEAN,
        listOf("it" to AshType.FAMILIAR)) { _, args ->
        AshValue.of(isTrendy(RestrictedItemType.FAMILIARS, args[0].toString()))
    }

    regFn(scope, "is_trendy", AshType.BOOLEAN,
        listOf("it" to AshType.SKILL)) { _, args ->
        AshValue.of(isTrendy(RestrictedItemType.SKILLS, args[0].toString()))
    }

    regFn(scope, "is_trendy", AshType.BOOLEAN,
        listOf("key" to AshType.STRING)) { _, args ->
        val key = args[0].toString()
        AshValue.of(
            isTrendy(RestrictedItemType.ITEMS, key) &&
                isTrendy(RestrictedItemType.CAMPGROUND, key) &&
                isTrendy(RestrictedItemType.BOOKSHELF_BOOKS, key) &&
                isTrendy(RestrictedItemType.FAMILIARS, key) &&
                isTrendy(RestrictedItemType.SKILLS, key) &&
                isTrendy(RestrictedItemType.CLAN_ITEMS, key),
        )
    }

    regFn(scope, "is_thrifty", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, args ->
        AshValue.of(isThrifty(RestrictedItemType.ITEMS, args[0].toString()))
    }

    regFn(scope, "is_thrifty", AshType.BOOLEAN,
        listOf("it" to AshType.FAMILIAR)) { _, args ->
        AshValue.of(isThrifty(RestrictedItemType.FAMILIARS, args[0].toString()))
    }

    regFn(scope, "is_thrifty", AshType.BOOLEAN,
        listOf("it" to AshType.SKILL)) { _, args ->
        AshValue.of(isThrifty(RestrictedItemType.SKILLS, args[0].toString()))
    }

    regFn(scope, "is_thrifty", AshType.BOOLEAN,
        listOf("key" to AshType.STRING)) { _, args ->
        val key = args[0].toString()
        AshValue.of(
            isThrifty(RestrictedItemType.ITEMS, key) &&
                isThrifty(RestrictedItemType.FAMILIARS, key) &&
                isThrifty(RestrictedItemType.SKILLS, key),
        )
    }

    // ── Phase 983: is_removable / is_shruggable ─────────────────────
    regFn(scope, "is_removable", AshType.BOOLEAN,
        listOf("eff" to AshType.EFFECT)) { _, args ->
        val name = args[0].toString()
        val effectId = net.sourceforge.kolmafia.data.EffectDatabase.getByName(name)?.id ?: -1
        AshValue.of(UneffectRequest.isRemovable(effectId))
    }

    regFn(scope, "is_shruggable", AshType.BOOLEAN,
        listOf("eff" to AshType.EFFECT)) { _, args ->
        val name = args[0].toString()
        val effectId = net.sourceforge.kolmafia.data.EffectDatabase.getByName(name)?.id ?: -1
        AshValue.of(UneffectRequest.isShruggable(effectId))
    }

    regFn(scope, "to_skill", AshType.SKILL,
        listOf("eff" to AshType.EFFECT)) { _, args ->
        AshValue(
            AshType.SKILL,
            UneffectSkillEffectMap.effectToSkill(args[0].toString()).orEmpty(),
        )
    }

    regFn(scope, "to_effect", AshType.EFFECT,
        listOf("skill" to AshType.SKILL)) { _, args ->
        AshValue(
            AshType.EFFECT,
            UneffectSkillEffectMap.skillToEffect(args[0].toString()).orEmpty(),
        )
    }

    val effectArray = AggregateType(AshType.INT, AshType.EFFECT)
    regFn(scope, "to_effects", effectArray,
        listOf("skill" to AshType.SKILL)) { _, args ->
        val result = AggregateValue(effectArray)
        UneffectSkillEffectMap.skillToEffects(args[0].toString()).forEachIndexed { index, name ->
            result[AshValue.of(index)] = AshValue(AshType.EFFECT, name)
        }
        result
    }

    regFn(scope, "monster_manuel_text", AshType.STRING,
        listOf("monster" to AshType.MONSTER)) { _, args ->
        val id = MonsterDatabase.getByName(args[0].toString())?.id ?: 0
        var text = MonsterManuelManager.getCachedManuelText(id)
        if (text.isEmpty() && id > 0) {
            val client = httpClient
            if (client != null) {
                text = kotlinx.coroutines.runBlocking {
                    MonsterManuelRequest(client).fetchMonster(id).getOrDefault("")
                }
            }
        }
        AshValue.of(text)
    }

    regFn(scope, "flush_monster_manuel_cache", AshType.VOID, emptyList()) { _, _ ->
        MonsterManuelManager.flushCache()
        AshValue.VOID
    }

    // ── Phase 984: is_goal ─────────────────────────────────────────
    regFn(scope, "is_goal", AshType.BOOLEAN,
        listOf("it" to AshType.ITEM)) { _, args ->
        val manager = goalManager
        if (manager == null) return@regFn AshValue.FALSE
        val itemId = resolveAshItemId(args[0])
        if (itemId != null && manager.hasItemGoal(itemId)) {
            return@regFn AshValue.TRUE
        }
        val name = args[0].toString()
        AshValue.of(manager.hasItemGoalByName(name))
    }
}
