package net.sourceforge.kolmafia.buffbot

import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.chat.ChatSender

class BuffBotManager(
    private val chatSender: ChatSender,
    private val database: BuffBotDatabase
) {
    suspend fun requestBuff(botName: String, buffId: Int, turns: Int): Result<Unit> {
        database.find(buffId)
            ?: return Result.failure(IllegalArgumentException("No known cost for buff $buffId"))
        return try {
            chatSender.sendPrivate(recipient = botName, message = "$buffId $turns")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
