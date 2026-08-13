package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.clan.ClanLoungeVipSync
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences

open class ClanRumpusRequest(private val client: HttpClient) {

    /** GETs clan_basement.php to collect rumpus room breakfast items. */
    open suspend fun visit(): Result<Unit> = try {
        val response = client.get("$KOL_BASE_URL/clan_basement.php")
        if (!response.status.isSuccess())
            Result.failure(Exception("HTTP ${response.status.value}"))
        else
            Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Desktop ClanRumpusRequest(RequestType.BALLS) — jump in the awesome ball pit. */
    open suspend fun jumpInBallpit(preferences: Preferences? = null): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/clan_rumpus.php",
            formParameters = parameters {
                append("preaction", "ballpit")
            },
        )
        if (!response.status.isSuccess()) {
            Result.failure(Exception("HTTP ${response.status.value}"))
        } else {
            val html = response.bodyAsText()
            ClanLoungeVipSync.syncBallpitFromResponse(html, preferences)
            Result.success(html)
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Desktop ClanRumpusRequest(RequestType.CHIPS, whichbag). */
    open suspend fun buyChips(whichbag: Int): Result<String> {
        if (whichbag !in 1..3) {
            return Result.failure(IllegalArgumentException("Invalid chip bag: $whichbag"))
        }
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/clan_rumpus.php",
                formParameters = parameters {
                    append("preaction", "buychips")
                    append("whichbag", whichbag.toString())
                },
            )
            if (!response.status.isSuccess()) {
                Result.failure(Exception("HTTP ${response.status.value}"))
            } else {
                Result.success(response.bodyAsText())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Desktop ClanRumpusRequest(RequestType.SOFA) — rest on the clan sofa. */
    open suspend fun nap(turns: Int): Result<String> {
        if (turns <= 0) {
            return Result.failure(IllegalArgumentException("Invalid nap turns: $turns"))
        }
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/clan_rumpus.php",
                formParameters = parameters {
                    append("preaction", "nap")
                    append("numturns", turns.toString())
                },
            )
            if (!response.status.isSuccess()) {
                Result.failure(Exception("HTTP ${response.status.value}"))
            } else {
                Result.success(response.bodyAsText())
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Desktop ClanRumpusRequest(RequestType.JUKEBOX, song). */
    open suspend fun playJukebox(
        song: Int,
        preferences: Preferences? = null,
    ): Result<String> {
        if (song !in 1..4) {
            return Result.failure(IllegalArgumentException("Invalid jukebox song: $song"))
        }
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/clan_rumpus.php",
                formParameters = parameters {
                    append("preaction", "jukebox")
                    append("whichsong", song.toString())
                },
            )
            if (!response.status.isSuccess()) {
                Result.failure(Exception("HTTP ${response.status.value}"))
            } else {
                val html = response.bodyAsText()
                ClanLoungeVipSync.syncJukeboxFromResponse(preferences)
                Result.success(html)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        const val RADIUM = 1
        const val WINTERGREEN = 2
        const val ENNUI = 3

        private val CHIP_FLAVORS = listOf(
            "radium" to RADIUM,
            "wintergreen" to WINTERGREEN,
            "ennui" to ENNUI,
        )

        /** Desktop ClanRumpusRequest.findChips — radium=1, wintergreen=2, ennui=3. */
        fun findChips(name: String): Int {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return 0
            return CHIP_FLAVORS.firstOrNull { it.first.equals(trimmed, ignoreCase = true) }?.second ?: 0
        }

        data class Song(val modifier: String, val effect: String, val index: Int)

        val SONGS: List<Song> = listOf(
            Song("meat", "Material Witness", 1),
            Song("stats", "No Worries", 2),
            Song("item", "Techno Bliss", 3),
            Song("initiative", "Metal Speed", 4),
        )

        /** Desktop ClanRumpusRequest.findSong — numeric 1–4 or exact modifier/effect. */
        fun findSong(name: String): Int {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return 0
            trimmed.toIntOrNull()?.let { n ->
                return if (n in 1..SONGS.size) n else 0
            }
            for (song in SONGS) {
                if (trimmed.equals(song.modifier, ignoreCase = true) ||
                    trimmed.equals(song.effect, ignoreCase = true)
                ) {
                    return song.index
                }
            }
            return 0
        }
    }
}
