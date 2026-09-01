package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.request.AscensionHistoryRequest
import net.sourceforge.kolmafia.request.AscensionRecord

/** Headless cache and status adapter for read-only ascension history. */
class AscensionHistoryManager {
    private var cached: List<AscensionRecord> = emptyList()

    fun records(): List<AscensionRecord> = cached

    fun remember(records: List<AscensionRecord>) {
        cached = records
    }

    fun statusLines(): List<String> {
        if (cached.isEmpty()) {
            return listOf("Ascension history HTTP unavailable.")
        }
        return cached.map(AscensionHistoryRequest::formatRecord)
    }
}
