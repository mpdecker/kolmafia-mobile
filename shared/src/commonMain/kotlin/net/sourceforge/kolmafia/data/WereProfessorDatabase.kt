package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * WereProfessor research skill tree from [wereprofessor.txt].
 * Mirrors desktop [ResearchBenchRequest] static research data.
 */
@OptIn(ExperimentalResourceApi::class)
object WereProfessorDatabase {

    data class Research(
        val key: Int,
        val field: String,
        val cost: Int,
        val parent: String,
        val name: String,
        val effect: String,
    ) : Comparable<Research> {
        override fun compareTo(other: Research): Int = key.compareTo(other.key)
    }

    private val allResearchInternal = sortedSetOf<Research>()
    private val fieldToResearch = mutableMapOf<String, Research>()
    private val terminalResearchInternal = mutableSetOf<Research>()
    private var loaded = false

    val isLoaded: Boolean get() = loaded
    val loadedResearchCount: Int get() = allResearchInternal.size

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/wereprofessor.txt").decodeToString()
        applyParse(parse(text))
        loaded = true
    }

    fun allResearch(): Set<Research> = allResearchInternal.toSet()

    fun findResearch(field: String): Research? {
        fieldToResearch[field]?.let { return it }
        if (field.startsWith(WEREPROF_PREFIX)) {
            return fieldToResearch[field.removePrefix(WEREPROF_PREFIX)]
        }
        return null
    }

    fun terminalResearch(): Set<Research> = terminalResearchInternal.toSet()

    internal fun parseForTest(text: String): ParseSnapshot = parse(text)

    internal fun injectForTest(snapshot: ParseSnapshot) {
        applyParse(snapshot)
        loaded = true
    }

    internal fun resetForTest() {
        allResearchInternal.clear()
        fieldToResearch.clear()
        terminalResearchInternal.clear()
        loaded = false
    }

    data class ParseSnapshot(
        val allResearch: Set<Research>,
        val fieldToResearch: Map<String, Research>,
        val terminalResearch: Set<Research>,
    )

    private const val WEREPROF_PREFIX = "wereprof_"

    private fun applyParse(snapshot: ParseSnapshot) {
        allResearchInternal.clear()
        allResearchInternal.addAll(snapshot.allResearch)
        fieldToResearch.clear()
        fieldToResearch.putAll(snapshot.fieldToResearch)
        terminalResearchInternal.clear()
        terminalResearchInternal.addAll(snapshot.terminalResearch)
    }

    private fun parse(text: String): ParseSnapshot {
        val researchSet = sortedSetOf<Research>()
        val fieldMap = mutableMapOf<String, Research>()
        val terminalSet = mutableSetOf<Research>()

        for (rawLine in text.lines()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("#")) continue

            val fields = line.split('\t')
            if (fields.size < 6) continue

            val key = fields[0].trim().toIntOrNull() ?: continue
            val field = fields[1].trim()
            val cost = fields[2].trim().toIntOrNull() ?: continue
            val parent = fields[3].trim()
            val name = fields[4].trim()
            val effect = fields[5].trim()

            val research = Research(key, field, cost, parent, name, effect)
            researchSet.add(research)
            fieldMap[field] = research
            fieldMap[WEREPROF_PREFIX + field] = research
            if (cost == TERMINAL_COST) {
                terminalSet.add(research)
            }
        }

        return ParseSnapshot(
            allResearch = researchSet,
            fieldToResearch = fieldMap,
            terminalResearch = terminalSet,
        )
    }

    private const val TERMINAL_COST = 100
}
