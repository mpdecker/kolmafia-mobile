package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.request.AscensionRecord

/** Desktop AscensionHistoryRequest.refreshFields backup/compare — headless snapshot delta. */
data class AscensionHistoryCompare(
    val newAscensions: List<AscensionRecord> = emptyList(),
    val turnDeltas: Map<Int, Int> = emptyMap(),
    val pointDeltas: Map<Int, Int> = emptyMap(),
) {
    val hasChanges: Boolean get() = newAscensions.isNotEmpty() || turnDeltas.isNotEmpty() || pointDeltas.isNotEmpty()

    fun summaryLines(): List<String> = buildList {
        newAscensions.forEach { record ->
            add("New ascension ${record.number ?: "?"}: ${record.className} / ${record.pathName}")
        }
        turnDeltas.forEach { (number, delta) ->
            add("Ascension #$number in progress: +$delta turns since last fetch")
        }
        pointDeltas.forEach { (number, delta) ->
            add("Ascension #$number points: +$delta since last fetch")
        }
    }
}

object AscensionHistoryCompareLogic {
    fun compare(previous: List<AscensionRecord>, current: List<AscensionRecord>): AscensionHistoryCompare {
        if (previous.isEmpty()) return AscensionHistoryCompare()
        val previousByNumber = previous.mapNotNull { record ->
            record.number?.let { it to record }
        }.toMap()
        val currentByNumber = current.mapNotNull { record ->
            record.number?.let { it to record }
        }.toMap()

        val newAscensions = current.filter { record ->
            val number = record.number ?: return@filter false
            number !in previousByNumber
        }

        val turnDeltas = buildMap {
            for ((number, oldRecord) in previousByNumber) {
                val newRecord = currentByNumber[number] ?: continue
                val oldTurns = oldRecord.turns ?: continue
                val newTurns = newRecord.turns ?: continue
                if (newTurns > oldTurns) put(number, newTurns - oldTurns)
            }
        }

        val pointDeltas = buildMap {
            for ((number, oldRecord) in previousByNumber) {
                val newRecord = currentByNumber[number] ?: continue
                val oldPoints = oldRecord.points ?: continue
                val newPoints = newRecord.points ?: continue
                if (newPoints > oldPoints) put(number, newPoints - oldPoints)
            }
        }

        return AscensionHistoryCompare(newAscensions, turnDeltas, pointDeltas)
    }
}
