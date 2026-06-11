package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

object AdventureDatabase {
    private val byName = mutableMapOf<String, AdventureZone>()
    private val bySnarfblat = mutableMapOf<String, AdventureZone>()
    private val zones = mutableListOf<AdventureZone>()
    private var loaded = false

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load() {
        if (loaded) return
        loaded = true

        val text = Res.readBytes("files/data/adventures.txt").decodeToString()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isBlank() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue

            val parts = line.split('\t')
            if (parts.size < 4) continue

            val zoneName = parts[0]
            val urlParams = parts[1]
            val attributesRaw = parts[2]
            val locationName = parts[3]
            val goalsRaw = if (parts.size > 4) parts[4] else ""
            val goals = if (goalsRaw.isBlank()) emptyList() else goalsRaw.split('|').map { it.trim() }.filter { it.isNotBlank() }

            var environment = "unknown"
            var diffLevel = "unknown"
            var statRequirement = 0
            var forceNoncombat = 0
            var isOverdrunk = false
            var noWander = false

            val attrTokens = attributesRaw.split(' ')
            var i = 0
            while (i < attrTokens.size) {
                val token = attrTokens[i]
                when {
                    token == "overdrunk" -> isOverdrunk = true
                    token == "nowander" -> noWander = true
                    token == "DiffLevel:" && i + 1 < attrTokens.size -> {
                        diffLevel = attrTokens[i + 1]
                        i++
                    }
                    token == "Env:" && i + 1 < attrTokens.size -> {
                        environment = attrTokens[i + 1]
                        i++
                    }
                    token == "Stat:" && i + 1 < attrTokens.size -> {
                        statRequirement = attrTokens[i + 1].toIntOrNull() ?: 0
                        i++
                    }
                    token == "ForceNoncombat:" && i + 1 < attrTokens.size -> {
                        forceNoncombat = attrTokens[i + 1].toIntOrNull() ?: 0
                        i++
                    }
                    token.endsWith(":") && i + 1 < attrTokens.size -> {
                        // skip unknown key: value pairs
                        i++
                    }
                }
                i++
            }

            val zone = AdventureZone(
                zoneName = zoneName,
                urlParams = urlParams,
                locationName = locationName,
                environment = environment,
                diffLevel = diffLevel,
                statRequirement = statRequirement,
                goals = goals,
                isOverdrunk = isOverdrunk,
                noWander = noWander
            )

            zones.add(zone)
            byName[locationName.lowercase()] = zone
            zone.snarfblat?.let { bySnarfblat[it] = zone }
        }
    }

    fun getByName(query: String): AdventureZone? = byName[query.lowercase()]

    fun getBySnarfblat(s: String): AdventureZone? = bySnarfblat[s]

    fun byZone(zoneName: String): List<AdventureZone> =
        zones.filter { it.zoneName.equals(zoneName, ignoreCase = true) }

    fun search(query: String): List<AdventureZone> {
        val q = query.lowercase()
        return zones.filter {
            it.locationName.lowercase().contains(q) || it.zoneName.lowercase().contains(q)
        }
    }

    fun all(): List<AdventureZone> = zones.toList()

    /** Test hook — register a zone without loading adventures.txt. */
    internal fun injectForTest(zone: AdventureZone) {
        zones.add(zone)
        byName[zone.locationName.lowercase()] = zone
        zone.snarfblat?.let { bySnarfblat[it] = zone }
        loaded = true
    }

    /** Test hook — reset singleton state. */
    internal fun resetForTest() {
        byName.clear()
        bySnarfblat.clear()
        zones.clear()
        loaded = false
    }
}
