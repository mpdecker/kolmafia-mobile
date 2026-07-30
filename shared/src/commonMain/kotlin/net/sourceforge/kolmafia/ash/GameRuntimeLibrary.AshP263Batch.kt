package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.buffbot.BuffBotDatabase

/** AshP263 — Buff request PM protocol (`request_buff` ASH + shared buff-request helper). */
internal fun GameRuntimeLibrary.registerAshP263Batch(scope: AshScope) {
    regFn(
        scope,
        "request_buff",
        AshType.BOOLEAN,
        listOf("bot" to AshType.STRING, "skill" to AshType.STRING),
    ) { _, args ->
        requestBuffAsh(args[0].toString(), args[1])
    }

    regFn(
        scope,
        "request_buff",
        AshType.BOOLEAN,
        listOf("bot" to AshType.STRING, "skill" to AshType.INT),
    ) { _, args ->
        requestBuffAsh(args[0].toString(), args[1], turns = null)
    }

    regFn(
        scope,
        "request_buff",
        AshType.BOOLEAN,
        listOf(
            "bot" to AshType.STRING,
            "skill" to AshType.STRING,
            "turns" to AshType.INT,
        ),
    ) { _, args ->
        requestBuffAsh(args[0].toString(), args[1], turns = args[2].toLong().toInt())
    }

    regFn(
        scope,
        "request_buff",
        AshType.BOOLEAN,
        listOf(
            "bot" to AshType.STRING,
            "skill" to AshType.INT,
            "turns" to AshType.INT,
        ),
    ) { _, args ->
        requestBuffAsh(args[0].toString(), args[1], turns = args[2].toLong().toInt())
    }
}

internal fun GameRuntimeLibrary.requestBuffAsh(
    bot: String,
    skillArg: AshValue,
    turns: Int? = null,
): AshValue {
    val buffId = resolveBuffSkillId(skillArg) ?: return AshValue.FALSE
    val db = buffBotDatabase ?: BuffBotDatabase.instance
    val cost = db.find(buffId) ?: return AshValue.FALSE
    val turnCount = turns ?: cost.turns
    return runBlocking {
        AshValue.of(requestBuffInternal(bot, buffId, turnCount).isSuccess)
    }
}

internal fun GameRuntimeLibrary.resolveBuffSkillId(skillArg: AshValue): Int? {
    if (skillArg.type == AshType.INT) {
        return skillArg.toLong().toInt()
    }
    val token = skillArg.toString()
    return token.toIntOrNull() ?: gameDatabase?.skill(token)?.id
}

internal fun GameRuntimeLibrary.resolveBuffSkillId(skillToken: String): Int? =
    skillToken.toIntOrNull() ?: gameDatabase?.skill(skillToken)?.id

internal suspend fun GameRuntimeLibrary.requestBuffInternal(
    bot: String,
    buffId: Int,
    turns: Int?,
): Result<Unit> {
    val mgr = buffBotManager
        ?: return Result.failure(IllegalStateException("BuffBot not available"))
    val db = buffBotDatabase ?: BuffBotDatabase.instance
    val cost = db.find(buffId)
        ?: return Result.failure(IllegalArgumentException("No known cost for buff $buffId"))
    val turnCount = turns ?: cost.turns
    return mgr.requestBuff(bot, buffId, turnCount)
}

internal suspend fun GameRuntimeLibrary.runBuffRequestCli(
    bot: String,
    skillToken: String,
    turns: Int?,
    rt: AshRuntimeContext,
) {
    val buffId = resolveBuffSkillId(skillToken)
    if (buffId == null) {
        rt.print("Unknown skill: $skillToken")
        return
    }
    val result = requestBuffInternal(bot, buffId, turns)
    if (result.isSuccess) {
        rt.print("Buff request sent to $bot.")
    } else {
        rt.print(result.exceptionOrNull()?.message ?: "Buff request failed.")
    }
}
