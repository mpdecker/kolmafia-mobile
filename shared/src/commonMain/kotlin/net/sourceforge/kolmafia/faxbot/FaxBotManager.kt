package net.sourceforge.kolmafia.faxbot

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.chat.ChatManager
import net.sourceforge.kolmafia.chat.ChatPoller
import net.sourceforge.kolmafia.chat.ChatProbe
import net.sourceforge.kolmafia.chat.ChatSender
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.data.MonsterDefinition
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.inventory.InventoryState
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ClanLoungeRequest
import net.sourceforge.kolmafia.session.BreakfastManager

class FaxBotManager(
    private val chatSender: ChatSender,
    private val chatPoller: ChatPoller,
    private val chatManager: ChatManager,
    private val clanLoungeRequest: ClanLoungeRequest,
    private val database: FaxBotDatabase,
    private val gameDatabase: GameDatabase?,
    private val preferences: Preferences?,
    private val inventoryManager: InventoryManager? = null,
    private val character: KoLCharacter? = null,
    private val chatProbe: ChatProbe? = null,
) {
    suspend fun requestFax(monster: MonsterDefinition, botName: String? = null): Result<Unit> {
        val preflight = canReceiveFax(getInventoryState(), character?.state?.value)
        if (preflight != null) {
            return Result.failure(IllegalStateException(preflight))
        }

        val bots = if (botName.isNullOrBlank()) {
            database.getSortedFaxbots(preferences)
        } else {
            listOfNotNull(database.getFaxbot(botName))
        }
        if (bots.isEmpty()) {
            return Result.failure(IllegalStateException("No faxbots configured."))
        }

        for (bot in bots) {
            val monsterRow = bot.getMonsterByMonsterId(monster.id) ?: continue
            if (!isBotOnline(bot.name)) continue
            if (requestFaxFromBot(bot.name, monsterRow).isSuccess) {
                preferences?.setString(FaxBotDatabase.PREF_LAST_SUCCESSFUL, bot.name)
                return Result.success(Unit)
            }
        }
        return Result.failure(IllegalStateException("Fax request failed."))
    }

    suspend fun requestFaxByCommand(command: String, botName: String? = null): Result<Unit> {
        val preflight = canReceiveFax(getInventoryState(), character?.state?.value)
        if (preflight != null) {
            return Result.failure(IllegalStateException(preflight))
        }

        val bots = if (botName.isNullOrBlank()) {
            database.getSortedFaxbots(preferences)
        } else {
            listOfNotNull(database.getFaxbot(botName))
        }
        if (bots.isEmpty()) {
            return Result.failure(IllegalStateException("No faxbots configured."))
        }

        for (bot in bots) {
            val matches = bot.findMatchingCommands(command)
            if (matches.isEmpty()) continue
            if (!isBotOnline(bot.name)) continue
            if (matches.size > 1) {
                return Result.failure(
                    IllegalStateException("[$command] has too many matches in bot ${bot.name}"),
                )
            }
            val monsterRow = bot.getMonsterByCommand(matches[0]) ?: continue
            if (requestFaxFromBot(bot.name, monsterRow).isSuccess) {
                preferences?.setString(FaxBotDatabase.PREF_LAST_SUCCESSFUL, bot.name)
                return Result.success(Unit)
            }
        }
        return Result.failure(IllegalStateException("No faxbots accept that command."))
    }

    suspend fun sendFax(): Result<Unit> {
        val inventoryState = getInventoryState()
        if (!inventoryState.items.containsKey(PHOTOCOPIED_MONSTER_ID)) {
            return Result.failure(
                IllegalStateException("You cannot send a fax without a photocopied monster in your inventory"),
            )
        }
        return clanLoungeRequest.sendFax().map {}
    }

    suspend fun receiveFaxOnly(): Result<Unit> {
        val inventoryState = getInventoryState()
        if (inventoryState.items.containsKey(PHOTOCOPIED_MONSTER_ID)) {
            return Result.failure(
                IllegalStateException("You cannot receive a fax with a photocopied monster in your inventory"),
            )
        }
        return clanLoungeRequest.receiveFax().map {}
    }

    private suspend fun isBotOnline(botName: String): Boolean {
        val probe = chatProbe ?: return true
        return probe.isPlayerOnline(botName)
    }

    private suspend fun requestFaxFromBot(botName: String, monster: FaxBotMonster): Result<Unit> {
        chatManager.activeFaxBot = botName
        try {
            chatManager.consumeLastFaxBotMessage()
            val sendResult = chatSender.sendPrivate(botName, monster.command)
            if (sendResult.isFailure) {
                return sendResult
            }

            while (true) {
                val response = waitForFaxBotResponse()
                    ?: return Result.failure(
                        IllegalStateException("No response from $botName after $RESPONSE_TIMEOUT_SECONDS seconds."),
                    )

                if (response.contains("just delivered a fax", ignoreCase = true)) {
                    delay(RETRY_DELAY_MS)
                    continue
                }

                if (isFaxSuccess(response)) {
                    return clanLoungeRequest.receiveFax().map {}
                }

                return Result.failure(IllegalStateException(response))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return Result.failure(e)
        } finally {
            chatManager.activeFaxBot = null
        }
    }

    private suspend fun waitForFaxBotResponse(): String? {
        val polls = (RESPONSE_TIMEOUT_SECONDS * 1000 / POLL_DELAY_MS).toInt()
        repeat(polls) {
            chatManager.consumeLastFaxBotMessage()
            val messages = chatPoller.fetchMessages()
            chatManager.dispatch(messages)
            chatManager.consumeLastFaxBotMessage()?.let { return it }
            delay(POLL_DELAY_MS)
        }
        return null
    }

    private fun isFaxSuccess(response: String): Boolean =
        response.contains("into your clan's Fax Machine", ignoreCase = true) ||
            response.contains("delivered to your clan Fax Machine", ignoreCase = true) ||
            response.contains("Your fax is ready", ignoreCase = true)

    private fun canReceiveFax(
        inventoryState: InventoryState,
        characterState: CharacterState? = null,
    ): String? {
        if (inventoryState.items.containsKey(PHOTOCOPIED_MONSTER_ID)) {
            return "You cannot receive a fax with a photocopied monster in your inventory."
        }
        if (!inventoryState.items.containsKey(BreakfastManager.VIP_LOUNGE_KEY_ID)) {
            return "You don't have a VIP key."
        }
        val state = characterState ?: CharacterState()
        if (state.isTrendy || state.isRestricted) {
            return "Fax machines are out of style."
        }
        if (state.isAxecore) {
            return "Boris sneered at technology."
        }
        if (state.ascensionPath == AscensionPath.AVATAR_OF_JARLSBERG) {
            return "Jarlsberg was more into magic than technology."
        }
        if (state.isSneakyPete) {
            return "Have you ever seen a cool person use a fax machine? I didn't think so."
        }
        return null
    }

    private fun getInventoryState(): InventoryState =
        inventoryManager?.state?.value ?: InventoryState()

    companion object {
        const val PHOTOCOPIED_MONSTER_ID = 4873
        private const val RESPONSE_TIMEOUT_SECONDS = 60
        private const val POLL_DELAY_MS = 200L
        private const val RETRY_DELAY_MS = 60_000L
    }
}
