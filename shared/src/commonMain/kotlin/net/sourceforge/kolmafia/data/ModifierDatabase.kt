package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
object ModifierDatabase {
    private val _byTypeAndName = mutableMapOf<String, MutableMap<String, ModifierEntry>>()
    private val _allByName = mutableMapOf<String, MutableList<ModifierEntry>>()
    private var loaded = false

    val byTypeAndName: Map<String, Map<String, ModifierEntry>> get() = _byTypeAndName
    val allByName: Map<String, List<ModifierEntry>> get() = _allByName

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/modifiers.txt").decodeToString()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue
            val parts = line.split('\t')
            if (parts.size < 3) continue
            val entityType = parts[0].trim()
            val name = parts[1].trim()
            val modifiers = parts[2].trim()
            if (entityType.isEmpty() || name.isEmpty()) continue
            val entry = ModifierEntry(entityType, name, modifiers)
            _byTypeAndName.getOrPut(entityType) { mutableMapOf() }[name] = entry
            _allByName.getOrPut(name.lowercase()) { mutableListOf() } += entry
        }
        loaded = true
    }

    fun getItem(name: String): ModifierEntry? = get("Item", name)

    fun getEffect(name: String): ModifierEntry? = get("Effect", name)

    fun getSkill(name: String): ModifierEntry? = get("Skill", name)

    fun get(type: String, name: String): ModifierEntry? = _byTypeAndName[type]?.get(name)

    fun all(): List<ModifierEntry> = _allByName.values.flatten()
}
