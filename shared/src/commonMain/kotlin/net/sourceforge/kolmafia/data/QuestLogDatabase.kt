package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

data class QuestLogEntry(
    val prefKey: String,
    val title: String,
    val steps: List<Pair<String, String>>,  // stepName → normalized text
)

@OptIn(ExperimentalResourceApi::class)
object QuestLogDatabase {

    private val byTitle = mutableMapOf<String, QuestLogEntry>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/questslog.txt").decodeToString()
        byTitle.putAll(parse(text))
        loaded = true
    }

    fun findByTitle(title: String): QuestLogEntry? = byTitle[title.lowercase().trim()]

    fun detectStep(entry: QuestLogEntry, bodyHtml: String): String {
        val normalized = bodyHtml
            .replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
        for ((stepName, stepText) in entry.steps.asReversed()) {
            if (stepText.isNotEmpty() && normalized.contains(stepText)) return stepName
        }
        return "started"
    }

    /** Test-only: parse text without touching the singleton state. */
    internal fun parseForTest(text: String): Map<String, QuestLogEntry> = parse(text)

    /** Test-only: inject fixture entries without file I/O. */
    internal fun injectForTest(entries: List<QuestLogEntry>) {
        byTitle.clear()
        entries.forEach { byTitle[it.title.lowercase().trim()] = it }
    }

    private fun parse(text: String): Map<String, QuestLogEntry> {
        val result = mutableMapOf<String, QuestLogEntry>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue  // version line
            val parts = line.split('\t')
            if (parts.size < 3) continue
            val prefKey = parts[0]
            val title   = parts[1]
            val texts   = parts.drop(2)
            result[title.lowercase().trim()] = QuestLogEntry(prefKey, title, buildSteps(texts))
        }
        return result
    }

    private fun buildSteps(texts: List<String>): List<Pair<String, String>> {
        if (texts.isEmpty()) return emptyList()
        if (texts.size == 1) return listOf("finished" to texts[0].normalizeText())
        val result = mutableListOf<Pair<String, String>>()
        result.add("started" to texts[0].normalizeText())
        for (i in 1 until texts.size - 1) {
            result.add("step$i" to texts[i].normalizeText())
        }
        result.add("finished" to texts.last().normalizeText())
        return result
    }

    private fun String.normalizeText(): String =
        replace(Regex("<[^>]+>"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
            .lowercase()
}
