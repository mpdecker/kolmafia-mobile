package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.data.MonsterDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.session.MonsterManuelManager

/** Desktop questlog.php?which=6 Monster Manuel HTTP request and entry parser. */
open class MonsterManuelRequest(private val client: HttpClient) {

    open suspend fun fetchPage(page: String? = null): Result<String> = try {
        val response = client.submitForm(
            url = "$KOL_BASE_URL/questlog.php",
            formParameters = parameters {
                append("which", "6")
                if (!page.isNullOrBlank()) append("vl", page)
            },
        )
        if (!response.status.isSuccess()) {
            Result.failure(IllegalStateException("HTTP ${response.status.value}"))
        } else {
            val body = response.bodyAsText()
            parseResponse("questlog.php?which=6${page?.let { "&vl=$it" }.orEmpty()}", body)
            Result.success(body)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    open suspend fun fetchMonster(id: Int): Result<String> {
        if (id <= 0) return Result.success(MonsterManuelManager.NO_FACTOIDS)
        val cached = MonsterManuelManager.getCachedManuelText(id)
        if (cached.isNotEmpty()) return Result.success(cached)
        val page = getManuelPage(id)
            ?: return Result.success(MonsterManuelManager.NO_FACTOIDS)
        return fetchPage(page).map { MonsterManuelManager.getCachedManuelText(id) }
    }

    companion object {
        private val MONSTER_ENTRY =
            Regex("""<a name=['"]mon(\d+)['"]>.*?</table>""", RegexOption.DOT_MATCHES_ALL)

        fun getManuelPage(id: Int): String? {
            val monster = MonsterDatabase.getById(id) ?: return null
            val name = monster.manuelName ?: monster.name
            val first = name.firstOrNull() ?: return null
            return if (first.isLetter()) first.lowercaseChar().toString() else "-"
        }

        /**
         * Manuel fragments can appear on questlog pages and in fight responses after learning a
         * factoid, so parsing intentionally keys off the entry anchors rather than URL alone.
         */
        fun parseResponse(urlString: String?, responseText: String): Int {
            if (urlString != null &&
                !urlString.contains("questlog.php") &&
                !urlString.contains("fight.php")
            ) {
                return 0
            }
            var count = 0
            for (match in MONSTER_ENTRY.findAll(responseText)) {
                val id = match.groupValues[1].toIntOrNull() ?: continue
                MonsterManuelManager.registerMonster(id, match.value)
                count++
            }
            return count
        }

        fun registerRequest(urlString: String): Boolean =
            urlString.substringAfterLast("$KOL_BASE_URL/")
                .let { it.startsWith("questlog.php") && it.contains("which=6") }
    }
}
