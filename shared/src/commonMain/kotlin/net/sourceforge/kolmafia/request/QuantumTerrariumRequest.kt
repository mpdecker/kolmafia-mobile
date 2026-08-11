package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.FamiliarDefinitionDatabase
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.QuantumTerrariumSync
import net.sourceforge.kolmafia.session.SessionLogger
import net.sourceforge.kolmafia.session.TurnCounter

/** Desktop [QuantumTerrariumRequest] GET/POST + counter-driven pre-fetch in Quantum Terrarium. */
object QuantumTerrariumRequest {

    private var lastChecked = 0

    suspend fun fetchHtml(client: HttpClient): Result<String> {
        return try {
            val response = client.get("$KOL_BASE_URL/qterrarium.php")
            if (response.status.isSuccess()) {
                Result.success(response.bodyAsText())
            } else {
                Result.failure(Exception("HTTP ${response.status.value}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun forceAlign(
        client: HttpClient,
        familiarId: Int,
        character: KoLCharacter,
        preferences: Preferences?,
        sessionLogger: SessionLogger? = null,
    ): QuantumTerrariumSync.ParseResult {
        if (familiarId <= 0) return QuantumTerrariumSync.ParseResult()
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/qterrarium.php",
                formParameters = parameters {
                    append("action", "fam")
                    append("fid", familiarId.toString())
                },
            )
            if (!response.status.isSuccess()) {
                return QuantumTerrariumSync.ParseResult()
            }
            parseVisit(
                url = "qterrarium.php?action=fam&fid=$familiarId",
                html = response.bodyAsText(),
                character = character,
                preferences = preferences,
                sessionLogger = sessionLogger,
            )
        } catch (_: Exception) {
            QuantumTerrariumSync.ParseResult()
        }
    }

    suspend fun refresh(
        client: HttpClient,
        character: KoLCharacter,
        preferences: Preferences?,
        sessionLogger: SessionLogger? = null,
    ): QuantumTerrariumSync.ParseResult {
        val html = fetchHtml(client).getOrNull() ?: return QuantumTerrariumSync.ParseResult()
        return parseVisit(
            url = "qterrarium.php",
            html = html,
            character = character,
            preferences = preferences,
            sessionLogger = sessionLogger,
        )
    }

    fun parseVisit(
        url: String?,
        html: String,
        character: KoLCharacter,
        preferences: Preferences?,
        sessionLogger: SessionLogger? = null,
    ): QuantumTerrariumSync.ParseResult {
        if (url?.contains("qterrarium.php", ignoreCase = true) != true) {
            return QuantumTerrariumSync.ParseResult()
        }
        val result = QuantumTerrariumSync.parseResponse(
            html = html,
            preferences = preferences,
            characterState = character.state.value,
        )
        result.currentFamiliarId?.let { id ->
            val name = result.currentFamiliarName ?: character.state.value.familiarName
            val prev = character.state.value
            character.updateFamiliar(id, name, prev.familiarWeight, prev.familiarExp)
        }
        if (result.forcedAlign && result.forcedNextFamiliarId != null) {
            val famName = FamiliarDefinitionDatabase.getById(result.forcedNextFamiliarId)?.name
                ?: "unknown familiar"
            sessionLogger?.appendRawLine("Forced next quantum familiar to be $famName")
        }
        return result
    }

    /**
     * Desktop [QuantumTerrariumRequest.checkCounter] — refresh qterrarium when familiar counter
     * is about to expire or not yet counting.
     */
    suspend fun checkCounter(
        client: HttpClient,
        character: KoLCharacter,
        preferences: Preferences?,
        url: String?,
        hasResult: Boolean,
        sessionLogger: SessionLogger? = null,
    ): QuantumTerrariumSync.ParseResult {
        val state = character.state.value
        if (!state.inQuantum || !hasResult) {
            return QuantumTerrariumSync.ParseResult()
        }
        if (lastChecked >= state.currentRun) {
            return QuantumTerrariumSync.ParseResult()
        }
        if (url?.contains("tiles.php", ignoreCase = true) == true) {
            return QuantumTerrariumSync.ParseResult()
        }
        val prefs = preferences ?: return QuantumTerrariumSync.ParseResult()
        val expiring = TurnCounter.getCounterLabels(
            preferences = prefs,
            label = QuantumTerrariumSync.FAMILIAR_COUNTER,
            currentRun = state.currentRun,
            minTurns = 0,
            maxTurns = 1,
        ).any { it.equals(QuantumTerrariumSync.FAMILIAR_COUNTER, ignoreCase = true) }
        val notCounting = !TurnCounter.isCounting(
            preferences = prefs,
            label = QuantumTerrariumSync.FAMILIAR_COUNTER,
            currentRun = state.currentRun,
        )
        if (!expiring && !notCounting) {
            return QuantumTerrariumSync.ParseResult()
        }
        lastChecked = state.currentRun
        return refresh(client, character, preferences, sessionLogger)
    }

    internal fun resetLastCheckedForTest() {
        lastChecked = 0
    }
}
