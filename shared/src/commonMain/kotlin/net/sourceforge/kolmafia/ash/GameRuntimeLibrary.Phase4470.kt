package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.adventure.AdventurePrep
import net.sourceforge.kolmafia.adventure.prep.AdventureZoneGates
import net.sourceforge.kolmafia.data.AdventureDatabase
import net.sourceforge.kolmafia.data.KolGameHolidayCalendar
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.request.DeckOfEveryCardRequest
import net.sourceforge.kolmafia.request.MonsterManuelRequest
import net.sourceforge.kolmafia.request.UseItemConsumptionSync
import net.sourceforge.kolmafia.session.ConsumptionHelperState
import net.sourceforge.kolmafia.session.MonsterManuelManager
import net.sourceforge.kolmafia.session.NumberologyManager
import net.sourceforge.kolmafia.utilities.leetify

/**
 * Phases 4461–4470 — ASH behavioral deepen VII (corpus-driven stub / missing-registration closure).
 *
 * 4461 can_walk_from_choice · 4462 reverse_numberology · 4463 monster_factoids_available ·
 * 4464 current_rad_sickness · 4465 dart_skills_to_parts · 4466 every_card_name ·
 * 4467 clear_food/booze_helper · 4468 pre_validate_adventure · 4469 pickpocket (AshP991) ·
 * 4470 leetify / stat_bonus_* / absorbed_monsters map / last_item_message
 */
internal fun GameRuntimeLibrary.registerPhase4470(scope: AshScope) {
    // ── 4461: can_walk_from_choice ────────────────────────────────
    regFn(scope, "can_walk_from_choice", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(adventureManager?.canWalkAwayFromChoice() ?: true)
    }

    // ── 4462: reverse_numberology ─────────────────────────────────
    val numberologyType = AggregateType(AshType.INT, AshType.INT)
    fun reverseNumberologyMap(advDelta: Int, spleenDelta: Int): AggregateValue {
        val result = AggregateValue(numberologyType)
        val cs = character?.state?.value ?: return result
        for ((prize, seed) in NumberologyManager.reverseNumberology(cs, advDelta, spleenDelta)) {
            result[AshValue.of(prize.toLong())] = AshValue.of(seed.toLong())
        }
        return result
    }
    regFn(scope, "reverse_numberology", numberologyType, emptyList()) { _, _ ->
        reverseNumberologyMap(0, 0)
    }
    regFn(
        scope,
        "reverse_numberology",
        numberologyType,
        listOf("advDelta" to AshType.INT, "spleenDelta" to AshType.INT),
    ) { _, args ->
        reverseNumberologyMap(args[0].toLong().toInt(), args[1].toLong().toInt())
    }

    // ── 4463: monster_factoids_available ──────────────────────────
    regFn(
        scope,
        "monster_factoids_available",
        AshType.INT,
        listOf("monster" to AshType.MONSTER, "cachedOnly" to AshType.BOOLEAN),
    ) { _, args ->
        val id = MonsterDatabase.getByName(args[0].toString())?.id
            ?: args[0].toLong().toInt()
        if (id == 0) return@regFn AshValue.ZERO
        val cachedOnly = args[1].toBoolean()
        var count = MonsterManuelManager.getFactoidsAvailable(id)
        if (!cachedOnly && count == 0 && id > 0) {
            val client = httpClient
            if (client != null) {
                runBlocking { MonsterManuelRequest(client).fetchMonster(id) }
                count = MonsterManuelManager.getFactoidsAvailable(id)
            }
        }
        AshValue.of(count.toLong())
    }

    // ── 4464: current_rad_sickness ────────────────────────────────
    regFn(scope, "current_rad_sickness", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.radSickness ?: 0).toLong())
    }

    // ── 4465: dart_skills_to_parts ────────────────────────────────
    val skillToString = AggregateType(AshType.SKILL, AshType.STRING)
    regFn(scope, "dart_skills_to_parts", skillToString, emptyList()) { _, _ ->
        val result = AggregateValue(skillToString)
        val board = preferences?.getString("_currentDartboard", "").orEmpty()
        for (dart in board.split(',').map { it.trim() }.filter { it.isNotEmpty() }) {
            val colon = dart.indexOf(':')
            if (colon <= 0) continue
            val skillId = dart.substring(0, colon).toIntOrNull() ?: continue
            val part = dart.substring(colon + 1)
            val skillName = gameDatabase?.skill(skillId)?.name ?: skillId.toString()
            result[AshValue(AshType.SKILL, skillName)] = AshValue.of(part)
        }
        result
    }

    // ── 4466: every_card_name ─────────────────────────────────────
    regFn(scope, "every_card_name", AshType.STRING, listOf("name" to AshType.STRING)) { _, args ->
        AshValue.of(DeckOfEveryCardRequest.everyCardName(args[0].toString()))
    }

    // ── 4467: clear_food_helper / clear_booze_helper ──────────────
    regFn(scope, "clear_food_helper", AshType.VOID, emptyList()) { _, _ ->
        ConsumptionHelperState.clearFoodHelper()
        AshValue.VOID
    }
    regFn(scope, "clear_booze_helper", AshType.VOID, emptyList()) { _, _ ->
        ConsumptionHelperState.clearBoozeHelper()
        AshValue.VOID
    }

    // ── 4468: pre_validate_adventure ──────────────────────────────
    regFn(scope, "pre_validate_adventure", AshType.BOOLEAN, listOf("loc" to AshType.LOCATION)) { _, args ->
        val locationName = args[0].toString()
        if (locationName.isBlank() || locationName.equals("none", ignoreCase = true)) {
            return@regFn AshValue.FALSE
        }
        val zone = AdventureDatabase.getByName(locationName)
        val ctx = AdventurePrep.buildContext(character?.state?.value, preferences)
        AshValue.of(AdventureZoneGates.preValidateAdventure(locationName, zone, ctx))
    }

    // ── 4470: leetify / stat_bonus_* / last_item_message ───────────
    regFn(scope, "leetify", AshType.STRING, listOf("string" to AshType.STRING)) { _, args ->
        AshValue.of(leetify(args[0].toString()))
    }

    regFn(scope, "stat_bonus_today", AshType.STAT, emptyList()) { _, _ ->
        AshValue(AshType.STAT, KolGameHolidayCalendar.getStatDay())
    }
    regFn(scope, "stat_bonus_tomorrow", AshType.STAT, emptyList()) { _, _ ->
        val tomorrow = KolGameHolidayCalendar.dayInKoLYear(kolRolloverDayDifference() + 1)
        AshValue(AshType.STAT, KolGameHolidayCalendar.getStatDay(tomorrow))
    }

    regFn(scope, "last_item_message", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(UseItemConsumptionSync.lastUpdate)
    }
}
