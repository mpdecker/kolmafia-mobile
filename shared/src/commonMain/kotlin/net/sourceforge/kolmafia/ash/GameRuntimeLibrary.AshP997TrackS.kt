package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import kotlin.math.roundToLong
import kotlin.math.floor
import kotlin.math.pow
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.request.PingRequest
import net.sourceforge.kolmafia.session.DadManager
import net.sourceforge.kolmafia.session.HeistManager
import net.sourceforge.kolmafia.session.PingManager
import net.sourceforge.kolmafia.session.UnusualConstructManager
import net.sourceforge.kolmafia.session.VotingBoothManager

/**
 * AshP997–1003 Track S — Character niche residuals.
 *
 * Phase 997: my_maxfury, my_ram, my_wildfire_water
 * Phase 998: minstrel_* (pref-backed)
 * Phase 999: heist(int), dart_parts_to_skills
 * Phase 1000: beret_bonus, mobius_bonus
 * Phase 1001: sausage_bonus, autumnaton_*
 * Phase 1002: locket_monster_map, florist_plants
 * Phase 1003: voting_booth_initiatives
 */
internal fun GameRuntimeLibrary.registerAshP997TrackSBatch(scope: AshScope) {
    // ── Phase 997: my_maxfury / my_ram / my_wildfire_water ───────────
    regFn(scope, "my_maxfury", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((character?.state?.value?.furyLimit ?: 0).toLong())
    }

    regFn(scope, "my_ram", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("_ramDrinks", 0) ?: 0).toLong())
    }

    regFn(scope, "my_wildfire_water", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("wildfireWater", 0) ?: 0).toLong())
    }

    // ── Phase 998: minstrel pref-backed ─────────────────────────────
    regFn(scope, "minstrel_level", AshType.INT, emptyList()) { _, _ ->
        AshValue.of((preferences?.getInt("clancyLevel", 1) ?: 1).toLong())
    }

    regFn(scope, "minstrel_instrument", AshType.ITEM, emptyList()) { _, _ ->
        val inst = preferences?.getString("clancyInstrument", "").orEmpty()
        AshValue.item(inst)
    }

    regFn(scope, "minstrel_quest", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(preferences?.getBoolean("clancyHasQuest", false) ?: false)
    }

    // ── Phase 999: heist / dart_parts_to_skills ─────────────────────
    val itemArray = AggregateType(AshType.INT, AshType.ITEM)
    val heistType = AggregateType(AshType.MONSTER, itemArray)
    regFn(scope, "heist_targets", heistType, emptyList()) { _, _ ->
        val result = AggregateValue(heistType)
        val client = httpClient ?: return@regFn result
        runBlocking {
            HeistManager(client).getHeistTargets().getOrNull()
                ?.heistables
                ?.forEach { (monster, items) ->
                    val itemValues = AggregateValue(itemArray)
                    items.forEachIndexed { index, item ->
                        itemValues[AshValue.of(index)] = AshValue.item(item.name)
                    }
                    result[AshValue(AshType.MONSTER, monster.name)] = itemValues
                }
        }
        result
    }

    regFn(scope, "heist", AshType.BOOLEAN,
        listOf("item" to AshType.ITEM)) { _, args ->
        val client = httpClient ?: return@regFn AshValue.FALSE
        val itemId = gameDatabase?.item(args[0].toString())?.id ?: return@regFn AshValue.FALSE
        AshValue.of(runBlocking {
            HeistManager(client).heist(1, itemId).isSuccess
        })
    }

    regFn(scope, "heist", AshType.BOOLEAN,
        listOf("num" to AshType.INT, "item" to AshType.ITEM)) { _, args ->
        val client = httpClient ?: return@regFn AshValue.FALSE
        val itemId = gameDatabase?.item(args[1].toString())?.id ?: return@regFn AshValue.FALSE
        val count = args[0].toLong().toInt().coerceAtLeast(1)
        AshValue.of(runBlocking {
            HeistManager(client).heist(count, itemId).isSuccess
        })
    }

    val stringToBoolean = AggregateType(AshType.STRING, AshType.BOOLEAN)
    // Phase 4487: correct dart_parts_to_skills — inverse of dart_skills_to_parts
    // (string→skill from `_currentDartboard`). Legacy dartPerks boolean map removed.
    val stringToSkill = AggregateType(AshType.STRING, AshType.SKILL)
    regFn(scope, "dart_parts_to_skills", stringToSkill, emptyList()) { _, _ ->
        val result = AggregateValue(stringToSkill)
        val board = preferences?.getString("_currentDartboard", "").orEmpty()
        for (dart in board.split(',').map { it.trim() }.filter { it.isNotEmpty() }) {
            val colon = dart.indexOf(':')
            if (colon <= 0) continue
            val skillId = dart.substring(0, colon).toIntOrNull() ?: continue
            val part = dart.substring(colon + 1)
            val skillName = gameDatabase?.skill(skillId)?.name ?: skillId.toString()
            result[AshValue.of(part)] = AshValue.skill(skillName)
        }
        result
    }

    // ── Phase 1000: beret_bonus / mobius_bonus ──────────────────────
    regFn(scope, "beret_bonus", AshType.FLOAT, emptyList()) { _, _ ->
        val power = preferences?.getInt("beretPower",
            preferences?.getInt("_beretBuskingPower", 0) ?: 0) ?: 0
        val capped = minOf(power, 1100) +
            floor(maxOf(0, power - 1100).toDouble().pow(0.8))
        AshValue.of(capped.toDouble())
    }

    regFn(scope, "mobius_bonus", AshType.FLOAT, emptyList()) { _, _ ->
        val encounters = preferences?.getInt("_mobiusStripEncounters", 0) ?: 0
        AshValue.of(mobiusDelay(encounters).toDouble())
    }

    // ── Phase 1001: sausage_bonus / autumnaton prefs ────────────────
    regFn(scope, "sausage_bonus", AshType.FLOAT, emptyList()) { _, _ ->
        val fights = preferences?.getInt("_sausageFights", 0) ?: 0
        val turns = 4 + 3 * fights + maxOf(0, fights - 5).toDouble().pow(3.0)
        val lastGoblin = preferences?.getInt("_lastSausageMonsterTurn", 0) ?: 0
        val played = character?.state?.value?.currentRun ?: 0
        AshValue.of(((played - lastGoblin + 1).toDouble() / (turns + 1.0))
            .coerceAtMost(1.0))
    }

    regFn(scope, "sausage_goblin_chance", AshType.FLOAT, emptyList()) { _, _ ->
        val fights = preferences?.getInt("_sausageFights", 0) ?: 0
        val turns = 4 + 3 * fights + maxOf(0, fights - 5).toDouble().pow(3.0)
        val lastGoblin = preferences?.getInt("_lastSausageMonsterTurn", 0) ?: 0
        val played = character?.state?.value?.currentRun ?: 0
        AshValue.of(
            if (played - lastGoblin >= turns) 1.0
            else ((played - lastGoblin + 1).toDouble() / (turns + 1.0)).coerceAtLeast(0.0),
        )
    }

    regFn(scope, "turns_until_mobius_noncombat_available", AshType.INT, emptyList()) { _, _ ->
        val prefs = preferences
        if (prefs?.getBoolean("_mobiusRingPrimed", false) != true) {
            AshValue.of(Int.MAX_VALUE.toLong())
        } else {
            val encounters = prefs.getInt("_mobiusStripEncounters", 0)
            val anchor = if (encounters == 0) {
                prefs.getInt("_mobiusRingPrimedTurn", 0)
            } else {
                prefs.getInt("_lastMobiusStripTurn", 0)
            }
            AshValue.of(
                (mobiusDelay(encounters) -
                    ((character?.state?.value?.currentRun ?: 0) - anchor)).coerceAtLeast(0).toLong(),
            )
        }
    }

    val stringArray = AggregateType(AshType.INT, AshType.STRING)
    regFn(scope, "autumnaton_locations", stringArray, emptyList()) { _, _ ->
        val result = AggregateValue(stringArray)
        val locs = preferences?.getString("autumnatonLocations", "")?.takeIf { it.isNotBlank() }
        if (locs != null) {
            for ((i, loc) in locs.split("|").filter { it.isNotBlank() }.withIndex()) {
                result[AshValue.of(i)] = AshValue.of(loc)
            }
        }
        result
    }

    val locationArray = AggregateType(AshType.INT, AshType.LOCATION)
    regFn(scope, "get_autumnaton_locations", locationArray, emptyList()) { _, _ ->
        val result = AggregateValue(locationArray)
        preferences?.getString("autumnatonLocations", "")
            ?.split("|")
            ?.filter { it.isNotBlank() }
            ?.forEachIndexed { index, location ->
                val name = gameDatabase?.zone(location)?.locationName ?: location
                result[AshValue.of(index)] = AshValue.location(name)
            }
        result
    }

    // ── Phase 1002: locket monsters / florist plants ────────────────
    val monsterArray = AggregateType(AshType.MONSTER, AshType.BOOLEAN)
    regFn(scope, "get_locket_monsters", monsterArray, emptyList()) { _, _ ->
        locketMonsterMap(preferences)
    }

    val stringToStringArray = AggregateType(AshType.LOCATION, AggregateType(AshType.INT, AshType.STRING))
    regFn(scope, "florist_available", AshType.BOOLEAN, emptyList()) { _, _ ->
        AshValue.of(net.sourceforge.kolmafia.request.FloristRequest.haveFlorist(preferences))
    }

    // ── Phase 3531–3535: IoTM state helpers ─────────────────────────
    regFn(scope, "dad_sea_monkee_weakness", AshType.ELEMENT, listOf("round" to AshType.INT)) { _, args ->
        val element = DadManager.weakness(args[0].toLong().toInt())
        AshValue(AshType.ELEMENT, DadManager.elementToName(element))
    }
    regFn(scope, "unusual_construct_disc", AshType.ITEM, emptyList()) { _, _ ->
        AshValue.item(ItemDatabase.getItemName(UnusualConstructManager.disc()))
    }

    // ── Phase 3536–3545: voting booth initiatives ───────────────────
    val initiativesType = AggregateType(AshType.BOOLEAN, AshType.STRING)
    regFn(scope, "voting_booth_initiatives", initiativesType,
        listOf("clss" to AshType.INT, "path" to AshType.INT,
            "daycount" to AshType.INT)) { _, args ->
        val result = AggregateValue(initiativesType)
        VotingBoothManager.getInitiatives(
            args[0].toLong().toInt(), args[1].toLong().toInt(), args[2].toLong().toInt(),
        )
            .forEach { result[AshValue.of(it.toString())] = AshValue.TRUE }
        result
    }
    regFn(scope, "voting_booth_initiatives", initiativesType,
        listOf("clss" to AshType.CLASS, "path" to AshType.PATH,
            "daycount" to AshType.INT)) { _, args ->
        val result = AggregateValue(initiativesType)
        val clss = ClassEntityFields.resolve(args[0].toString(), "id").toLong().toInt()
        val path = PathEntityFields.resolve(args[1].toString(), "id", preferences).toLong().toInt()
        VotingBoothManager.getInitiatives(clss, path, args[2].toLong().toInt())
            .forEach { result[AshValue.of(it.toString())] = AshValue.TRUE }
        result
    }

    val pingRecord = RecordType(
        "ping_test",
        listOf("page", "count", "low", "high", "total", "bytes", "average", "bps")
            .mapIndexed { index, name ->
                RecordField(name, if (index == 0) AshType.STRING else AshType.INT, index)
            },
    )
    fun toPingRecord(result: PingManager.PingTest): RecordValue {
        val record = RecordValue(pingRecord)
        record.setField("page", AshValue.of(result.page))
        record.setField("count", AshValue.of(result.count))
        record.setField("low", AshValue.of(result.low))
        record.setField("high", AshValue.of(result.high))
        record.setField("total", AshValue.of(result.total))
        record.setField("bytes", AshValue.of(result.bytes))
        record.setField("average", AshValue.of(result.average.roundToLong()))
        record.setField("bps", AshValue.of(result.bps.roundToLong()))
        return record
    }
    regFn(scope, "ping", pingRecord, emptyList()) { _, _ ->
        val page = preferences?.getString("pingDefaultTestPage", "api") ?: "api"
        val count = preferences?.getInt("pingDefaultTestPings", 10) ?: 10
        val result = httpClient?.let {
            runBlocking { PingManager.runPingTest(it, count, page, preferences) }
        } ?: PingManager.PingTest(PingRequest.normalizePage(page))
        toPingRecord(result)
    }
    regFn(scope, "ping", pingRecord, listOf("pingTest" to AshType.STRING)) { _, args ->
        val result = PingManager.parse(args[0].toString())
        toPingRecord(result)
    }
    regFn(scope, "ping", pingRecord, listOf("count" to AshType.INT, "page" to AshType.STRING)) { _, args ->
        val result = httpClient?.let {
            runBlocking {
                PingManager.runPingTest(
                    it, args[0].toLong().toInt(), args[1].toString(), preferences,
                )
            }
        } ?: PingManager.PingTest(PingRequest.normalizePage(args[1].toString()))
        toPingRecord(result)
    }
}

private fun mobiusDelay(encounters: Int): Int = when (encounters) {
    0 -> 4
    1 -> 7
    2 -> 13
    3 -> 19
    4 -> 25
    5 -> 31
    in 6..10 -> 41
    in 11..15 -> 51
    else -> 76
}

/** Desktop PingCommand — execute a headless ping test without UI dependencies. */
internal fun GameRuntimeLibrary.runPingCli(parameters: String, print: (String) -> Unit) {
    val tokens = parameters.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    var count = preferences?.getInt("pingDefaultTestPings", 4) ?: 4
    var page = preferences?.getString("pingDefaultTestPage", "api") ?: "api"
    var verbose = false
    if (tokens.isNotEmpty()) {
        count = tokens[0].toIntOrNull() ?: run {
            print("Usage: ping [count [(api|status|events|council|main) [verbose]]]")
            return
        }
    }
    if (tokens.size > 1) {
        page = PingRequest.normalizePage(tokens[1])
        if (page !in PingRequest.VALID_PAGES) {
            print("'${tokens[1]}' is not a valid page to ping.")
            return
        }
    }
    if (tokens.size > 2) verbose = tokens[2].equals("verbose", true) || tokens[2] == "true"
    val result = httpClient?.let {
        runBlocking { PingManager.runPingTest(it, count, page, preferences, checkTriggers = false) }
    } ?: PingManager.PingTest(PingRequest.normalizePage(page))
    print(
        "${result.count} pings to ${result.page} at ${result.low}-${result.high} msec apiece " +
            "(total = ${result.total}, average = ${"%.2f".format(result.average)}) = " +
            "${result.bps.roundToLong()} bytes/second",
    )
    if (verbose) print("Ping test completed${if (result.trigger != null) " with abort trigger" else ""}.")
}
