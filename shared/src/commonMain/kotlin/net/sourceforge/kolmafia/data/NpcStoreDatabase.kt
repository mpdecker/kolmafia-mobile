package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
object NpcStoreDatabase {
    private val _byKey = mutableMapOf<String, NpcStoreData>()
    private val _byName = mutableMapOf<String, NpcStoreData>()
    private var loaded = false

    val byKey: Map<String, NpcStoreData> get() = _byKey
    val byName: Map<String, NpcStoreData> get() = _byName

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/npcstores.txt").decodeToString()
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t') && line.toIntOrNull() != null) continue
            val parts = line.split('\t')
            if (parts.size < 3) continue
            val storeKey = parts[0].trim()
            val storeName = parts[1].trim()
            val storeType = parts[2].trim()
            val entry = NpcStoreData(storeKey, storeName, storeType)
            _byKey[storeKey.lowercase()] = entry
            _byName[storeName.lowercase()] = entry
        }
        loaded = true
    }

    fun getByKey(key: String): NpcStoreData? = _byKey[key.lowercase()]

    fun getByName(name: String): NpcStoreData? = _byName[name.lowercase()]

    fun all(): List<NpcStoreData> = _byKey.values.toList()

    fun npcStores(): List<NpcStoreData> = _byKey.values.filter { it.isNpc }

    fun coinmasters(): List<NpcStoreData> = _byKey.values.filter { it.isCoinmaster }
}
