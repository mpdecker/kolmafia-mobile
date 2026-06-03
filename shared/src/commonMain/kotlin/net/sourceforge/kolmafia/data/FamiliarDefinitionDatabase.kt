package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

object FamiliarDefinitionDatabase {
    private val _byId = mutableMapOf<Int, FamiliarDefinition>()
    private val _byName = mutableMapOf<String, FamiliarDefinition>()
    private var loaded = false

    val byId: Map<Int, FamiliarDefinition> get() = _byId
    val byName: Map<String, FamiliarDefinition> get() = _byName

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load() {
        if (loaded) return

        val text = Res.readBytes("files/data/familiars.txt").decodeToString()
        var versionSkipped = false

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isBlank() || line.startsWith("#")) continue

            if (!versionSkipped && line.count { it == '\t' } < 2) {
                versionSkipped = true
                continue
            }

            val parts = line.split("\t")
            if (parts.size < 10) continue

            val id = parts[0].trim().toIntOrNull() ?: continue
            val name = parts[1].trim()
            if (name.isEmpty()) continue
            val image = parts[2].trim()
            val types = parts[3].trim()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
            val larvaItem = parts[4].trim()
            val hatchlingItem = parts[5].trim()
            val cm = parts[6].trim().toIntOrNull() ?: 0
            val sh = parts[7].trim().toIntOrNull() ?: 0
            val oc = parts[8].trim().toIntOrNull() ?: 0
            val hs = parts[9].trim().toIntOrNull() ?: 0
            val attributes = if (parts.size > 10) {
                parts[10].trim()
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toSet()
            } else {
                emptySet()
            }

            val familiar = FamiliarDefinition(
                id = id,
                name = name,
                image = image,
                types = types,
                larvaItem = larvaItem,
                hatchlingItem = hatchlingItem,
                arenaCombatMoves = cm,
                arenaStrength = sh,
                arenaOc = oc,
                arenaHs = hs,
                attributes = attributes
            )

            _byId[id] = familiar
            _byName[name.lowercase()] = familiar
        }

        loaded = true
    }

    fun getById(id: Int): FamiliarDefinition? = _byId[id]

    fun getByName(name: String): FamiliarDefinition? = _byName[name.lowercase()]

    fun all(): Collection<FamiliarDefinition> = _byId.values

    fun withType(typeCode: String): List<FamiliarDefinition> =
        _byId.values.filter { typeCode in it.types }

    fun withAttribute(attr: String): List<FamiliarDefinition> =
        _byId.values.filter { attr in it.attributes }
}
