package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.KolGameHolidayCalendar
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase

/**
 * AshP1004–1010 Track T — Date / time / misc residuals.
 *
 * Phase 1004: now_to_int, gametime_to_int
 * Phase 1005: moon_light, moon_phase
 * Phase 1006: format_date_time, date helpers
 * Phase 1007: current_maximizer_score
 * Phase 1008: council, tavern
 * Phase 1009: receive_fax, refresh_stash
 * Phase 1010: familiar_equipment, favorite_familiars
 */
internal fun GameRuntimeLibrary.registerAshP1004TrackTBatch(scope: AshScope) {
    // ── Phase 1004: now_to_int / gametime_to_int ────────────────────
    regFn(scope, "now_to_int", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(currentTimeMillis())
    }

    regFn(scope, "gametime_to_int", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(kolTimeInKoLDayMillis().toLong())
    }

    // ── Phase 1005: moon_light / moon_phase ─────────────────────────
    regFn(scope, "moon_light", AshType.INT, emptyList()) { _, _ ->
        val ronald = KolGameHolidayCalendar.ronaldPhaseIndex()
        val grimace = KolGameHolidayCalendar.grimacePhaseIndex()
        val light = moonLight(ronald, grimace)
        AshValue.of(light.toLong())
    }

    regFn(scope, "moon_phase", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(KolGameHolidayCalendar.ronaldPhaseIndex().toLong())
    }

    regFn(scope, "ronald_phase", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(KolGameHolidayCalendar.ronaldPhaseIndex().toLong())
    }

    regFn(scope, "grimace_phase", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(KolGameHolidayCalendar.grimacePhaseIndex().toLong())
    }

    regFn(scope, "hamburglaris_phase", AshType.INT, emptyList()) { _, _ ->
        AshValue.of(KolGameHolidayCalendar.miniMoonPosition().toLong())
    }

    // ── Phase 1006: format_date_time / date helpers ─────────────────
    regFn(scope, "format_date_time", AshType.STRING,
        listOf("format" to AshType.STRING, "dateTimeMillis" to AshType.INT,
            "timeZone" to AshType.STRING)) { _, args ->
        val millis = args[1].toLong()
        val tz = args[2].toString().takeIf { it.isNotBlank() }
        AshValue.of(formatAshDateTime(args[0].toString(), millis, tz))
    }

    regFn(scope, "format_date_time", AshType.STRING,
        listOf("format" to AshType.STRING)) { _, args ->
        AshValue.of(formatAshDateTime(args[0].toString(), currentTimeMillis(), null))
    }

    regFn(scope, "date_to_timestamp", AshType.INT,
        listOf("inFormat" to AshType.STRING, "dateString" to AshType.STRING)) { _, args ->
        AshValue.of(parseAshDateTimestamp(args[0].toString(), args[1].toString()))
    }

    regFn(scope, "timestamp_to_date", AshType.STRING,
        listOf("timestamp" to AshType.INT, "outFormat" to AshType.STRING)) { _, args ->
        val ts = args[0].toLong()
        AshValue.of(formatAshTimestamp(ts, args[1].toString()))
    }

    regFn(scope, "today_to_string", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(formatAshDateTime("yyyyMMdd", currentTimeMillis(), null))
    }

    regFn(scope, "time_to_string", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(formatAshDateTime("HH:mm:ss", currentTimeMillis(), null))
    }

    // ── Phase 1007: current_maximizer_score ─────────────────────────
    regFn(scope, "current_maximizer_score", AshType.FLOAT, emptyList()) { _, _ ->
        val goal = preferences?.getString("maximizerList", "")?.takeIf { it.isNotBlank() }
            ?: maximizerManager?.lastMaximizeGoal?.takeIf { it.isNotBlank() }
            ?: ""
        AshValue.of(
            if (goal.isBlank()) {
                0.0
            } else {
                net.sourceforge.kolmafia.maximizer.Evaluator(goal)
                    .getScore(buildCurrentModifiers())
            },
        )
    }
    regFn(scope, "current_maximizer_score", AshType.FLOAT,
        listOf("evaluationString" to AshType.STRING)) { _, args ->
        AshValue.of(net.sourceforge.kolmafia.maximizer.Evaluator(args[0].toString())
            .getScore(buildCurrentModifiers()))
    }
    regFn(scope, "last_maximizer_succeeded", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(maximizerManager?.lastMaximizeSucceeded() ?: false)
    }

    // ── Phase 1008: council / tavern ────────────────────────────────
    regFn(scope, "council", AshType.VOID, emptyList()) { rt, _ ->
        dispatchCli("council", rt)
        AshValue.VOID
    }

    regFn(scope, "tavern", AshType.INT, emptyList()) { _, _ ->
        val layout = preferences?.getString("tavernLayout", "")?.takeIf { it.isNotBlank() }
        val target = layout?.indexOf('3') ?: -1
        AshValue.of((if (target >= 0) target + 1 else 0).toLong())
    }

    regFn(scope, "tavern", AshType.INT, listOf("goal" to AshType.STRING)) { _, args ->
        val goal = args[0].toString()
        val layout = preferences?.getString("tavernLayout", "")?.takeIf { it.isNotBlank() }
        val targetChar = when {
            goal.equals("baron", ignoreCase = true) -> '4'
            goal.equals("explore", ignoreCase = true) -> '0'
            goal.equals("faucet", ignoreCase = true) -> '3'
            else -> '3'
        }
        val target = layout?.indexOf(targetChar) ?: -1
        AshValue.of((if (target >= 0) target + 1 else 0).toLong())
    }

    // ── Phase 1009: receive_fax / refresh_stash ─────────────────────
    regFn(scope, "receive_fax", AshType.BOOLEAN, emptyList()) { rt, _ ->
        val manager = faxBotManager
        if (manager != null) {
            val ok = runBlocking { manager.receiveFaxOnly().isSuccess }
            return@regFn AshValue.of(ok)
        }
        dispatchCli("fax receive", rt)
        AshValue.FALSE
    }

    regFn(scope, "refresh_stash", AshType.BOOLEAN, emptyList()) { _, _ ->
        val req = clanStashRequest ?: return@regFn AshValue.FALSE
        val ok = runBlocking {
            try {
                req.fetchContents()
                refreshStashCacheAfter(Result.success(Unit))
                true
            } catch (_: Exception) {
                false
            }
        }
        AshValue.of(ok)
    }

    // ── Phase 1010: familiar_equipment / favorite_familiars ─────────
    regFn(scope, "familiar_equipment", AshType.ITEM,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        val race = args[0].toString()
        val defaultItem = gameDatabase?.familiar(race)?.familiarItem
            ?: FamiliarDefinitionDatabase.getByName(race)?.familiarItem
        AshValue.item(defaultItem.orEmpty())
    }

    regFn(scope, "familiar_equipped_equipment", AshType.ITEM,
        listOf("fam" to AshType.FAMILIAR)) { _, args ->
        val race = args[0].toString()
        val item = familiarManager?.state?.value?.ownedFamiliars
            ?.firstOrNull { it.race.equals(race, ignoreCase = true) }
            ?.equipment?.name
        AshValue.item(item.orEmpty())
    }

    val famToBool = AggregateType(AshType.FAMILIAR, AshType.BOOLEAN)
    regFn(scope, "favorite_familiars", famToBool, emptyList()) { _, _ ->
        val result = AggregateValue(famToBool)
        val owned = familiarManager?.state?.value?.ownedFamiliars.orEmpty()
        for (familiar in owned.filter { it.favorite }) {
            result[AshValue.familiar(familiar.race)] = AshValue.TRUE
        }
        val favs = preferences?.getString("favoriteFamiliars", "")
            ?.takeIf { it.isNotBlank() && owned.isEmpty() }
        if (favs != null) {
            for (race in favs.split("|").filter { it.isNotBlank() }) {
                result[AshValue.familiar(race)] = AshValue.TRUE
            }
        }
        result
    }
}

private fun moonLight(ronald: Int, grimace: Int): Int {
    val MOON_LIGHT = intArrayOf(0, 1, 2, 3, 4, 3, 2, 1)
    return (MOON_LIGHT.getOrElse(ronald) { 0 }) + (MOON_LIGHT.getOrElse(grimace) { 0 })
}
