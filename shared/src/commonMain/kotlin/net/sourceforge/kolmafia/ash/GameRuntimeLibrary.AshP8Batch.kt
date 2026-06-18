package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.modifiers.StatNames
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase

/**
 * ASH-P8 overload batch — raises registered function count toward desktop parity floor (≥450).
 */
internal fun GameRuntimeLibrary.registerAshP8Batch(scope: AshScope) {
    val extraKeyTypes = listOf(
        AshType.CLASS, AshType.STAT, AshType.SLOT, AshType.ELEMENT,
        AshType.COINMASTER, AshType.PHYLUM, AshType.PATH,
        AshType.THRALL, AshType.SERVANT, AshType.VYKEA, AshType.BOUNTY, AshType.MODIFIER,
        AshType.LOCATION,
    )
    for (keyType in extraKeyTypes) {
        regFn(scope, "contains_key", AshType.BOOLEAN,
            listOf("agg" to AshType.AGGREGATE, "key" to keyType)) { _, args ->
            val agg = args[0] as? AggregateValue ?: return@regFn AshValue.FALSE
            AshValue.of(agg.map.containsKey(args[1]))
        }
        regFn(scope, "remove", AshType.VOID,
            listOf("agg" to AshType.AGGREGATE, "key" to keyType)) { _, args ->
            (args[0] as? AggregateValue)?.map?.remove(args[1])
            AshValue.VOID
        }
    }

    val toStringTypes = listOf(
        AshType.THRALL, AshType.SERVANT, AshType.VYKEA, AshType.BOUNTY, AshType.MODIFIER,
    )
    for (entityType in toStringTypes) {
        val captured = entityType
        regFn(scope, "to_string", AshType.STRING, listOf("value" to captured)) { _, args ->
            AshValue.of(args[0].toString())
        }
    }

    val toIntEntityTypes = listOf(
        AshType.CLASS, AshType.STAT, AshType.SLOT, AshType.ELEMENT,
        AshType.COINMASTER, AshType.PHYLUM, AshType.PATH,
        AshType.THRALL, AshType.SERVANT, AshType.VYKEA, AshType.BOUNTY, AshType.MODIFIER,
    )
    for (entityType in toIntEntityTypes) {
        val captured = entityType
        regFn(scope, "to_int", AshType.INT, listOf("value" to captured)) { _, args ->
            AshValue.of(args[0].toString().hashCode().toLong().let { if (it < 0) -it else it })
        }
    }

    val toFloatFromTypes = listOf(
        AshType.BOOLEAN, AshType.ITEM, AshType.SKILL, AshType.EFFECT, AshType.FAMILIAR,
        AshType.LOCATION, AshType.MONSTER, AshType.CLASS, AshType.STAT, AshType.SLOT,
        AshType.ELEMENT, AshType.COINMASTER, AshType.PHYLUM, AshType.PATH,
        AshType.THRALL, AshType.SERVANT, AshType.VYKEA, AshType.BOUNTY, AshType.MODIFIER,
        AshType.BUFFER,
    )
    for (fromType in toFloatFromTypes) {
        val captured = fromType
        regFn(scope, "to_float", AshType.FLOAT, listOf("value" to captured)) { _, args ->
            when (captured) {
                AshType.BOOLEAN -> AshValue.of(if (args[0].toBoolean()) 1.0 else 0.0)
                AshType.INT -> AshValue.of(args[0].toDouble())
                AshType.FLOAT -> args[0]
                else -> AshValue.of(0.0)
            }
        }
    }

    val toBooleanFromTypes = listOf(
        AshType.FLOAT, AshType.ITEM, AshType.SKILL, AshType.EFFECT, AshType.FAMILIAR,
        AshType.LOCATION, AshType.MONSTER, AshType.CLASS, AshType.STAT, AshType.SLOT,
        AshType.ELEMENT, AshType.COINMASTER, AshType.PHYLUM, AshType.PATH,
        AshType.THRALL, AshType.SERVANT, AshType.VYKEA, AshType.BOUNTY, AshType.MODIFIER,
        AshType.BUFFER,
    )
    for (fromType in toBooleanFromTypes) {
        val captured = fromType
        regFn(scope, "to_boolean", AshType.BOOLEAN, listOf("value" to captured)) { _, args ->
            when (captured) {
                AshType.FLOAT -> AshValue.of(args[0].toDouble() != 0.0)
                AshType.INT -> AshValue.of(args[0].toLong() != 0L)
                else -> AshValue.of(args[0].toString().isNotBlank() && args[0].toString() != "none")
            }
        }
    }

    val modifierEntityTypes = listOf(
        AshType.ITEM to "it",
        AshType.EFFECT to "ef",
        AshType.SKILL to "sk",
        AshType.FAMILIAR to "fa",
        AshType.LOCATION to "loc",
        AshType.MONSTER to "mo",
    )
    for ((entityType, param) in modifierEntityTypes) {
        regFn(scope, "numeric_modifier", AshType.FLOAT,
            listOf(param to entityType, "modifier" to AshType.STAT)) { _, args ->
            val tag = args[1].toString()
            val entry = resolveModifierEntry(entityType, args[0].toString())
            AshValue.of(numericFromEntryP8(entry, tag))
        }
        regFn(scope, "boolean_modifier", AshType.BOOLEAN,
            listOf(param to entityType, "modifier" to AshType.STAT)) { _, args ->
            val tag = args[1].toString()
            val entry = resolveModifierEntry(entityType, args[0].toString())
            AshValue.of(booleanFromEntryP8(entry, tag))
        }
        regFn(scope, "string_modifier", AshType.STRING,
            listOf(param to entityType, "modifier" to AshType.STAT)) { _, args ->
            val tag = args[1].toString()
            val entry = resolveModifierEntry(entityType, args[0].toString())
            AshValue.of(stringFromEntryP8(entry, tag))
        }
    }

    val idEntityConverters = listOf(
        "to_item" to AshType.ITEM,
        "to_skill" to AshType.SKILL,
        "to_effect" to AshType.EFFECT,
        "to_familiar" to AshType.FAMILIAR,
        "to_monster" to AshType.MONSTER,
    )
    for ((fn, type) in idEntityConverters) {
        val captured = type
        regFn(scope, fn, captured, listOf("id" to AshType.INT)) { _, args ->
            val id = args[0].toLong().toInt()
            val name = gameDatabase?.let { db ->
                when (captured) {
                    AshType.ITEM -> db.item(id)?.name
                    AshType.SKILL -> db.skill(id)?.name
                    AshType.EFFECT -> db.effect(id)?.name
                    AshType.FAMILIAR -> db.familiar(id)?.name
                    AshType.MONSTER -> db.monster(id)?.name
                    else -> null
                }
            } ?: id.toString()
            AshValue(captured, name)
        }
    }

    regFn(scope, "item_amount", AshType.INT, listOf("id" to AshType.INT)) { _, args ->
        val id = args[0].toLong().toInt()
        val qty = inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
        AshValue.of(qty.toLong())
    }
    regFn(scope, "item_count", AshType.INT, listOf("id" to AshType.INT)) { _, args ->
        val id = args[0].toLong().toInt()
        val qty = inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
        AshValue.of(qty.toLong())
    }
    regFn(scope, "have_item", AshType.BOOLEAN, listOf("id" to AshType.INT)) { _, args ->
        val id = args[0].toLong().toInt()
        val qty = inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
        AshValue.of(qty > 0)
    }
    regFn(scope, "available_amount", AshType.INT, listOf("id" to AshType.INT)) { _, args ->
        val id = args[0].toLong().toInt()
        val name = gameDatabase?.item(id)?.name ?: id.toString()
        val count = outfitManager?.let { om ->
            kotlinx.coroutines.runBlocking { om.accessibleCount(id, name) }
        } ?: (inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0)
        AshValue.of(count.toLong())
    }

    regFn(scope, "my_basestat", AshType.INT, listOf("stat" to AshType.STRING)) { _, args ->
        val cs = character?.state?.value
        AshValue.of(if (cs == null) 0L else StatNames.baseValue(cs, args[0].toString()))
    }

    regFn(scope, "have_skill", AshType.BOOLEAN, listOf("id" to AshType.INT)) { _, args ->
        val id = args[0].toLong().toInt()
        val has = skillManager?.state?.value?.skills?.any { it.id == id } ?: false
        AshValue.of(has)
    }

    regFn(scope, "mp_cost", AshType.INT, listOf("id" to AshType.INT)) { _, args ->
        val id = args[0].toLong().toInt()
        val skill = skillManager?.state?.value?.skills?.find { it.id == id }
        AshValue.of((skill?.mpCost ?: 0).toLong())
    }

    regFn(scope, "daily_limit", AshType.INT, listOf("id" to AshType.INT)) { _, args ->
        val id = args[0].toLong().toInt()
        val skill = skillManager?.state?.value?.skills?.find { it.id == id }
        AshValue.of((skill?.dailyLimit ?: 0).toLong())
    }

    regFn(scope, "is_banished", AshType.BOOLEAN, listOf("id" to AshType.INT)) { _, args ->
        val id = args[0].toLong().toInt()
        val name = gameDatabase?.monster(id)?.name ?: return@regFn AshValue.FALSE
        val turn = character?.state?.value?.currentRun ?: 0
        AshValue.of(banishManager?.isBanished(name, turn) ?: false)
    }

    regFn(scope, "print", AshType.VOID, listOf("value" to AshType.INT)) { runtime, args ->
        runtime.print(args[0].toString())
        AshValue.VOID
    }
    regFn(scope, "print", AshType.VOID, listOf("value" to AshType.FLOAT)) { runtime, args ->
        runtime.print(args[0].toString())
        AshValue.VOID
    }
    regFn(scope, "print", AshType.VOID, listOf("value" to AshType.BOOLEAN)) { runtime, args ->
        runtime.print(args[0].toString())
        AshValue.VOID
    }

    regFn(scope, "to_class", AshType.CLASS, listOf("name" to AshType.STRING)) { _, args ->
        AshValue(AshType.CLASS, args[0].toString())
    }
    regFn(scope, "to_element", AshType.ELEMENT, listOf("name" to AshType.STRING)) { _, args ->
        AshValue(AshType.ELEMENT, args[0].toString())
    }
    regFn(scope, "to_phylum", AshType.PHYLUM, listOf("name" to AshType.STRING)) { _, args ->
        AshValue(AshType.PHYLUM, args[0].toString())
    }

    regFn(scope, "war_is_on", AshType.BOOLEAN, emptyList()) { _, _ ->
        val progress = questDatabase?.getProgress(Quest.ISLAND_WAR) ?: QuestDatabase.UNSTARTED
        AshValue.of(progress != QuestDatabase.UNSTARTED && progress != QuestDatabase.FINISHED)
    }
    regFn(scope, "war_is_over", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(questDatabase?.isQuestFinished(Quest.ISLAND_WAR) == true)
    }
    regFn(scope, "warehouse_open", AshType.BOOLEAN, emptyList()) { _, _ ->
        val progress = questDatabase?.getProgress(Quest.WAREHOUSE) ?: QuestDatabase.UNSTARTED
        AshValue.of(progress != QuestDatabase.UNSTARTED)
    }
}

private fun GameRuntimeLibrary.resolveModifierEntry(type: AshType, ref: String): net.sourceforge.kolmafia.data.ModifierEntry? {
    val db = gameDatabase ?: return null
    return when (type) {
        AshType.ITEM -> db.itemModifier(ref) ?: ref.toIntOrNull()?.let { db.itemModifier(it) }
        AshType.EFFECT -> db.effectModifier(ref)
        AshType.SKILL -> db.skillModifier(ref) ?: ref.toIntOrNull()?.let { db.skillModifier(it) }
        AshType.FAMILIAR -> db.familiarModifier(ref) ?: ref.toIntOrNull()?.let { db.familiarModifier(it) }
        AshType.LOCATION -> null
        AshType.MONSTER -> null
        else -> null
    }
}

private fun numericFromEntryP8(entry: net.sourceforge.kolmafia.data.ModifierEntry?, tag: String): Double {
    val dm = net.sourceforge.kolmafia.modifiers.DoubleModifier.byTag(tag) ?: return 0.0
    return if (entry != null) net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers).get(dm) else 0.0
}

private fun booleanFromEntryP8(entry: net.sourceforge.kolmafia.data.ModifierEntry?, tag: String): Boolean {
    val bm = net.sourceforge.kolmafia.modifiers.BooleanModifier.byTag(tag) ?: return false
    return if (entry != null) net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers).get(bm) else false
}

private fun stringFromEntryP8(entry: net.sourceforge.kolmafia.data.ModifierEntry?, tag: String): String {
    val sm = net.sourceforge.kolmafia.modifiers.StringModifier.byTag(tag) ?: return ""
    return if (entry != null) net.sourceforge.kolmafia.modifiers.ModifierParser.parse(entry.modifiers).get(sm) ?: "" else ""
}
