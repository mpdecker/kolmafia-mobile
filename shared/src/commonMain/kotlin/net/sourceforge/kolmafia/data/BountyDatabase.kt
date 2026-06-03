package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

object BountyDatabase {
    private val _byName = mutableMapOf<String, BountyData>()
    private val _easy = mutableListOf<BountyData>()
    private val _hard = mutableListOf<BountyData>()
    private val _special = mutableListOf<BountyData>()
    private var loaded = false

    val byName: Map<String, BountyData> get() = _byName

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load() {
        if (loaded) return

        val text = Res.readBytes("files/data/bounty.txt").decodeToString()
        var versionSkipped = false

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isBlank() || line.startsWith("#")) continue

            if (!versionSkipped && line.count { it == '\t' } < 2) {
                versionSkipped = true
                continue
            }

            val parts = line.split("\t")
            if (parts.size < 7) continue

            val name = parts[0].trim()
            if (name.isEmpty()) continue
            val plural = parts[1].trim()
            val typeStr = parts[2].trim().lowercase()
            val image = parts[3].trim()
            val count = parts[4].trim().toIntOrNull() ?: 0
            val monster = parts[5].trim()
            val bestLocation = parts[6].trim()

            val type = when (typeStr) {
                "easy" -> BountyType.EASY
                "hard" -> BountyType.HARD
                "special" -> BountyType.SPECIAL
                else -> BountyType.UNKNOWN
            }

            val bounty = BountyData(
                name = name,
                plural = plural,
                type = type,
                image = image,
                count = count,
                monster = monster,
                bestLocation = bestLocation
            )

            _byName[name.lowercase()] = bounty
            when (type) {
                BountyType.EASY -> _easy.add(bounty)
                BountyType.HARD -> _hard.add(bounty)
                BountyType.SPECIAL -> _special.add(bounty)
                BountyType.UNKNOWN -> Unit
            }
        }

        loaded = true
    }

    fun getByName(name: String): BountyData? = _byName[name.lowercase()]

    fun all(): Collection<BountyData> = _byName.values

    fun easy(): List<BountyData> = _easy

    fun hard(): List<BountyData> = _hard

    fun special(): List<BountyData> = _special

    fun forMonster(monsterName: String): List<BountyData> {
        val lower = monsterName.lowercase()
        return _byName.values.filter { it.monster.lowercase() == lower }
    }
}
