package net.sourceforge.kolmafia.ash

import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.platform.UserDataFileIO
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
 */
internal fun GameRuntimeLibrary.registerAshP943TrackHBatch(scope: AshScope) {
    // ── Phase 943: auto_attack ──────────────────────────────────────
    regFn(scope, "get_auto_attack", AshType.INT, emptyList()) { _, _ ->
        val action = character?.state?.value?.autoAttackAction ?: 0
        AshValue.of(action.toLong())
    }

    regFn(scope, "set_auto_attack", AshType.VOID, listOf("attackValue" to AshType.INT)) { rt, args ->
        val value = args[0].toLong().toInt()
        character?.setAutoAttackAction(value)
        AshValue.VOID
    }

    regFn(scope, "set_auto_attack", AshType.VOID, listOf("attackValue" to AshType.STRING)) { rt, args ->
        val arg = args[0].toString()
        dispatchCli("autoattack $arg", rt)
        AshValue.VOID
    }

    // ── Phase 944: CCS (Combat Command Script) ─────────────────────
    regFn(scope, "set_ccs", AshType.BOOLEAN, listOf("name" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        val path = "ccs/$name.ccs"
        val text = UserDataFileIO.readText(path)
        AshValue.of(text != null)
    }

    regFn(scope, "read_ccs", AshType.BUFFER, listOf("name" to AshType.STRING)) { _, args ->
        val name = args[0].toString()
        val path = "ccs/$name.ccs"
        val text = UserDataFileIO.readText(path) ?: ""
        AshValue(AshType.BUFFER, StringBuilder(text))
    }

    regFn(scope, "write_ccs", AshType.BOOLEAN,
        listOf("data" to AshType.BUFFER, "name" to AshType.STRING)) { _, args ->
        val data = args[0].toString()
        val name = args[1].toString()
        val path = "ccs/$name.ccs"
        try {
            UserDataFileIO.writeText(path, data)
            AshValue.TRUE
        } catch (_: Exception) {
            AshValue.FALSE
        }
    }

    // ── Phase 945: eudora / eudora_item ────────────────────────────
    regFn(scope, "eudora", AshType.STRING, emptyList()) { _, _ ->
        val current = preferences?.getString("currentEudora", "").orEmpty()
        val name = LongTailCli.Correspondent.findByName(current).name
        AshValue.of(if (name == "Pen Pal") "Penpal" else name)
    }

    regFn(scope, "eudora", AshType.BOOLEAN, listOf("newEudora" to AshType.STRING)) { rt, args ->
        val arg = args[0].toString()
        dispatchCli("eudora $arg", rt)
        AshValue.TRUE
    }

    regFn(scope, "eudora_item", AshType.ITEM, emptyList()) { _, _ ->
        val current = preferences?.getString("currentEudora", "").orEmpty()
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
        batchedCommands.getOrPut(rt) { mutableListOf() }
        AshValue.VOID
    }

    regFn(scope, "batch_close", AshType.BOOLEAN, emptyList()) { rt, _ ->
        val commands = batchedCommands.remove(rt)
        if (commands != null) {
            for (cmd in commands) {
                dispatchCli(cmd, rt)
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
        val checked = preferences?.getBoolean("floristFriarChecked") ?: false
        val have = preferences?.getBoolean("_floristPlantsAvailable") ?: false
        AshValue.of(checked && have)
    }

    // ── Phase 949: allied_radio ────────────────────────────────────
    regFn(scope, "allied_radio", AshType.BOOLEAN, listOf("request" to AshType.STRING)) { rt, args ->
        val request = args[0].toString()
        dispatchCli("alliedradio $request", rt)
        AshValue.TRUE
    }
}

@Suppress("ObjectPropertyName")
private val batchedCommands = mutableMapOf<AshRuntimeContext, MutableList<String>>()
