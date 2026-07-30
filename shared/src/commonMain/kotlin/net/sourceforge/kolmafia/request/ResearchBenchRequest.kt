package net.sourceforge.kolmafia.request

import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import net.sourceforge.kolmafia.adventure.AdventureManager
import net.sourceforge.kolmafia.adventure.choice.EffectPool
import net.sourceforge.kolmafia.data.WereProfessorDatabase
import net.sourceforge.kolmafia.effect.EffectManager
import net.sourceforge.kolmafia.http.KOL_BASE_URL
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.WereProfessorResearchSync

/** Desktop [ResearchBenchRequest] — WereProfessor research bench visit + choice 1523 POST. */
open class ResearchBenchRequest(
    private val client: HttpClient,
    private val effects: EffectManager,
    private val preferences: Preferences,
) {

    sealed class ResearchBenchError(message: String) : Exception(message) {
        class NotMildManneredProfessor :
            ResearchBenchError("Only Mild-Mannered Professors can research at their Research Bench.")

        class InvalidResearch(field: String) :
            ResearchBenchError("Research '$field' is not valid.")

        class AlreadyKnown(field: String) :
            ResearchBenchError("You have already researched '$field'.")

        class NotAvailable(field: String) :
            ResearchBenchError("You cannot research '$field' at this time.")

        class InsufficientRp(field: String) :
            ResearchBenchError("You don't have enough rp to research '$field'.")
    }

    open suspend fun visitBench(): Result<Pair<String, String>> {
        if (!isMildManneredProfessor()) {
            return Result.failure(ResearchBenchError.NotMildManneredProfessor())
        }
        if (isAlreadyOnBench()) {
            return Result.success("" to "")
        }
        return try {
            val response = client.get(
                "$KOL_BASE_URL/place.php?whichplace=wereprof_cottage&action=wereprof_researchbench",
            )
            if (!response.status.isSuccess()) {
                Result.failure(Exception("HTTP ${response.status.value}"))
            } else {
                val html = response.bodyAsText()
                val url = response.request.url.toString()
                WereProfessorResearchSync.visitChoice(html, preferences)
                Result.success(html to url)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    open suspend fun research(field: String): Result<Pair<String, String>> {
        val skill = WereProfessorDatabase.findResearch(field)
            ?: return Result.failure(ResearchBenchError.InvalidResearch(field))

        if (!isMildManneredProfessor()) {
            return Result.failure(ResearchBenchError.NotMildManneredProfessor())
        }

        if (!isAlreadyOnBench()) {
            visitBench().onFailure { return Result.failure(it) }
        }

        val known = WereProfessorDatabase.loadResearch(preferences, WereProfessorDatabase.KNOWN_RESEARCH)
        if (known.contains(skill)) {
            return Result.failure(ResearchBenchError.AlreadyKnown(skill.field))
        }

        val available =
            WereProfessorDatabase.loadResearch(preferences, WereProfessorDatabase.AVAILABLE_RESEARCH)
        if (!available.contains(skill)) {
            return Result.failure(ResearchBenchError.NotAvailable(skill.field))
        }

        val rp = preferences.getInt("wereProfessorResearchPoints", 0)
        if (skill.cost > rp) {
            return Result.failure(ResearchBenchError.InsufficientRp(skill.field))
        }

        return try {
            val response = client.submitForm(
                url = "$KOL_BASE_URL/choice.php",
                formParameters = parameters {
                    append("whichchoice", WereProfessorDatabase.RESEARCH_BENCH_CHOICE.toString())
                    append("option", "1")
                    append("r", "wereprof_${skill.field}")
                },
            )
            if (!response.status.isSuccess()) {
                Result.failure(Exception("HTTP ${response.status.value}"))
            } else {
                val html = response.bodyAsText()
                val url = buildResearchUrl(skill.field)
                WereProfessorResearchSync.postChoice2(url, html, preferences)
                if (html.contains("whichchoice")) {
                    WereProfessorResearchSync.visitChoice(html, preferences)
                }
                Result.success(html to url)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    protected open fun isMildManneredProfessor(): Boolean =
        effects.state.value.effects.any {
            it.name.equals(EffectPool.MILD_MANNERED_PROFESSOR, ignoreCase = true)
        }

    private fun isAlreadyOnBench(): Boolean =
        preferences.getInt(AdventureManager.LAST_CHOICE_ID, 0) ==
            WereProfessorDatabase.RESEARCH_BENCH_CHOICE

    companion object {
        fun buildResearchUrl(field: String): String =
            "choice.php?whichchoice=${WereProfessorDatabase.RESEARCH_BENCH_CHOICE}" +
                "&option=1&r=wereprof_$field"
    }
}
