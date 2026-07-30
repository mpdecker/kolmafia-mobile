package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ChoiceUtilities
import net.sourceforge.kolmafia.data.WereProfessorDatabase
import net.sourceforge.kolmafia.data.WereProfessorDatabase.Research
import net.sourceforge.kolmafia.preferences.Preferences

/** Choice 1523 research bench pref sync. Mirrors desktop [ResearchBenchRequest] visit/post hooks. */
object WereProfessorResearchSync {

    private val availableResearchPattern = Regex("""name="r" value="([^"]+)"""")
    private val researchPointsPattern =
        Regex("""<p>You have (\d+) research points? \(rp\)""")
    private val researchUrlPattern = Regex("""[?&]r=([^&]+)""")
    private val doResearchPattern = Regex("""You successfully research ([^.]+)\.""")

    fun isResearchBenchChoice(choiceId: Int): Boolean =
        choiceId == WereProfessorDatabase.RESEARCH_BENCH_CHOICE

    fun visitChoice(html: String, preferences: Preferences) {
        val rp = parseResearchPoints(html)
        preferences.setInt("wereProfessorResearchPoints", rp)

        val availableResearch = parseAvailableResearch(html)
        WereProfessorDatabase.saveResearch(preferences, WereProfessorDatabase.AVAILABLE_RESEARCH, availableResearch)

        val knownResearch = WereProfessorDatabase.deriveKnownResearch(availableResearch)
        WereProfessorDatabase.saveResearch(preferences, WereProfessorDatabase.KNOWN_RESEARCH, knownResearch)

        val knownResearchString = knownResearch.joinToString(",") { it.field }
        preferences.setInt("wereProfessorStomach", tierCount(knownResearchString, "stomach"))
        preferences.setInt("wereProfessorLiver", tierCount(knownResearchString, "liver"))
        preferences.setInt("wereProfessorBite", tierCount(knownResearchString, "bite"))
        preferences.setInt("wereProfessorKick", tierCount(knownResearchString, "kick"))
        preferences.setInt("wereProfessorRend", tierCount(knownResearchString, "rend"))
    }

    fun postChoice0(url: String, text: String, sessionLogger: SessionLogger? = null) {
        val research = getResearch(url) ?: return
        val message =
            "Researching ${research.name} (${research.field}) for ${research.cost} rp."
        sessionLogger?.appendRawLine(message)
    }

    fun postChoice2(
        url: String,
        text: String,
        preferences: Preferences,
        sessionLogger: SessionLogger? = null,
    ) {
        val research = getResearch(url) ?: return
        if (!doResearchPattern.containsMatchIn(text)) {
            sessionLogger?.appendRawLine("You failed to research ${research.name}.")
            return
        }
        val message = "You spent ${research.cost} rp to research ${research.name}."
        sessionLogger?.appendRawLine(message)
        if (!text.contains("choice.php")) {
            visitChoice(text, preferences)
        }
    }

    fun registerRequest(url: String, sessionLogger: SessionLogger? = null): Boolean {
        if (!url.contains("choice.php", ignoreCase = true)) return false
        val choice = ChoiceUtilities.extractChoiceFromUrl(url)
        if (choice != WereProfessorDatabase.RESEARCH_BENCH_CHOICE) return false
        val research = getResearch(url) ?: return false
        sessionLogger?.appendRawLine(
            "Took choice ${WereProfessorDatabase.RESEARCH_BENCH_CHOICE}/1: " +
                "${research.name} (${research.cost} rp)",
        )
        return true
    }

    fun parseResearchPoints(html: String): Int =
        researchPointsPattern.find(html)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

    fun parseAvailableResearch(html: String): Set<Research> {
        val result = mutableSetOf<Research>()
        for (match in availableResearchPattern.findAll(html)) {
            WereProfessorDatabase.findResearch(match.groupValues[1])?.let { result.add(it) }
        }
        return result
    }

    fun getResearch(url: String): Research? {
        val field = researchUrlPattern.find(url)?.groupValues?.getOrNull(1) ?: return null
        return WereProfessorDatabase.findResearch(field)
    }

    private fun tierCount(knownResearchString: String, prefix: String): Int =
        (1..3).count { knownResearchString.contains("$prefix$it") }
}
