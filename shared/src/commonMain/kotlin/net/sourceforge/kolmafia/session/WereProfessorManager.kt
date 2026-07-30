package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.WereProfessorDatabase
import net.sourceforge.kolmafia.data.WereProfessorDatabase.Research
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.request.ResearchBenchRequest

/** Desktop [net.sourceforge.kolmafia.textui.command.WereProfessorCommand] orchestration. */
object WereProfessorManager {

    private data class TreeRow(val field: String, val prefix: Int, val data: Int, val suffix: Int)

    private val rows = arrayOf(
        TreeRow("mus1", 0, 3, 3),
        TreeRow("rend1", 1, 4, 1),
        TreeRow("hp1", 1, 3, 2),
        TreeRow("skin1", 2, 4, 0),
        TreeRow("stomach1", 2, 4, 0),
        TreeRow("myst1", 0, 3, 3),
        TreeRow("bite1", 1, 4, 1),
        TreeRow("res1", 1, 3, 2),
        TreeRow("items1", 2, 4, 0),
        TreeRow("ml1", 2, 4, 0),
        TreeRow("mox1", 0, 3, 3),
        TreeRow("kick1", 1, 4, 1),
        TreeRow("init1", 1, 3, 2),
        TreeRow("meat1", 2, 4, 0),
        TreeRow("liver1", 2, 4, 0),
    )

    suspend fun printSkillTrees(
        charState: CharacterState,
        effectNames: Collection<String>,
        preferences: Preferences?,
        request: ResearchBenchRequest?,
        verbose: Boolean,
        print: (String) -> Unit,
    ) {
        if (!charState.inWereProfessor) {
            dumpSkills(verbose = verbose, print = print)
            print("")
            return
        }

        if (charState.isMildManneredProfessor(effectNames)) {
            request?.visitBench()
        }

        val prefs = preferences ?: run {
            dumpSkills(verbose = verbose, print = print)
            print("")
            return
        }
        val known = WereProfessorDatabase.loadResearch(prefs, WereProfessorDatabase.KNOWN_RESEARCH)
        val available =
            WereProfessorDatabase.loadResearch(prefs, WereProfessorDatabase.AVAILABLE_RESEARCH)
        val rp = prefs.getInt("wereProfessorResearchPoints", 0)

        dumpSkills(known, available, verbose, print)
        print("You have $rp Research Points available.")
        print("")
    }

    suspend fun researchSkill(
        field: String,
        charState: CharacterState,
        effectNames: Collection<String>,
        preferences: Preferences,
        request: ResearchBenchRequest?,
        print: (String) -> Unit,
    ): Boolean {
        val research = WereProfessorDatabase.findResearch(field) ?: run {
            print("'$field' is not known research")
            return false
        }

        if (!charState.inWereProfessor) {
            print("Only WereProfessors can use their Research Bench.")
            return false
        }

        if (charState.isSavageBeast(effectNames)) {
            print("You are locked out of your Humble Cottage.")
            return false
        }

        request?.visitBench()

        val known = WereProfessorDatabase.loadResearch(preferences, WereProfessorDatabase.KNOWN_RESEARCH)
        if (known.contains(research)) {
            print("You've already researched '${research.field}'.")
            return false
        }

        val available =
            WereProfessorDatabase.loadResearch(preferences, WereProfessorDatabase.AVAILABLE_RESEARCH)
        if (!available.contains(research)) {
            print("'${research.field}' is not currently available to research.")
            return false
        }

        val rp = preferences.getInt("wereProfessorResearchPoints", 0)
        if (research.cost > rp) {
            print("'${research.field}' requires ${research.cost} rp, but you only have $rp.")
            return false
        }

        if (request == null) {
            print("Research Bench is not available.")
            return false
        }

        request.research(research.field).onFailure { error ->
            print(error.message ?: "Research failed.")
            return false
        }

        val updatedRp = preferences.getInt("wereProfessorResearchPoints", 0)
        print("You have $updatedRp Research Points available.")
        return true
    }

    fun dumpSkills(
        known: Set<Research>? = null,
        available: Set<Research>? = null,
        verbose: Boolean = false,
        print: (String) -> Unit,
    ) {
        val annotate = known != null
        val allResearch = WereProfessorDatabase.allResearch().sorted().toTypedArray()
        val output = StringBuilder()
        output.append("<table border=2 cols=6>")

        var researchIndex = 0
        val knownSet = known
        val availableSet = available
        for (row in rows) {
            output.append("<tr>")

            if (row.prefix > 0) {
                output.append("<td colspan=")
                output.append(row.prefix)
                output.append(">&nbsp;</td>")
            }

            repeat(row.data) {
                val research = allResearch[researchIndex++]
                output.append("<td>")
                if (annotate) {
                    when {
                        knownSet.contains(research) ->
                            output.append("""<span style="color:black font-weight:bold">""")
                        availableSet!!.contains(research) ->
                            output.append("""<span style="color:red">""")
                        else ->
                            output.append("""<span style="color:gray">""")
                    }
                }
                output.append(research.field)
                if (!annotate || knownSet?.contains(research) != true) {
                    output.append(" (")
                    output.append(research.cost)
                    output.append(" rp)")
                }
                if (annotate) {
                    output.append("</span>")
                }
                if (verbose) {
                    output.append("<div>")
                    output.append(research.effect)
                    output.append("</div>")
                }
                output.append("</td>")
            }

            if (row.suffix > 0) {
                output.append("<td colspan=")
                output.append(row.suffix)
                output.append(">&nbsp;</td>")
            }

            output.append("</tr>")
        }

        output.append("</table>")
        print(output.toString())
    }
}
