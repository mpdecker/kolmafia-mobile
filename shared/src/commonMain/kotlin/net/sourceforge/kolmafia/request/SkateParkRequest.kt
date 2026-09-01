package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.SkateParkAvailability
import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.session.EquipmentManager
import net.sourceforge.kolmafia.session.SessionLogger

/** Desktop [SkateParkRequest] — daily Skate Park buffs. */
class SkateParkRequest(
    private val client: HttpClient,
    private val character: KoLCharacter? = null,
    private val inventory: InventoryManager? = null,
    private val equipmentManager: EquipmentManager? = null,
    private val equipmentRequest: EquipmentRequest? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    suspend fun takeBuff(
        place: String,
        preferences: Preferences?,
    ): Result<String> {
        val prefs = preferences
            ?: return Result.failure(IllegalStateException("Preferences are not available."))
        val buffIndex = SkateParkAvailability.placeToBuff(place)
            ?: return Result.failure(
                IllegalArgumentException("That's not a valid location in the Skate Park"),
            )
        val data = SkateParkAvailability.buffToData(buffIndex)
            ?: return Result.failure(IllegalArgumentException("Unknown skate buff."))
        ensureUpdatedSkatePark(prefs, character?.state?.value?.ascensionNumber ?: 0)
        val status = prefs.getString("skateParkStatus", "war")
        if (status != data.state) {
            return Result.failure(IllegalStateException("You cannot visit ${data.place}."))
        }
        if (prefs.getBoolean(data.setting, false)) {
            return Result.failure(IllegalStateException(data.error))
        }
        return try {
            equipUnderwater()
            val path = "$KOL_BASE_URL/sea_skatepark.php?action=${data.action}"
            registerRequest(path, sessionLogger)
            val response = client.get("$KOL_BASE_URL/sea_skatepark.php") {
                parameter("action", data.action)
            }
            if (!response.status.isSuccess()) {
                return Result.failure(IllegalStateException("Skate Park visit failed."))
            }
            val html = response.bodyAsText()
            if (parseResponse(data.action, html, prefs)) {
                return Result.failure(IllegalStateException(data.error))
            }
            Result.success(html)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun parseResponse(action: String, html: String, preferences: Preferences?): Boolean {
            val prefs = preferences ?: return false
            val data = SkateParkAvailability.BUFF_DATA.firstOrNull { it.action == action }
                ?: return false
            when {
                html.contains("ocean/rumble") -> prefs.setString("skateParkStatus", "war")
                html.contains("ocean/ice_territory") -> prefs.setString("skateParkStatus", "ice")
                html.contains("ocean/roller_territory") ->
                    prefs.setString("skateParkStatus", "roller")
                html.contains("ocean/fountain") -> prefs.setString("skateParkStatus", "peace")
            }
            val error = html.contains(data.error, ignoreCase = true) ||
                html.contains("already", ignoreCase = true) &&
                html.contains(data.place, ignoreCase = true)
            if (html.contains("You acquire an effect", ignoreCase = true) || error) {
                prefs.setBoolean(data.setting, true)
            }
            return error
        }

        fun parseResponseFromUrl(url: String, html: String, preferences: Preferences?): Boolean {
            val action = Regex("""(?:^|[?&])action=([^&]+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.getOrNull(1) ?: return false
            return parseResponse(action, html, preferences)
        }

        fun ensureUpdatedSkatePark(preferences: Preferences?, ascension: Int) {
            val prefs = preferences ?: return
            if (prefs.getInt("lastSkateParkReset", -1) < ascension) {
                prefs.setInt("lastSkateParkReset", ascension)
                prefs.setString("skateParkStatus", "war")
                SkateParkAvailability.BUFF_DATA.forEach {
                    prefs.setBoolean(it.setting, false)
                }
            }
        }

        fun findBuffAction(place: String): String? {
            val buff = SkateParkAvailability.placeToBuff(place) ?: return null
            return SkateParkAvailability.buffToData(buff)?.action
        }

        fun registerRequest(url: String, logger: SessionLogger?): Boolean {
            if (!url.startsWith("$KOL_BASE_URL/sea_skatepark.php", ignoreCase = true) &&
                !url.startsWith("sea_skatepark.php", ignoreCase = true)
            ) return false
            val action = Regex("""(?:^|[?&])action=([^&]+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.getOrNull(1)
            val data = action?.let { value ->
                SkateParkAvailability.BUFF_DATA.firstOrNull { it.action == value }
            }
            logger?.appendRawLine("Visiting ${data?.place ?: "the Skate Park"}")
            return true
        }
    }

    private suspend fun equipUnderwater() {
        val counts = inventory ?: return
        val equipment = equipmentRequest ?: return
        val self = listOf(
            6052, // aerated diving helmet
            10056, // scholar mask
            10057, // gladiator mask
            10058, // crappy mask
            4600, // scuba gear
            4601, // old scuba tank
        ).firstOrNull { counts.getCount(it) > 0 } ?: return
        if (equipmentManager?.hasEquipped(self) != true) {
            equipment.equipItem(self, EquipmentSlot.HAT)
        }
        val familiarItem = listOf(10059, 10060).firstOrNull { counts.getCount(it) > 0 } ?: return
        if (equipmentManager?.hasEquipped(familiarItem) != true) {
            equipment.equipItem(familiarItem, EquipmentSlot.FAMILIAR)
        }
    }
}
