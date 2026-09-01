package net.sourceforge.kolmafia.ash

import kotlinx.coroutines.runBlocking
import net.sourceforge.kolmafia.session.GuildUnlockManager

/** Guild unlock CLI adapter; orchestration remains in the shared session manager. */
internal fun GameRuntimeLibrary.cliCompleteGuild(print: (String) -> Unit) {
    val manager = guildUnlockManager
    if (manager == null) {
        print("Guild unlock manager is not available.")
        return
    }
    runBlocking {
        manager.unlockGuild().fold(
            onSuccess = { result ->
                when (result) {
                    GuildUnlockManager.UnlockResult.AlreadyUnlocked ->
                        print("Guild already unlocked.")
                    is GuildUnlockManager.UnlockResult.Unlocked ->
                        print("Guild successfully unlocked.")
                }
            },
            onFailure = { error ->
                print(error.message ?: "Guild unlock failed.")
            },
        )
    }
}
