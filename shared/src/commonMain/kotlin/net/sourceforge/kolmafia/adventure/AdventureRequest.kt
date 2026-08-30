package net.sourceforge.kolmafia.adventure

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.parameters
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.quest.Quest
import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.quest.TavernCellarSync

/**
 * Desktop [AdventureRequest] formSource POST (Phases 2571–2630).
 */
class AdventureRequest(
    private val client: HttpClient,
    private val preferences: Preferences? = null,
    private val effectManager: EffectManager? = null,
    private val questDatabase: QuestDatabase? = null,
) {
    /**
     * Execute one adventure turn. Returns Pair(responseBody, finalUrl).
     * Mining is rejected like desktop (not automated).
     */
    suspend fun adventure(location: AdventureLocation): Result<Pair<String, String>> {
        val form = resolveForm(location)
        if (form.formSource == "mining.php") {
            return Result.failure(IllegalStateException("Automated mining is not currently implemented."))
        }
        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/${form.formSource}",
                formParameters = parameters {
                    for ((k, v) in form.fields) {
                        append(k, v)
                    }
                    if (form.formSource == "adventure.php" && "adv" !in form.fields) {
                        append("adv", "1")
                    }
                },
            )
            val body = response.bodyAsText()
            val finalUrl = response.request.url.toString()
            val processed = processResults(form, body, finalUrl)
            if (processed != null) {
                Result.failure(IllegalStateException(processed))
            } else {
                Result.success(body to finalUrl)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** Travel to a zone without spending an adventure (desktop set_location). */
    suspend fun travel(location: AdventureLocation): Result<String> = try {
        val form = resolveForm(location)
        val response = if (form.formSource == "adventure.php") {
            val snarf = form.fields["snarfblat"] ?: location.id
            client.get("$KOL_BASE_URL/adventure.php") {
                parameter("snarfblat", snarf)
            }
        } else {
            client.get("$KOL_BASE_URL/${form.formSource}") {
                for ((k, v) in form.fields) {
                    parameter(k, v)
                }
            }
        }
        Result.success(response.bodyAsText())
    } catch (e: Exception) {
        Result.failure(e)
    }

    /** Backward-compat: travel by snarfblat id string. */
    suspend fun travel(snarfblat: String): Result<String> =
        travel(AdventureLocation(id = snarfblat, name = "", zone = ""))

    fun resolveForm(location: AdventureLocation): AdventureForm {
        val ctx = buildContext(location.name)
        return AdventureFormBuilder.updateFields(
            formSource = location.formSource,
            adventureId = location.adventureId,
            ctx = ctx,
        )
    }

    fun buildRequestUrl(location: AdventureLocation): String =
        resolveForm(location).requestUrl

    private fun buildContext(adventureName: String): AdventureFormBuilder.Context {
        val prefs = preferences
        val hasAffinity = effectManager?.state?.value?.effects.orEmpty()
            .any { it.name.equals(ShadowRift.SHADOW_AFFINITY, ignoreCase = true) }
        val manorDone = questDatabase?.isQuestFinished(Quest.MANOR) == true
        val layout = prefs?.getString("tavernLayout", TavernCellarSync.EMPTY_LAYOUT)
            ?: TavernCellarSync.EMPTY_LAYOUT
        val square = recommendCellarSquare(layout)
        val autoFaucet = prefs?.getBoolean("autoFaucet", false) == true &&
            layout.contains('3')
        return AdventureFormBuilder.Context(
            adventureName = adventureName,
            preferences = prefs,
            hasShadowAffinity = hasAffinity,
            manorQuestFinished = manorDone,
            cellarSquare = square,
            cellarAutoFaucet = autoFaucet,
        )
    }

    /**
     * Desktop TavernManager.recommendSquare subset — first unexplored ('0') cell.
     */
    fun recommendCellarSquare(layout: String): Int {
        val pad = layout.padEnd(25, '0')
        for (i in pad.indices) {
            if (pad[i] == '0') return i + 1
        }
        return 0
    }

    /**
     * Selective desktop [AdventureRequest.processResults] place.php edges.
     * Returns an error message when adventure cannot continue; null on success.
     */
    fun processResults(form: AdventureForm, responseText: String, finalUrl: String): String? {
        if (responseText.isBlank() || responseText.contains("No, that isn't a place yet.")) {
            return "You can't get to that area yet."
        }
        if (form.formSource == "place.php") {
            val loc = form.requestUrl
            if (loc.contains("whichplace=nstower")) {
                // nstower should redirect to fight/choice
                if (!finalUrl.contains("fight.php", ignoreCase = true) &&
                    !finalUrl.contains("choice.php", ignoreCase = true) &&
                    !responseText.contains("fight.php", ignoreCase = true)
                ) {
                    return "You can't adventure there."
                }
            }
            if (loc.contains("crimbo22_engine") &&
                !finalUrl.contains("fight.php", ignoreCase = true) &&
                !responseText.contains("fight.php", ignoreCase = true)
            ) {
                return "You can't adventure there."
            }
        }
        return null
    }
}
