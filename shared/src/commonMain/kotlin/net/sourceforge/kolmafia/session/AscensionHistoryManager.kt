package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.request.AscensionHistoryRequest
import net.sourceforge.kolmafia.request.AscensionRecord

/** Headless cache and status adapter for read-only ascension history. */
class AscensionHistoryManager {
    private var cached: List<AscensionRecord> = emptyList()
    private var snapshot: List<AscensionRecord> = emptyList()
    private var playerName: String? = null
    private var playerId: String? = null
    private var lastCompare: AscensionHistoryCompare = AscensionHistoryCompare()

    fun records(): List<AscensionRecord> = cached

    fun playerName(): String? = playerName

    fun playerId(): String? = playerId

    fun lastCompare(): AscensionHistoryCompare = lastCompare

    fun remember(records: List<AscensionRecord>, name: String? = null, id: String? = null) {
        if (snapshot.isNotEmpty()) {
            lastCompare = AscensionHistoryCompareLogic.compare(snapshot, records)
        } else {
            lastCompare = AscensionHistoryCompare()
        }
        cached = records
        snapshot = records
        if (!name.isNullOrBlank()) playerName = name
        if (!id.isNullOrBlank()) playerId = id
    }

    fun statusLines(): List<String> {
        if (cached.isEmpty()) {
            return listOf("Ascension history HTTP unavailable.")
        }
        return buildList {
            playerName?.let { add("Player: $it") }
            lastCompare.summaryLines().forEach(::add)
            addAll(cached.map(AscensionHistoryRequest::formatRecord))
        }
    }
}
