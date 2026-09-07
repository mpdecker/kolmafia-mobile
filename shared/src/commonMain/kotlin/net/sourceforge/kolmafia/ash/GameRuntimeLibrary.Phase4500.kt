package net.sourceforge.kolmafia.ash

import kotlin.math.ln
import kotlin.math.sqrt
import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.campground.ColdMedicineCabinetGuess
import net.sourceforge.kolmafia.combat.MonsterStatusTracker
import net.sourceforge.kolmafia.data.CupOf13sDatabase
import net.sourceforge.kolmafia.data.EffectDatabase
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.data.HeartstoneDatabase
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.platform.UserDataFileIO
import net.sourceforge.kolmafia.request.MonkeyPawRequest

/**
 * Phases 4491–4500 — ASH behavioral deepen IX (missing registration / util closure).
 *
 * 4491 my_effects · 4492 get_title / get_avatar · 4493 monkey_paw ·
 * 4494 square_root / truncate / log_n / last_index_of ·
 * 4495 insert / delete / set_length / append_buffer_to_file ·
 * 4496 append_replacement / append_tail / group_names ·
 * 4497 get_ignore_zone_warnings · 4498 shield_dr ·
 * 4499 heartstone_* / cup_of_13s_tier · 4500 expected_cold_medicine_cabinet
 */
internal fun GameRuntimeLibrary.registerPhase4500(scope: AshScope) {
    // ── 4491: my_effects ──────────────────────────────────────────
    val effectToInt = AggregateType(AshType.EFFECT, AshType.INT)
    regFn(scope, "my_effects", effectToInt, emptyList()) { _, _ ->
        val result = AggregateValue(effectToInt)
        val effects = effectManager?.state?.value?.effects.orEmpty()
        for (effect in effects) {
            val duration = if (effect.duration == Int.MAX_VALUE) -1 else effect.duration
            val name = effect.name.ifBlank {
                EffectDatabase.getById(effect.id)?.name
            } ?: continue
            result[AshValue.effect(name)] = AshValue.of(duration.toLong())
        }
        result
    }

    // ── 4492: get_title / get_avatar ──────────────────────────────
    regFn(scope, "get_title", AshType.STRING, emptyList()) { _, _ ->
        AshValue.of(character?.state?.value?.title.orEmpty())
    }
    val stringArray = AggregateType(AshType.INT, AshType.STRING)
    regFn(scope, "get_avatar", stringArray, emptyList()) { _, _ ->
        val result = AggregateValue(stringArray)
        val avatar = character?.state?.value?.avatar.orEmpty()
        if (avatar.isNotBlank()) {
            result[AshValue.ZERO] = AshValue.of(avatar)
        }
        result
    }

    // ── 4493: monkey_paw ──────────────────────────────────────────
    fun resolveMonkeyWish(arg: AshValue): String? = when (arg.type) {
        AshType.ITEM -> {
            val name = arg.toString()
            val item = ItemDatabase.getByName(name)
                ?: ItemDatabase.getById(arg.toLong().toInt())
                ?: return null
            MonkeyPawRequest.getValidItemSubstring(item.name)
        }
        AshType.EFFECT -> {
            val name = arg.toString()
            val effect = EffectDatabase.getByName(name)
                ?: EffectDatabase.getById(arg.toLong().toInt())
                ?: return null
            MonkeyPawRequest.getValidEffectSubstring(effect.name)
        }
        else -> arg.toString().trim().ifBlank { null }
    }
    fun runMonkeyPaw(wish: String?): AshValue {
        if (wish.isNullOrBlank()) return AshValue.FALSE
        val client = httpClient ?: return AshValue.FALSE
        val choice = choiceRequest ?: return AshValue.FALSE
        val counts: (Int) -> Int = { id ->
            inventoryManager?.state?.value?.items?.get(id)?.quantity ?: 0
        }
        val html = runBlocking {
            MonkeyPawRequest(client, choice, equipmentRequest)
                .makeWish(wish, preferences, character?.state?.value, counts)
                .getOrNull()
        } ?: return AshValue.FALSE
        return AshValue.of(!html.contains("impossible", ignoreCase = true))
    }
    regFn(scope, "monkey_paw", AshType.BOOLEAN, listOf("wish" to AshType.STRING)) { _, args ->
        runMonkeyPaw(resolveMonkeyWish(args[0]))
    }
    regFn(scope, "monkey_paw", AshType.BOOLEAN, listOf("it" to AshType.ITEM)) { _, args ->
        runMonkeyPaw(resolveMonkeyWish(args[0]))
    }
    regFn(scope, "monkey_paw", AshType.BOOLEAN, listOf("ef" to AshType.EFFECT)) { _, args ->
        runMonkeyPaw(resolveMonkeyWish(args[0]))
    }

    // ── 4494: math / string ───────────────────────────────────────
    regFn(scope, "truncate", AshType.INT, listOf("val" to AshType.FLOAT)) { _, args ->
        AshValue.of(args[0].toDouble().toLong())
    }
    regFn(scope, "square_root", AshType.FLOAT, listOf("val" to AshType.FLOAT)) { _, args ->
        val value = args[0].toDouble()
        if (value < 0.0) throw ScriptException("Can't take square root of a negative value")
        AshValue.of(sqrt(value))
    }
    regFn(scope, "log_n", AshType.FLOAT, listOf("val" to AshType.FLOAT)) { _, args ->
        AshValue.of(ln(args[0].toDouble()))
    }
    regFn(
        scope,
        "log_n",
        AshType.FLOAT,
        listOf("val" to AshType.FLOAT, "base" to AshType.FLOAT),
    ) { _, args ->
        AshValue.of(ln(args[0].toDouble()) / ln(args[1].toDouble()))
    }
    regFn(
        scope,
        "last_index_of",
        AshType.INT,
        listOf("source" to AshType.STRING, "search" to AshType.STRING),
    ) { _, args ->
        AshValue.of(args[0].toString().lastIndexOf(args[1].toString()).toLong())
    }
    regFn(
        scope,
        "last_index_of",
        AshType.INT,
        listOf("source" to AshType.STRING, "search" to AshType.STRING, "start" to AshType.INT),
    ) { _, args ->
        val string = args[0].toString()
        val begin = args[2].toLong().toInt()
        if (begin < 0 || begin > string.length) {
            throw ScriptException("Begin index $begin out of bounds")
        }
        AshValue.of(string.lastIndexOf(args[1].toString(), begin).toLong())
    }

    // ── 4495: buffer mutators + append_buffer_to_file ─────────────
    regFn(
        scope,
        "insert",
        AshType.BUFFER,
        listOf("buf" to AshType.BUFFER, "index" to AshType.INT, "s" to AshType.STRING),
    ) { _, args ->
        val buf = args[0].content as? StringBuilder
            ?: return@regFn args[0]
        val offset = args[1].toLong().toInt()
        if (offset < 0 || offset > buf.length) {
            throw ScriptException("Index $offset out of bounds")
        }
        buf.insert(offset, args[2].toString())
        args[0]
    }
    regFn(
        scope,
        "delete",
        AshType.BUFFER,
        listOf("buf" to AshType.BUFFER, "start" to AshType.INT, "finish" to AshType.INT),
    ) { _, args ->
        val buf = args[0].content as? StringBuilder
            ?: return@regFn args[0]
        val begin = args[1].toLong().toInt()
        val end = args[2].toLong().toInt()
        if (begin < 0) throw ScriptException("Begin index $begin out of bounds")
        if (end > buf.length) throw ScriptException("End index $end out of bounds")
        if (begin > end) {
            throw ScriptException("Begin index $begin greater than end index $end")
        }
        buf.deleteRange(begin, end)
        args[0]
    }
    regFn(
        scope,
        "set_length",
        AshType.VOID,
        listOf("buf" to AshType.BUFFER, "len" to AshType.INT),
    ) { _, args ->
        val buf = args[0].content as? StringBuilder ?: return@regFn AshValue.VOID
        val length = args[1].toLong().toInt()
        if (length < 0) throw ScriptException("Desired length is less than zero")
        buf.setLength(length)
        AshValue.VOID
    }
    regFn(
        scope,
        "append_buffer_to_file",
        AshType.BOOLEAN,
        listOf("data" to AshType.BUFFER, "filename" to AshType.STRING),
    ) { _, args ->
        val data = args[0].toString()
        val filename = args[1].toString()
        try {
            val existing = UserDataFileIO.readText(filename) ?: ""
            UserDataFileIO.writeText(filename, existing + data)
            AshValue.TRUE
        } catch (_: Exception) {
            AshValue.FALSE
        }
    }

    // ── 4496: matcher append / group_names ────────────────────────
    regFn(
        scope,
        "append_tail",
        AshType.BUFFER,
        listOf("m" to AshType.MATCHER, "buf" to AshType.BUFFER),
    ) { _, args ->
        val ms = args[0].content as? AshMatcherState ?: return@regFn args[1]
        val buf = args[1].content as? StringBuilder ?: return@regFn args[1]
        ms.appendTail(buf)
        args[1]
    }
    regFn(
        scope,
        "append_replacement",
        AshType.BUFFER,
        listOf("m" to AshType.MATCHER, "buf" to AshType.BUFFER, "replacement" to AshType.STRING),
    ) { _, args ->
        val ms = args[0].content as? AshMatcherState ?: return@regFn args[1]
        val buf = args[1].content as? StringBuilder ?: return@regFn args[1]
        try {
            ms.appendReplacement(buf, args[2].toString())
        } catch (e: IllegalStateException) {
            throw ScriptException(e.message ?: "No match attempted or previous match failed")
        }
        args[1]
    }
    val stringToBoolean = AggregateType(AshType.STRING, AshType.BOOLEAN)
    regFn(scope, "group_names", stringToBoolean, listOf("m" to AshType.MATCHER)) { _, args ->
        val result = AggregateValue(stringToBoolean)
        val ms = args[0].content as? AshMatcherState ?: return@regFn result
        for (name in ms.namedGroups()) {
            result[AshValue.of(name)] = AshValue.TRUE
        }
        result
    }

    // ── 4497: get_ignore_zone_warnings ────────────────────────────
    regFn(scope, "get_ignore_zone_warnings", AshType.BOOLEAN, emptyList()) { _, _ ->
        val fromChar = character?.state?.value?.ignoreZoneWarnings
        val fromPref = preferences?.getBoolean("ignoreZoneWarnings", false) ?: false
        AshValue.of(fromChar ?: fromPref)
    }

    // ── 4498: shield_dr ───────────────────────────────────────────
    regFn(scope, "shield_dr", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
        val itemId = resolveAshItemId(args[0]) ?: 0
        val level = character?.state?.value?.level ?: 0
        AshValue.of(EquipmentDatabase.getShieldDamageReduction(itemId, level).toLong())
    }

    // ── 4499: heartstone_* / cup_of_13s_tier ──────────────────────
    fun heartstoneLetter(monsterName: String): AshValue {
        val letter = HeartstoneDatabase.middleLetter(monsterName)?.letter.orEmpty()
        return AshValue.of(letter)
    }
    regFn(scope, "heartstone_middle_letter", AshType.STRING, emptyList()) { _, _ ->
        heartstoneLetter(MonsterStatusTracker.getLastMonsterName())
    }
    regFn(
        scope,
        "heartstone_middle_letter",
        AshType.STRING,
        listOf("monster" to AshType.MONSTER),
    ) { _, args ->
        val monster = MonsterDatabase.getByName(args[0].toString())
        val name = monster?.manuelName?.takeIf { it.isNotBlank() }
            ?: monster?.name
            ?: args[0].toString()
        heartstoneLetter(name)
    }
    regFn(
        scope,
        "heartstone_middle_letter",
        AshType.STRING,
        listOf("name" to AshType.STRING),
    ) { _, args ->
        heartstoneLetter(args[0].toString())
    }
    regFn(
        scope,
        "heartstone_string_length",
        AshType.INT,
        listOf("s" to AshType.STRING),
    ) { _, args ->
        AshValue.of(HeartstoneDatabase.spaceStrippedStringLength(args[0].toString()).toLong())
    }
    regFn(scope, "cup_of_13s_tier", AshType.INT, listOf("it" to AshType.ITEM)) { _, args ->
        AshValue.of(CupOf13sDatabase.getTier(resolveAshItemId(args[0]) ?: 0).toLong())
    }

    // ── 4500: expected_cold_medicine_cabinet ──────────────────────
    val stringToItem = AggregateType(AshType.STRING, AshType.ITEM)
    regFn(scope, "expected_cold_medicine_cabinet", stringToItem, emptyList()) { _, _ ->
        val result = AggregateValue(stringToItem)
        val cabinet = ColdMedicineCabinetGuess.guessCabinet(
            preferences,
            character?.state?.value,
        )
        for (type in ColdMedicineCabinetGuess.ITEM_TYPES) {
            val itemId = cabinet[type]
            val name = ColdMedicineCabinetGuess.itemName(itemId)
            result[AshValue.of(type)] = if (name.isNotBlank()) {
                AshValue.item(name)
            } else {
                AshValue.item("none")
            }
        }
        result
    }
}
