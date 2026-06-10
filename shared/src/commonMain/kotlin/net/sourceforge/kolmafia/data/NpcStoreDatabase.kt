package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

@OptIn(ExperimentalResourceApi::class)
object NpcStoreDatabase {
    private val _byKey   = mutableMapOf<String, NpcStoreData>()
    private val _byName  = mutableMapOf<String, NpcStoreData>()
    private val _itemPrices = mutableMapOf<String, Int>()
    private val _byItemName = mutableMapOf<String, NpcStoreData>()
    private var loaded = false

    val byKey:  Map<String, NpcStoreData> get() = _byKey
    val byName: Map<String, NpcStoreData> get() = _byName

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/npcstores.txt").decodeToString()
        loadFromText(text)
    }

    internal fun loadFromText(text: String) {
        _byKey.clear()
        _byName.clear()
        _itemPrices.clear()
        _byItemName.clear()

        val storeItems = mutableMapOf<String, MutableList<NpcStoreItem>>()

        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            if (!line.contains('\t')) continue   // skip version number line

            val parts = line.split('\t')
            if (parts.size < 4) continue

            val storeName = parts[0].trim()
            val storeKey  = parts[1].trim()
            val itemName  = parts[2].trim()
            val price     = parts[3].trim().toIntOrNull() ?: continue

            if (!_byKey.containsKey(storeKey.lowercase())) {
                val entry = NpcStoreData(storeKey = storeKey, storeName = storeName, storeType = "NPC")
                _byKey[storeKey.lowercase()]   = entry
                _byName[storeName.lowercase()] = entry
            }

            storeItems.getOrPut(storeKey.lowercase()) { mutableListOf() }
                .add(NpcStoreItem(itemName, price))

            _itemPrices.putIfAbsent(itemName.lowercase(), price)
        }

        // Attach item lists to store entries
        storeItems.forEach { (key, items) ->
            _byKey[key]?.let { existing ->
                val updated = existing.copy(items = items)
                _byKey[key] = updated
                _byName[existing.storeName.lowercase()] = updated
            }
        }

        // Build item-name → store index after all stores are fully populated
        _byKey.values.forEach { store ->
            store.items.forEach { item ->
                _byItemName.putIfAbsent(item.itemName.lowercase(), store)
            }
        }

        loaded = true
    }

    fun npcPrice(itemName: String): Int = _itemPrices[itemName.lowercase()] ?: 0

    fun storeForItem(itemName: String): NpcStoreData? = _byItemName[itemName.lowercase()]

    fun getByKey(key: String): NpcStoreData? = _byKey[key.lowercase()]
    fun getByName(name: String): NpcStoreData? = _byName[name.lowercase()]
    fun all(): List<NpcStoreData> = _byKey.values.toList()
    fun npcStores(): List<NpcStoreData> = _byKey.values.filter { it.isNpc }
    fun coinmasters(): List<NpcStoreData> = _byKey.values.filter { it.isCoinmaster }

    internal fun resetForTest() {
        _byKey.clear()
        _byName.clear()
        _itemPrices.clear()
        _byItemName.clear()
        loaded = false
    }
}
