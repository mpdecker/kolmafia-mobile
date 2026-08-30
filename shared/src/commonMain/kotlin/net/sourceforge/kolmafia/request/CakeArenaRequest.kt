package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CancellationException
import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.CakeArenaManager
import net.sourceforge.kolmafia.session.RequestLogger
import net.sourceforge.kolmafia.session.ResultProcessor
import net.sourceforge.kolmafia.session.SessionLogger

/**
 * Desktop [CakeArenaRequest] — visit opponent list + fight action=go
 * (Phases 3231–3245).
 */
open class CakeArenaRequest(
    private val client: HttpClient,
    private val preferences: Preferences? = null,
    private val character: KoLCharacter? = null,
    private val inventory: InventoryManager? = null,
    private val familiarManager: FamiliarManager? = null,
    private val sessionLogger: SessionLogger? = null,
) {
    var responseText: String? = null
        private set
    var results: Array<String>? = null
        private set
    var suckage: String? = null
        private set
    var lastEventId: Int = -1
        private set
    var ignoreCounters: Boolean = false

    open suspend fun visit(): Result<String> = try {
        val response = client.get("$KOL_BASE_URL/arena.php")
        if (!response.status.isSuccess()) {
            Result.failure(IllegalStateException("HTTP ${response.status.value}"))
        } else {
            val body = response.bodyAsText()
            responseText = body
            processResults("arena.php", body, isCompetition = false)
            Result.success(body)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    open suspend fun fight(
        opponentId: Int,
        eventId: Int,
        ignoreCounters: Boolean = false,
    ): Result<String> = try {
        this.ignoreCounters = ignoreCounters
        lastEventId = eventId
        registerRequest(
            "arena.php?action=go&whichopp=$opponentId&event=$eventId",
            character,
            familiarManager,
            sessionLogger,
        )
        val response = client.submitForm(
            url = "$KOL_BASE_URL/arena.php",
            formParameters = parameters {
                append("action", "go")
                append("whichopp", opponentId.toString())
                append("event", eventId.toString())
            },
        )
        if (!response.status.isSuccess()) {
            Result.failure(IllegalStateException("HTTP ${response.status.value}"))
        } else {
            val body = response.bodyAsText()
            responseText = body
            processResults(
                "arena.php?action=go&whichopp=$opponentId&event=$eventId",
                body,
                isCompetition = true,
            )
            Result.success(body)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    fun earnedXP(): Int = earnedXP(responseText.orEmpty())

    fun badContest(): Boolean = suckage != null

    private fun processResults(url: String, html: String, isCompetition: Boolean) {
        if (html.contains("You can't") ||
            html.contains("You shouldn't") ||
            html.contains("You don't") ||
            html.contains("You need")
        ) {
            return
        }
        if (html.contains("You're way too beaten") || html.contains("You're too drunk")) {
            return
        }
        parseResponse(url, html, preferences, character, inventory, familiarManager, sessionLogger)
        if (isCompetition) {
            results = contestLines(html)
            suckage = parseSuckage(results)
        }
    }

    companion object {
        val WIN_PATTERN = Regex("""is the winner, and gains (\d+) experience""")
        private val WINCOUNT_PATTERN = Regex("""You have won (\d*) time""")
        private val OPPONENT_PATTERN = Regex(
            """name=whichopp value=(\d+)>.*?<b>(.*?)</b> the (.*?)<br/?>(\d+)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        private val CONTEST_PATTERN = Regex(
            """<table><tr><td>(You enter.*?)</td></tr></table>""",
            RegexOption.DOT_MATCHES_ALL,
        )
        private val ENTRY_PATTERN = Regex(
            """You enter (.*?) against (.*?) in (?:a game of|an|a) (.*?)(?: race)?\.""",
        )
        private val EVENT_PATTERN = Regex("""event=(\d*)""")
        private val OPP_PATTERN = Regex("""whichopp=(\d*)""")

        fun getAdventuresUsed(urlString: String): Int =
            if (urlString.contains("action=go")) 1 else 0

        fun getAdventuresUsed(isCompetition: Boolean): Int = if (isCompetition) 1 else 0

        fun earnedXP(responseText: String): Int =
            WIN_PATTERN.find(responseText)?.groupValues?.get(1)?.toIntOrNull() ?: 0

        fun parseResponse(
            urlString: String,
            responseText: String,
            preferences: Preferences? = null,
            character: KoLCharacter? = null,
            inventory: InventoryManager? = null,
            familiarManager: FamiliarManager? = null,
            sessionLogger: SessionLogger? = null,
        ) {
            preferences?.setBoolean("cakeArenaVisited", true)

            if (urlString.contains("action=go")) {
                if (responseText.contains("You don't have enough Meat")) return

                ResultProcessor.processMeat(-100, character)

                val eventId = getEvent(urlString)
                val lines = contestLines(responseText)
                if (lines != null) {
                    for (i in 1 until lines.size) {
                        if (isContestResult(eventId, lines[i])) break
                        RequestLogger.updateSessionLog(prettyContestLine(lines[i]), sessionLogger)
                    }
                }

                val message = resultMessage(responseText, familiarManager, character)
                RequestLogger.updateSessionLog(message, sessionLogger)

                if (!message.contains("lost")) {
                    val wins = (character?.state?.value?.arenaWins ?: 0) + 1
                    character?.updateArenaWins(wins)
                    preferences?.setInt("cakeArenaWins", wins)
                }

                // Preserve thin-sync fights-left scrape when present
                Regex("""You have (\d+) fight""", RegexOption.IGNORE_CASE)
                    .find(responseText)?.groupValues?.get(1)?.toIntOrNull()?.let {
                        preferences?.setInt("cakeArenaFightsLeft", it)
                    }

                parseResults(responseText, inventory, character, preferences, familiarManager)
                return
            }

            WINCOUNT_PATTERN.find(responseText)?.groupValues?.get(1)?.toIntOrNull()?.let { wins ->
                character?.updateArenaWins(wins)
                preferences?.setInt("cakeArenaWins", wins)
            }

            Regex("""You have (\d+) fight""", RegexOption.IGNORE_CASE)
                .find(responseText)?.groupValues?.get(1)?.toIntOrNull()?.let {
                    preferences?.setInt("cakeArenaFightsLeft", it)
                }

            var last = 0
            while (true) {
                val m = OPPONENT_PATTERN.find(responseText, last) ?: break
                last = m.range.last + 1
                val id = m.groupValues[1].toIntOrNull() ?: continue
                val name = m.groupValues[2]
                val race = m.groupValues[3]
                val weight = m.groupValues[4].toIntOrNull() ?: 0
                CakeArenaManager.registerOpponent(id, name, race, weight)
            }

            ResultProcessor.processResults(false, responseText, inventory, character, preferences)
        }

        fun parseResults(
            responseText: String,
            inventory: InventoryManager? = null,
            character: KoLCharacter? = null,
            preferences: Preferences? = null,
            familiarManager: FamiliarManager? = null,
        ): Boolean {
            val familiarId = familiarManager?.state?.value?.activeFamiliar?.id
                ?: character?.state?.value?.familiarId
                ?: 0
            // Baby Bugged Bugbear free beanie on arena win prize text
            if (familiarId == 40 &&
                responseText.contains("Congratulations on your %arenawins arena win")
            ) {
                inventory?.gainItemLocally(ItemPool.BUGGED_BEANIE, 1)
                return true
            }
            return ResultProcessor.processResults(false, responseText, inventory, character, preferences)
        }

        fun registerRequest(
            urlString: String,
            character: KoLCharacter? = null,
            familiarManager: FamiliarManager? = null,
            sessionLogger: SessionLogger? = null,
        ): Boolean {
            val url = urlString.substringAfterLast("$KOL_BASE_URL/")
            if (!url.startsWith("arena.php")) return false
            if (!url.contains("action=go")) return true

            val familiar = familiarManager?.state?.value?.activeFamiliar
            if (familiar == null && (character?.state?.value?.familiarId ?: 0) <= 0) return true
            if ((character?.state?.value?.meat ?: 0) < 100) return true

            val opponent = getOpponent(url)
            if (opponent < 0) return true
            val event = getEvent(url)
            if (event < 0) return true

            val ao = CakeArenaManager.getOpponent(opponent)
            val eventName = CakeArenaManager.eventIdToName(event)
            val turns = character?.state?.value?.turnsPlayed
                ?: 0
            val message1 = "[$turns] Cake-Shaped Arena"

            val famName = familiar?.name
                ?: character?.state?.value?.familiarName
                ?: "familiar"
            val famWeight = familiar?.weight
                ?: character?.state?.value?.familiarWeight
                ?: 0
            val famRace = familiar?.race
                ?: net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
                    .getById(character?.state?.value?.familiarId ?: 0)?.name
                ?: "familiar"
            val fam1 = "$famName, the $famWeight lb. $famRace"
            val fam2 = if (ao == null) {
                "opponent #$opponent"
            } else {
                "${ao.name}, the ${ao.weight} lb. ${ao.race}"
            }

            RequestLogger.updateSessionLog(message1, sessionLogger)
            RequestLogger.updateSessionLog("Familiar: $fam1", sessionLogger)
            RequestLogger.updateSessionLog("Opponent: $fam2", sessionLogger)
            RequestLogger.updateSessionLog("Contest: $eventName", sessionLogger)
            return true
        }

        private fun getEvent(urlString: String): Int =
            EVENT_PATTERN.find(urlString)?.groupValues?.get(1)?.toIntOrNull() ?: -1

        private fun getOpponent(urlString: String): Int =
            OPP_PATTERN.find(urlString)?.groupValues?.get(1)?.toIntOrNull() ?: -1

        internal fun contestLines(responseText: String): Array<String>? {
            val m = CONTEST_PATTERN.find(responseText) ?: return null
            return m.groupValues[1]
                .replace("<p><p>", "<p>(Missing \"this familiar sucks at this contest\" message)<p>")
                .split("<p>")
                .toTypedArray()
        }

        private fun prettyContestLine(line: String): String =
            line.replace("<br>", " / ", ignoreCase = true)

        private fun isContestResult(eventId: Int, line: String): Boolean = when (eventId) {
            1 -> line.contains("is eventually knocked out") ||
                (line.contains("knocks") && line.contains("out after"))
            2 -> line.contains("items from the list")
            3 -> line.contains("makes it through the obstacle course")
            4 -> line.contains("manages to stay hidden for")
            else -> false
        }

        private fun parseSuckage(lines: Array<String>?): String? {
            if (lines == null || lines.size < 2) return null
            val m = ENTRY_PATTERN.find(lines[0]) ?: return null
            val eventId = CakeArenaManager.eventNameToId(m.groupValues[3])
            val line2 = lines[1]
            if (isContestResult(eventId, line2)) return null
            val opponentName = m.groupValues[2]
            if (line2.contains(opponentName)) return null
            return prettyContestLine(line2)
        }

        private fun resultMessage(
            responseText: String,
            familiarManager: FamiliarManager?,
            character: KoLCharacter?,
        ): String {
            val name = familiarManager?.state?.value?.activeFamiliar?.name
                ?: character?.state?.value?.familiarName
                ?: "familiar"
            val xp = earnedXP(responseText)
            return if (xp > 0) {
                val gain = responseText.contains("gains a pound")
                familiarManager?.let { fm ->
                    val active = fm.state.value.activeFamiliar ?: return@let
                    val newXp = active.experience + xp
                    val newWeight = if (gain) active.weight + 1 else active.weight
                    fm.applyActiveWeightXpLocally(newWeight, newXp)
                }
                "$name gains $xp experience${if (gain) " and a pound." else "."}"
            } else {
                "$name lost."
            }
        }
    }
}
