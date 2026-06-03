package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
object PackageDatabase {
    private val _byName = mutableMapOf<String, PackageData>()
    private val _byContainedItemId = mutableMapOf<Int, PackageData>()
    private var loaded = false

    val byName: Map<String, PackageData> get() = _byName
    val byContainedItemId: Map<Int, PackageData> get() = _byContainedItemId

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/packages.txt").decodeToString()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            val parts = line.split('\t')
            if (parts.size < 4) continue
            val name = parts[0].trim()
            val containedItemId = parts[1].trim().toIntOrNull() ?: continue
            val count = parts[2].trim().toIntOrNull() ?: continue
            val meatValue = parts[3].trim().toIntOrNull() ?: 0
            val entry = PackageData(name, containedItemId, count, meatValue)
            _byName[name.lowercase()] = entry
            _byContainedItemId[containedItemId] = entry
        }
        loaded = true
    }

    fun getByName(name: String): PackageData? = _byName[name.lowercase()]

    fun getByContainedItemId(id: Int): PackageData? = _byContainedItemId[id]

    fun all(): List<PackageData> = _byName.values.toList()
}
