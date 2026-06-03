package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

object SkillDefinitionDatabase {
    private val _byId = mutableMapOf<Int, SkillDefinition>()
    private val _byName = mutableMapOf<String, SkillDefinition>()
    private var loaded = false

    val byId: Map<Int, SkillDefinition> get() = _byId
    val byName: Map<String, SkillDefinition> get() = _byName

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load() {
        if (loaded) return

        val text = Res.readBytes("files/data/classskills.txt").decodeToString()
        var versionSkipped = false

        for (raw in text.lineSequence()) {
            val line = raw.trim()
            if (line.isBlank() || line.startsWith("#")) continue

            if (!versionSkipped && line.count { it == '\t' } < 2) {
                versionSkipped = true
                continue
            }

            val parts = line.split("\t")
            if (parts.size < 6) continue

            val id = parts[0].trim().toIntOrNull() ?: continue
            val name = parts[1].trim()
            if (name.isEmpty()) continue
            val image = parts[2].trim()
            val tags = parts[3].trim()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .toSet()
            val mpCost = parts[4].trim().toIntOrNull() ?: 0
            val duration = parts[5].trim().toIntOrNull() ?: 0

            var isPermable = true
            for (i in 6 until parts.size) {
                val attr = parts[i].trim()
                if (attr.startsWith("Permable:")) {
                    val value = attr.removePrefix("Permable:").trim()
                    isPermable = value.lowercase() != "false"
                }
            }

            val skill = SkillDefinition(
                id = id,
                name = name,
                image = image,
                tags = tags,
                mpCost = mpCost,
                duration = duration,
                isPermable = isPermable,
                isPassive = "passive" in tags,
                isCombat = "combat" in tags,
                isNonCombat = "nc" in tags,
                isSong = "song" in tags
            )

            _byId[id] = skill
            _byName[name.lowercase()] = skill
        }

        loaded = true
    }

    fun getById(id: Int): SkillDefinition? = _byId[id]

    fun getByName(name: String): SkillDefinition? = _byName[name.lowercase()]

    fun all(): Collection<SkillDefinition> = _byId.values

    fun combatSkills(): List<SkillDefinition> = _byId.values.filter { it.isCombat }

    fun passiveSkills(): List<SkillDefinition> = _byId.values.filter { it.isPassive }

    fun songs(): List<SkillDefinition> = _byId.values.filter { it.isSong }
}
