package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

object OutfitDatabase {
    private val _byId = mutableMapOf<Int, OutfitData>()
    private val _byName = mutableMapOf<String, OutfitData>()
    private var loaded = false

    val byId: Map<Int, OutfitData> get() = _byId
    val byName: Map<String, OutfitData> get() = _byName

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load() {
        if (loaded) return

        val text = Res.readBytes("files/data/outfits.txt").decodeToString()
        var versionSkipped = false

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isBlank() || line.startsWith("#")) continue

            if (!versionSkipped && line.count { it == '\t' } < 2) {
                versionSkipped = true
                continue
            }

            val parts = line.split("\t")
            if (parts.size < 3) continue

            val id = parts[0].trim().toIntOrNull() ?: continue
            val name = parts[1].trim()
            if (name.isEmpty()) continue
            val image = parts[2].trim()

            val equipment = if (parts.size > 3 && parts[3].trim().isNotEmpty()) {
                parts[3].trim()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            } else {
                emptyList()
            }

            val halloweenDrops = if (parts.size > 4 && parts[4].trim().isNotEmpty()) {
                parts[4].trim()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            } else {
                emptyList()
            }

            val outfit = OutfitData(
                id = id,
                name = name,
                image = image,
                equipment = equipment,
                halloweenDrops = halloweenDrops
            )

            _byId[id] = outfit
            _byName[name.lowercase()] = outfit
        }

        loaded = true
    }

    fun getById(id: Int): OutfitData? = _byId[id]

    fun getByName(name: String): OutfitData? = _byName[name.lowercase()]

    fun all(): Collection<OutfitData> = _byId.values

    fun findByPiece(itemName: String): OutfitData? {
        val lower = itemName.lowercase()
        return _byId.values.firstOrNull { outfit ->
            outfit.equipment.any { it.lowercase() == lower }
        }
    }
}
