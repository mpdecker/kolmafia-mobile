package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.request.AscensionRecord

/** Desktop AscensionSnapshot filter/cache subset — headless ascension history views. */
object AscensionSnapshotCache {

    enum class Filter { ALL, NORMAL, HARDCORE, CASUAL }

    data class Snapshot(
        val playerName: String?,
        val playerId: String?,
        val records: List<AscensionRecord>,
        val filter: Filter,
    ) {
        val ascensionCount: Int get() = records.size
        val totalPoints: Int get() = records.mapNotNull { it.points }.sum()
    }

    fun filter(records: List<AscensionRecord>, mode: Filter): List<AscensionRecord> = when (mode) {
        Filter.ALL -> records
        Filter.NORMAL -> records.filter { isNormal(it) }
        Filter.HARDCORE -> records.filter { isHardcore(it) }
        Filter.CASUAL -> records.filter { isCasual(it) }
    }

    fun build(
        records: List<AscensionRecord>,
        playerName: String?,
        playerId: String?,
        mode: Filter = Filter.ALL,
    ): Snapshot = Snapshot(
        playerName = playerName,
        playerId = playerId,
        records = filter(records, mode),
        filter = mode,
    )

    fun summaryLines(snapshot: Snapshot): List<String> = buildList {
        snapshot.playerName?.let { add("Player: $it (${snapshot.filter.name.lowercase()})") }
        add("Ascensions: ${snapshot.ascensionCount}, points: ${snapshot.totalPoints}")
        snapshot.records.take(5).forEach { record ->
            val number = record.number?.let { "#$it" } ?: "#?"
            add("$number ${record.className} / ${record.pathName}")
        }
        if (snapshot.records.size > 5) add("… and ${snapshot.records.size - 5} more")
    }

    fun classCounts(records: List<AscensionRecord>): Map<String, Int> =
        records.groupingBy { it.className.trim().ifEmpty { "Unknown" } }.eachCount()

    fun pathCounts(records: List<AscensionRecord>): Map<String, Int> =
        records.groupingBy { it.pathName.trim().ifEmpty { "Unknown" } }.eachCount()

    fun highestPointRecord(records: List<AscensionRecord>): AscensionRecord? =
        records.maxByOrNull { it.points ?: 0 }

    fun extendedSummaryLines(snapshot: Snapshot): List<String> = buildList {
        addAll(summaryLines(snapshot))
        val classes = classCounts(snapshot.records)
        if (classes.isNotEmpty()) {
            add("Classes: " + classes.entries.joinToString(", ") { "${it.key}×${it.value}" })
        }
        highestPointRecord(snapshot.records)?.let { best ->
            add("Best: #${best.number ?: "?"} ${best.className} (${best.points ?: 0} pts)")
        }
    }

    private fun isHardcore(record: AscensionRecord): Boolean =
        record.pathName.contains("hardcore", ignoreCase = true)

    private fun isCasual(record: AscensionRecord): Boolean =
        record.pathName.contains("casual", ignoreCase = true)

    private fun isNormal(record: AscensionRecord): Boolean = !isHardcore(record) && !isCasual(record)
}
