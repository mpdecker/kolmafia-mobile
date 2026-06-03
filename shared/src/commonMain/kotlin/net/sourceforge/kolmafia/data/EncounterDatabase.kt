package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

object EncounterDatabase {
    private val byLocation = mutableMapOf<String, MutableList<EncounterData>>()
    private val encounters = mutableListOf<EncounterData>()
    private var loaded = false

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load() {
        if (loaded) return
        loaded = true

        val text = Res.readBytes("files/data/encounters.txt").decodeToString()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isBlank() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 3) continue

            val locationName = parts[0]
            val type = parts[1]
            val title = parts[2]

            val data = EncounterData(
                locationName = locationName,
                type = type,
                title = title
            )

            encounters.add(data)
            byLocation.getOrPut(locationName.lowercase()) { mutableListOf() }.add(data)
        }
    }

    fun forLocation(name: String): List<EncounterData> =
        (byLocation[name.lowercase()] ?: emptyList<EncounterData>()) +
            (byLocation["*"] ?: emptyList())

    fun globalEncounters(): List<EncounterData> = byLocation["*"] ?: emptyList()

    fun autoStops(): List<EncounterData> = encounters.filter { it.isAutoStop }

    fun all(): List<EncounterData> = encounters.toList()
}
