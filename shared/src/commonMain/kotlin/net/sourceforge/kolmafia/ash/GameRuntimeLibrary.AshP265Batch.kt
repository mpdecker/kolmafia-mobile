package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.faxbot.FaxBotDatabase
import net.sourceforge.kolmafia.request.ClanLoungeRequest

/** AshP265 — Faxbot PM request protocol + `can_faxbot`/`send_fax` ASH. */
internal fun GameRuntimeLibrary.registerAshP265Batch(scope: AshScope) {
    regFn(scope, "faxbot", AshType.BOOLEAN, listOf("monster" to AshType.MONSTER)) { _, args ->
        faxbotAsh(args[0], botName = null)
    }

    regFn(
        scope,
        "faxbot",
        AshType.BOOLEAN,
        listOf("monster" to AshType.MONSTER, "bot" to AshType.STRING),
    ) { _, args ->
        faxbotAsh(args[0], botName = args[1].toString())
    }

    regFn(scope, "can_faxbot", AshType.BOOLEAN, listOf("monster" to AshType.MONSTER)) { _, args ->
        canFaxbotAsh(args[0], botName = null)
    }

    regFn(
        scope,
        "can_faxbot",
        AshType.BOOLEAN,
        listOf("monster" to AshType.MONSTER, "bot" to AshType.STRING),
    ) { _, args ->
        canFaxbotAsh(args[0], botName = args[1].toString())
    }

    regFn(scope, "send_fax", AshType.VOID, emptyList()) { _, _ ->
        runBlocking { runSendFaxCli() }
        AshValue.VOID
    }
}

internal fun GameRuntimeLibrary.faxbotAsh(monsterArg: AshValue, botName: String?): AshValue {
    val monster = resolveFaxMonster(monsterArg) ?: return AshValue.FALSE
    return runBlocking {
        ensureFaxBotsConfigured()
        val mgr = faxBotManager ?: return@runBlocking AshValue.FALSE
        AshValue.of(mgr.requestFax(monster, botName).isSuccess)
    }
}

internal fun GameRuntimeLibrary.canFaxbotAsh(monsterArg: AshValue, botName: String?): AshValue {
    val monster = resolveFaxMonster(monsterArg) ?: return AshValue.FALSE
    return runBlocking {
        ensureFaxBotsConfigured()
        val db = faxBotDatabase ?: FaxBotDatabase.instance
        val probe = chatProbe
        if (probe == null) {
            return@runBlocking AshValue.of(db.canFaxbot(monster.id, botName))
        }
        AshValue.of(
            db.canFaxbotOnline(monster.id, botName) { botName ->
                probe.isPlayerOnline(botName)
            },
        )
    }
}

internal suspend fun GameRuntimeLibrary.runSendFaxCli() {
    val mgr = faxBotManager ?: return
    val result = mgr.sendFax()
    if (result.isFailure) {
        throw ScriptException(result.exceptionOrNull()?.message ?: "Fax send failed.")
    }
}

internal suspend fun GameRuntimeLibrary.runFaxbotCli(command: String, rt: AshRuntimeContext) {
    ensureFaxBotsConfigured()
    val mgr = faxBotManager
    if (mgr == null) {
        rt.print("Faxbot support is not available.")
        return
    }
    val result = mgr.requestFaxByCommand(command)
    if (result.isSuccess) {
        rt.print("Fax request sent.")
    } else {
        rt.print(result.exceptionOrNull()?.message ?: "Fax request failed.")
    }
}

internal suspend fun GameRuntimeLibrary.runFaxCli(option: String, rt: AshRuntimeContext) {
    val faxOption = clanLoungeRequest?.findFaxOption(option) ?: 0
    if (faxOption == 0) {
        rt.print("I don't understand what it means to '$option' a fax.")
        return
    }
    val mgr = faxBotManager
    val result = when (faxOption) {
        ClanLoungeRequest.SEND_FAX -> mgr?.sendFax()
        ClanLoungeRequest.RECEIVE_FAX -> mgr?.receiveFaxOnly()
        else -> null
    } ?: clanLoungeRequest?.let { lounge ->
        when (faxOption) {
            ClanLoungeRequest.SEND_FAX -> lounge.sendFax().map {}
            ClanLoungeRequest.RECEIVE_FAX -> lounge.receiveFax().map {}
            else -> Result.failure(IllegalArgumentException("Unknown fax option"))
        }
    }
    if (result?.isFailure == true) {
        rt.print(result.exceptionOrNull()?.message ?: "Fax command failed.")
    }
}

internal fun GameRuntimeLibrary.resolveFaxMonster(arg: AshValue): MonsterDefinition? {
    if (arg.type == AshType.INT) {
        return gameDatabase?.monster(arg.toLong().toInt())
    }
    return gameDatabase?.monster(arg.toString())
}

internal suspend fun GameRuntimeLibrary.ensureFaxBotsConfigured() {
    val db = faxBotDatabase ?: FaxBotDatabase.instance
    if (db.allBots().isNotEmpty()) return
    val client = httpClient ?: return
    db.configure(client, gameDatabase)
}
