package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.quest.QuestDatabase
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

data class QuestCouncilEntry(
    val prefKey: String,
    val step: String,
    val texts: List<String>,
)

@OptIn(ExperimentalResourceApi::class)
object QuestCouncilDatabase {

    private val entries = mutableListOf<QuestCouncilEntry>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/questscouncil.txt").decodeToString()
        entries.addAll(parse(text))
        loaded = true
    }

    fun handleCouncilText(responseText: String, questDatabase: QuestDatabase) {
        if (entries.isEmpty()) return
        val responseTokens = responseText.split(Regex("<[pP]>"))
        for (responseToken in responseTokens) {
            val cleanedResponse = normalizeToken(responseToken)
            if (cleanedResponse.isEmpty()) continue
            for (entry in entries) {
                if (matchEntry(cleanedResponse, entry)) {
                    setQuestIfBetter(questDatabase, entry.prefKey, entry.step)
                }
            }
        }
    }

    internal fun parseForTest(text: String): List<QuestCouncilEntry> = parse(text)

    internal fun injectForTest(fixture: List<QuestCouncilEntry>) {
        entries.clear()
        entries.addAll(fixture)
        loaded = true
    }

    private fun matchEntry(cleanedResponse: String, entry: QuestCouncilEntry): Boolean {
        for (text in entry.texts) {
            for (councilToken in text.split(Regex("<[pP]>"))) {
                val cleanedQuest = normalizeToken(councilToken)
                if (cleanedQuest.isNotEmpty() && cleanedResponse.contains(cleanedQuest)) {
                    return true
                }
            }
        }
        return false
    }

    private fun setQuestIfBetter(questDatabase: QuestDatabase, prefKey: String, step: String) {
        val current = questDatabase.progressFor(prefKey)
        if (QuestDatabase.stepOrdinal(step) > QuestDatabase.stepOrdinal(current)) {
            questDatabase.setProgressByPrefKey(prefKey, step)
        }
    }

    private fun parse(text: String): List<QuestCouncilEntry> {
        val result = mutableListOf<QuestCouncilEntry>()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue
            val parts = line.split('\t')
            if (parts.size < 3) continue
            val prefKey = parts[0]
            val step = parts[1]
            val texts = parts.drop(2)
            result.add(QuestCouncilEntry(prefKey, step, texts))
        }
        return result
    }

    private fun normalizeToken(text: String): String =
        text.replace(Regex("<[^>]+>"), "")
            .replace(Regex("\\s+"), "")
            .lowercase()
}
