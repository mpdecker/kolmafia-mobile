package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.combat.CombatActionManager
import net.sourceforge.kolmafia.maximizer.MaximizerContinuation
import net.sourceforge.kolmafia.session.NumberologyManager

/**
 * AshP943–949 Track H — Daily utilities.
 *
 * Phase 943: get_auto_attack / set_auto_attack
 * Phase 944: set_ccs / read_ccs / write_ccs
 * Phase 945: eudora / eudora_item
 * Phase 946: batch_open / batch_close
 * Phase 947: desc_to_item / desc_to_effect
 * Phase 948: numberology_prize / florist_available
 * Phase 949: allied_radio(string)
 *
 * Phase 4871–4930 deepen: desktop-shaped batch coalesce
 * (`cmd → prefix → comma-joined params`) + flush via CLI.
 */
internal fun GameRuntimeLibrary.registerAshP943TrackHBatch(scope: AshScope) {
    // ── Phase 943: auto_attack ──────────────────────────────────────
    // XLVI-C: character state + defaultAutoAttack pref account sync
    regFn(scope, "get_auto_attack", AshType.INT, emptyList()) { _, _ ->
        val fromChar = character?.state?.value?.autoAttackAction
        val action = when {
            fromChar != null -> fromChar
            else -> preferences?.getInt("defaultAutoAttack", 0) ?: 0
        }
        AshValue.of(action.toLong())
    }

    regFn(scope, "set_auto_attack", AshType.VOID, listOf("attackValue" to AshType.INT)) { rt, args ->
        val value = args[0].toLong().toInt()
        character?.setAutoAttackAction(value)
        preferences?.setInt("defaultAutoAttack", value)
        dispatchCli("autoattack $value", rt)
        AshValue.VOID
    }

    regFn(scope, "set_auto_attack", AshType.VOID, listOf("attackValue" to AshType.STRING)) { rt, args ->
        val arg = args[0].toString()
        dispatchCli("autoattack $arg", rt)
        // Offline / headless: still sync character + pref like account.php autoattack
        val resolved = LongTailCli.resolveAutoAttack(arg) { raw ->
            raw.toIntOrNull()
                ?: skillManager?.state?.value?.skills?.firstOrNull {
                    it.name.equals(raw, ignoreCase = true)
                }?.id
                ?: net.sourceforge.kolmafia.data.SkillDefinitionDatabase.getByName(raw)?.id
        }
        if (resolved != null) {
            character?.setAutoAttackAction(resolved)
            preferences?.setInt("defaultAutoAttack", resolved)
        }
        AshValue.VOID
    }

    // ── Phase 944: CCS (Combat Command Script) ─────────────────────
    // XLIV Track A: CcsFileManager-style lookup + active reload on write
    regFn(scope, "set_ccs", AshType.BOOLEAN, listOf("name" to AshType.STRING)) { _, args ->
        val name = args[0].toString().trim()
        val matched = CombatActionManager.findAvailableLookup(name)
            ?: return@regFn AshValue.FALSE
        AshValue.of(CombatActionManager.loadStrategyLookup(matched, preferences))
    }

    regFn(scope, "read_ccs", AshType.BUFFER, listOf("name" to AshType.STRING)) { _, args ->
        val text = CombatActionManager.readCcs(args[0].toString())
        AshValue(AshType.BUFFER, StringBuilder(text))
    }

    regFn(scope, "write_ccs", AshType.BOOLEAN,
        listOf("data" to AshType.BUFFER, "name" to AshType.STRING)) { _, args ->
        AshValue.of(
            CombatActionManager.writeCcs(args[1].toString(), args[0].toString(), preferences),
        )
    }

    // ── Phase 945: eudora / eudora_item ────────────────────────────
    regFn(scope, "eudora", AshType.STRING, emptyList()) { _, _ ->
        val current = preferences?.getString("currentEudora", "").orEmpty()
            .ifBlank { preferences?.getString("eudora", "").orEmpty() }
        val name = LongTailCli.Correspondent.findByName(current).name
        AshValue.of(if (name == "Pen Pal") "Penpal" else name)
    }

    regFn(scope, "eudora", AshType.BOOLEAN, listOf("newEudora" to AshType.STRING)) { rt, args ->
        val arg = args[0].toString()
        val target = LongTailCli.Correspondent.find(arg) ?: return@regFn AshValue.FALSE
        val before = preferences?.getString("eudora", "").orEmpty()
            .ifBlank { preferences?.getString("currentEudora", "").orEmpty() }
        dispatchCli("eudora $arg", rt)
        val after = preferences?.getString("eudora", "").orEmpty()
            .ifBlank { preferences?.getString("currentEudora", "").orEmpty() }
        AshValue.of(
            after.equals(target.name, ignoreCase = true) ||
                (before.isNotBlank() && after != before),
        )
    }

    regFn(scope, "eudora_item", AshType.ITEM, emptyList()) { _, _ ->
        val current = preferences?.getString("currentEudora", "").orEmpty()
            .ifBlank { preferences?.getString("eudora", "").orEmpty() }
        val correspondent = LongTailCli.Correspondent.findByName(current)
        val itemName = when (correspondent.id) {
            1 -> "envelope from your pen pal"
            2 -> "GameInformPowerDailyPro subscription card"
            3 -> "Xi Receiver Unit"
            4 -> "New-You Club Membership Form"
            5 -> "Our Daily Candles™ order form"
            6 -> "Black & White Apron Enrollment Form"
            else -> ""
        }
        AshValue.item(itemName)
    }

    // ── Phase 946: batch_open / batch_close ────────────────────────
    regFn(scope, "batch_open", AshType.VOID, emptyList()) { rt, _ ->
        batchedCommands.getOrPut(rt) { LinkedHashMap() }
        AshValue.VOID
    }

    regFn(scope, "batch_close", AshType.BOOLEAN, emptyList()) { rt, _ ->
        val batched = batchedCommands.remove(rt) ?: return@regFn AshValue.TRUE
        for ((cmd, prefixes) in batched) {
            if (!MaximizerContinuation.permitsContinue()) break
            for ((prefix, buf) in prefixes) {
                if (!MaximizerContinuation.permitsContinue()) break
                val params = buf.toString()
                val rest = if (prefix.isEmpty()) params else "$prefix $params"
                dispatchCli("$cmd $rest".trim(), rt)
            }
        }
        AshValue.TRUE
    }

    // ── Phase 947: desc_to_item / desc_to_effect ───────────────────
    regFn(scope, "desc_to_item", AshType.ITEM, listOf("value" to AshType.STRING)) { _, args ->
        val descId = args[0].toString()
        val item = ItemDatabase.getByDescId(descId)
        AshValue.item(item?.name ?: "")
    }

    regFn(scope, "desc_to_effect", AshType.EFFECT, listOf("value" to AshType.STRING)) { _, args ->
        val descId = args[0].toString()
        val effect = EffectDatabase.getByDescId(descId)
        AshValue.effect(effect?.name ?: "")
    }

    // ── Phase 948: numberology_prize / florist_available ────────────
    regFn(scope, "numberology_prize", AshType.STRING, listOf("num" to AshType.INT)) { _, args ->
        val num = args[0].toLong().toInt()
        AshValue.of(NumberologyManager.numberologyPrize(num))
    }

    regFn(scope, "florist_available", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(net.sourceforge.kolmafia.request.FloristRequest.haveFlorist(preferences))
    }

    // ── Phase 949: allied_radio ────────────────────────────────────
    regFn(scope, "allied_radio", AshType.BOOLEAN, listOf("request" to AshType.STRING)) { rt, args ->
        val request = args[0].toString()
        dispatchCli("alliedradio $request", rt)
        AshValue.TRUE
    }
}

/** Desktop ScriptRuntime.getBatched(): cmd → prefix → comma-joined params. */
internal typealias BatchedPrefixMap = LinkedHashMap<String, StringBuilder>
internal typealias BatchedCommandMap = LinkedHashMap<String, BatchedPrefixMap>

@Suppress("ObjectPropertyName")
internal val batchedCommands = mutableMapOf<AshRuntimeContext, BatchedCommandMap>()

internal fun GameRuntimeLibrary.isBatching(rt: AshRuntimeContext): Boolean =
    batchedCommands.containsKey(rt)

/**
 * Desktop RuntimeLibrary.batchCommand — coalesce under [cmd]+[prefix] while batching,
 * otherwise execute immediately as `cmd [prefix] params`.
 */
internal fun GameRuntimeLibrary.batchCommand(
    rt: AshRuntimeContext,
    cmd: String,
    prefix: String?,
    params: String,
) {
    val batched = batchedCommands[rt]
    if (batched == null) {
        val rest = if (prefix.isNullOrEmpty()) params else "$prefix $params"
        dispatchCli("$cmd $rest".trim(), rt)
        return
    }
    val prefixMap = batched.getOrPut(cmd) { LinkedHashMap() }
    val key = prefix ?: ""
    val existing = prefixMap[key]
    if (existing == null) {
        prefixMap[key] = StringBuilder(params)
    } else {
        existing.append(", ").append(params)
    }
}

/** Desktop ItemFinder pilcrow form: `count ¶itemId`. */
internal fun pilcrowItemParams(count: Int, itemId: Int): String = "$count \u00B6$itemId"

internal fun pilcrowItemParamsAll(itemId: Int): String = "* \u00B6$itemId"
