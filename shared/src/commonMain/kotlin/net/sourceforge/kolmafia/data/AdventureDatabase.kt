package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.adventure.ShadowRift
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

object AdventureDatabase {
    private val byName = mutableMapOf<String, AdventureZone>()
    private val bySnarfblat = mutableMapOf<String, AdventureZone>()
    private val byUrl = mutableMapOf<String, AdventureZone>()
    private val zones = mutableListOf<AdventureZone>()
    private var loaded = false

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load() {
        if (loaded) return
        loaded = true

        val text = Res.readBytes("files/data/adventures.txt").decodeToString()
        loadFromText(text)
    }

    internal fun loadFromText(text: String) {
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
            var explicitWaterLevel: Int? = null

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
                    token == "Level:" && i + 1 < attrTokens.size -> {
                        explicitWaterLevel = attrTokens[i + 1].toIntOrNull()
                        i++
                    }
                    token.endsWith(":") && i + 1 < attrTokens.size -> {
                        i++
                    }
                }
                i++
            }

            val waterLevel = computeWaterLevel(environment, statRequirement, explicitWaterLevel)

            val zone = AdventureZone(
                zoneName = zoneName,
                urlParams = urlParams,
                locationName = locationName,
                environment = environment,
                diffLevel = diffLevel,
                statRequirement = statRequirement,
                goals = goals,
                isOverdrunk = isOverdrunk,
                noWander = noWander,
                forceNoncombat = forceNoncombat,
                waterLevel = waterLevel,
            )

            registerZone(zone)
        }
    }

    private fun registerZone(zone: AdventureZone) {
        zones.add(zone)
        byName[zone.locationName.lowercase()] = zone
        zone.snarfblat?.let { bySnarfblat[it] = zone }
        // Desktop indexes by formSource?id=adventureId (e.g. adventure.php?snarfblat=112)
        val urlKey = when (zone.formSource) {
            "adventure.php" -> "adventure.php?snarfblat=${zone.adventureId}"
            "place.php" -> "place.php?whichplace=${zone.adventureId}"
            else -> "${zone.formSource}?${zone.urlKey}=${zone.adventureId}"
        }
        byUrl[urlKey.lowercase()] = zone
        byUrl[zone.formSource.lowercase() + "?" + zone.urlParams.lowercase()] = zone
    }

    fun getByName(query: String): AdventureZone? = byName[query.lowercase()]

    fun getBySnarfblat(s: String): AdventureZone? = bySnarfblat[s]

    /**
     * Desktop [AdventureDatabase.getAdventureByURL] subset —
     * shadow_rift + high-traffic place.php aliases + snarfblat.
     */
    fun getAdventureByURL(adventureURL: String): AdventureZone? {
        var url = adventureURL.trim()
        val q = url.indexOf('?')
        if (q < 0 && !url.contains('=')) {
            // bare snarfblat
            return bySnarfblat[url]
        }
        // Strip host / leading path noise
        val pathStart = url.indexOf("adventure.php", ignoreCase = true).takeIf { it >= 0 }
            ?: url.indexOf("place.php", ignoreCase = true).takeIf { it >= 0 }
            ?: url.indexOf("casino.php", ignoreCase = true).takeIf { it >= 0 }
            ?: url.indexOf("cellar.php", ignoreCase = true).takeIf { it >= 0 }
            ?: 0
        url = url.substring(pathStart)

        // Snarfblat
        Regex("""snarfblat=(\d+)""", RegexOption.IGNORE_CASE).find(url)?.let { m ->
            bySnarfblat[m.groupValues[1]]?.let { return it }
        }

        // Shadow rift place.php — map whichplace to named zone via enum
        if (url.contains("place.php", ignoreCase = true)) {
            val place = Regex("""whichplace=([^&]+)""", RegexOption.IGNORE_CASE)
                .find(url)?.groupValues?.getOrNull(1)
            if (place != null) {
                val rift = ShadowRift.findPlace(place)
                if (rift != null) {
                    return byName[rift.adventureName.lowercase()]
                        ?: AdventureZone(
                            zoneName = "Shadow Rift",
                            urlParams = "place=shadow_rift",
                            locationName = rift.adventureName,
                            environment = "outdoor",
                            diffLevel = "mid",
                            statRequirement = 0,
                            goals = emptyList(),
                            isOverdrunk = false,
                            noWander = true,
                        )
                }
            }
            // manor4 chamber aliases
            if (url.contains("manor4_chamber", ignoreCase = true)) {
                return byName["summoning chamber"]
                    ?: getByUrlKey("place.php?whichplace=manor4_chamberboss")
            }
            // nstower
            if (url.contains("whichplace=nstower", ignoreCase = true)) {
                val action = Regex("""action=([^&]+)""", RegexOption.IGNORE_CASE)
                    .find(url)?.groupValues?.getOrNull(1)
                if (action != null) {
                    getByUrlKey("place.php?whichplace=$action")?.let { return it }
                    return zones.firstOrNull {
                        it.formSource == "place.php" && it.adventureId.equals(action, ignoreCase = true)
                    }
                }
            }
        }

        byUrl[url.lowercase()]?.let { return it }
        // Fallback: formSource?key=id from adventures.txt col2
        val form = url.substringBefore('?').lowercase()
        val query = url.substringAfter('?', "")
        if (query.isNotEmpty()) {
            byUrl["$form?$query".lowercase()]?.let { return it }
        }
        return null
    }

    private fun getByUrlKey(key: String): AdventureZone? = byUrl[key.lowercase()]

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
        registerZone(zone)
        loaded = true
    }

    /** Test hook — reset singleton state. */
    internal fun resetForTest() {
        byName.clear()
        bySnarfblat.clear()
        byUrl.clear()
        zones.clear()
        loaded = false
    }

    internal fun computeWaterLevel(
        environment: String,
        statRequirement: Int,
        explicitLevel: Int?,
    ): Int {
        if (explicitLevel != null) return explicitLevel
        var waterLevel = when (environment) {
            "outdoor", "none" -> 1
            "indoor" -> 3
            "underground" -> 5
            else -> 1
        }
        if (statRequirement >= 40) waterLevel++
        if (environment == "underwater") waterLevel = 0
        return waterLevel
    }
}
