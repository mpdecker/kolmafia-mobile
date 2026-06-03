package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.adventure.AdventureLocation
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.skill.SkillManager
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToLong
import kotlin.math.sqrt
import kotlin.random.Random

class GameRuntimeLibrary(
    private val character: KoLCharacter? = null,
    private val inventoryManager: InventoryManager? = null,
    private val skillManager: SkillManager? = null,
    private val effectManager: EffectManager? = null,
    private val adventureManager: AdventureManager? = null
) : RuntimeLibrary() {

    companion object {
        /** Used in tests where no game managers are needed. */
        fun forTesting() = GameRuntimeLibrary()
    }

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
            AshValue.familiar(character?.state?.value?.name ?: "none")
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
            val location = AdventureLocation(locName, locName, "")
            // Fix: call .join() so the script coroutine blocks until all turns complete.
            kotlinx.coroutines.runBlocking {
                manager.runAdventures(location, turns, this).join()
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
            // Minimal: echo the command to output.
            // Full KoLmafia CLI dispatch is out of scope for Phase 5.
            runtime.print("[cli] ${args[0]}")
            AshValue.of(true)
        }
    }
}
