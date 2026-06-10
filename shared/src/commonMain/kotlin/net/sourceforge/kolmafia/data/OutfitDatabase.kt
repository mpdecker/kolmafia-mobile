package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

object OutfitDatabase {
    private val _byId = mutableMapOf<Int, OutfitData>()
    private val _byName = mutableMapOf<String, OutfitData>()
    private val _customById = mutableMapOf<Int, OutfitData>()
    private val _customByName = mutableMapOf<String, OutfitData>()
    private var loaded = false

    val byId: Map<Int, OutfitData> get() = _byId + _customById
    val byName: Map<String, OutfitData> get() = _byName + _customByName

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

    fun getById(id: Int): OutfitData? = _byId[id] ?: _customById[id]

    fun getByName(name: String): OutfitData? {
        val lower = name.lowercase()
        return _byName[lower] ?: _customByName[lower]
    }

    fun all(): Collection<OutfitData> = _byId.values

    fun customOutfits(): Collection<OutfitData> = _customById.values

    fun allOutfits(): Collection<OutfitData> = _byId.values + _customById.values

    fun registerCustom(outfit: OutfitData) {
        require(outfit.id < 0) { "Custom outfit ids must be negative" }
        _customById[outfit.id] = outfit
        _customByName[outfit.name.lowercase()] = outfit
    }

    fun clearCustom() {
        _customById.clear()
        _customByName.clear()
    }

    /** Registers a static outfit entry (for tests). */
    fun registerStatic(outfit: OutfitData) {
        require(outfit.id > 0) { "Static outfit ids must be positive" }
        _byId[outfit.id] = outfit
        _byName[outfit.name.lowercase()] = outfit
    }

    fun findByPiece(itemName: String): OutfitData? {
        val lower = itemName.lowercase()
        return _byId.values.firstOrNull { outfit ->
            outfit.equipment.any { it.lowercase() == lower }
        }
    }
}
