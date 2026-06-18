package net.sourceforge.kolmafia.servant

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.http.isSuccess
import net.sourceforge.kolmafia.character.AscensionPath
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.modifiers.ServantData
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Minimal Ed servant switching — preference-backed summoned list + HTTP choice 1053.
 */
class EdServantManager(
    private val httpClient: HttpClient,
    private val preferences: Preferences,
    private val character: KoLCharacter?,
) {

    fun isEd(): Boolean {
        val path = character?.state?.value?.ascensionPath ?: return false
        return path == AscensionPath.ACTUALLY_ED_THE_UNDYING || path == AscensionPath.ED
    }

    fun getSummonedTypes(): List<String> =
        preferences.getString(SERVANTS_PREF, "")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun hasSummonedServant(type: String): Boolean {
        val resolved = ServantData.resolve(type)?.type ?: return false
        return getSummonedTypes().any { it.equals(resolved, ignoreCase = true) }
    }

    fun activeServantType(): String = preferences.getString(ACTIVE_SERVANT_PREF, "")

    fun syncFromCharpane(html: String) {
        if (!isEd()) return
        val type = EdServantCharpaneSync.parseActiveServantType(html) ?: return
        preferences.setString(ACTIVE_SERVANT_PREF, type)
    }

    fun printStatus(print: (String) -> Unit) {
        if (!isEd()) {
            print("Only Ed the Undying has entombed servants!")
            return
        }
        val active = activeServantType()
        val summoned = getSummonedTypes()
        if (summoned.isEmpty()) {
            print("No entombed servants summoned.")
            return
        }
        print("Entombed servants: ${summoned.joinToString(", ")}")
        if (active.isNotBlank()) {
            print("Active servant: $active")
        }
    }

    suspend fun useServant(type: String, print: (String) -> Unit): Boolean {
        if (!isEd()) {
            print("Only Ed the Undying has entombed servants!")
            return false
        }
        val servant = ServantData.resolve(type) ?: run {
            print("Ed has no servants of type \"$type\".")
            return false
        }
        if (!hasSummonedServant(servant.type)) {
            print("You have not called forth a ${servant.type} to be your servant.")
            return false
        }
        print("Putting your ${servant.type} to work...")
        return switchServant(servant, print)
    }

    private suspend fun switchServant(servant: ServantData.Servant, print: (String) -> Unit): Boolean {
        val previousChoice = preferences.getInt(CHOICE_PREF, 0)
        return try {
            preferences.setInt(CHOICE_PREF, servant.id)
            val door = httpClient.get("$KOL_BASE_URL/place.php?whichplace=edbase&action=edbase_door")
            if (!door.status.isSuccess()) {
                print("Failed to open Ed's door.")
                return false
            }
            val choice = httpClient.get(
                "$KOL_BASE_URL/choice.php?whichchoice=$ED_CHOICE&option=1&sid=${servant.id}",
            )
            if (!choice.status.isSuccess()) {
                print("Failed to switch to ${servant.type}.")
                return false
            }
            preferences.setString(ACTIVE_SERVANT_PREF, servant.type)
            true
        } catch (_: Exception) {
            print("Failed to switch to ${servant.type}.")
            false
        } finally {
            preferences.setInt(CHOICE_PREF, previousChoice)
        }
    }

    companion object {
        const val SERVANTS_PREF = "_edServants"
        const val ACTIVE_SERVANT_PREF = "_edActiveServant"
        const val CHOICE_PREF = "choiceAdventure1053"
        const val ED_CHOICE = 1053
    }
}
