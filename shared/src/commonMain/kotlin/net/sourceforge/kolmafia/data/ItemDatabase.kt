package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

// Parses items.txt from the bundled compose resources.
// Format (tab-separated): id  name  descid  image  use  access  autosell  [plural]
// Call load() once at app startup (or lazily on first access).
@OptIn(ExperimentalResourceApi::class)
object ItemDatabase {

    private val byId = mutableMapOf<Int, ItemData>()
    private val byName = mutableMapOf<String, ItemData>()
    private val byPlural = mutableMapOf<String, ItemData>()
    private val byDescId = mutableMapOf<String, ItemData>()
    private var loaded = false

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/items.txt").decodeToString()
        parse(text)
        loaded = true
    }

    fun getById(id: Int): ItemData? = byId[id]
    fun getByName(name: String): ItemData? = byName[name.lowercase()]
    fun getByDescId(descId: String): ItemData? = byDescId[descId]
    fun getByPluralOrName(name: String): ItemData? {
        val lower = name.lowercase()
        return byName[lower] ?: byPlural[lower]
    }
    fun all(): Collection<ItemData> = byId.values

    fun isTradeable(itemId: Int): Boolean = getById(itemId)?.isTradeable ?: false

    fun isGiftable(itemId: Int): Boolean = getById(itemId)?.isGiftable ?: false

    fun isDiscardable(itemId: Int): Boolean = getById(itemId)?.isDiscardable ?: false

    fun isQuestItem(itemId: Int): Boolean = getById(itemId)?.isQuestItem ?: false

    fun isDisplayable(itemId: Int): Boolean =
        itemId > 0 && !isQuestItem(itemId) && !isVirtualItem(itemId)

    /** Virtual items exist in KoL data but cannot live in inventory (desktop ItemDatabase.isVirtualItem). */
    fun isVirtualItem(itemId: Int): Boolean = itemId in VIRTUAL_ITEM_IDS

    fun getPluralName(itemId: Int): String {
        if (itemId <= 0) return ""
        val item = getById(itemId) ?: return ""
        val plural = item.plural?.takeIf { it.isNotBlank() }
        return plural ?: "${item.name}s"
    }

    /** Test hook — register an item without loading items.txt. */
    internal fun registerForTest(item: ItemData) {
        byId[item.id] = item
        byName[item.name.lowercase()] = item
        byDescId[item.descId] = item
        item.plural?.let { byPlural[it.lowercase()] = item }
    }

    internal fun resetForTest() {
        byId.clear()
        byName.clear()
        byPlural.clear()
        byDescId.clear()
        loaded = false
    }

    private fun parse(text: String) {
        for (raw in text.lines()) {
            val line = raw.trim()
            if (line.isEmpty() || line.startsWith("#")) continue
            // Skip version line (first non-comment line is just a number)
            val firstToken = line.substringBefore('\t')
            if (firstToken.toIntOrNull() != null && line.count { it == '\t' } < 3) continue

            val parts = line.split('\t')
            if (parts.size < 7) continue

            val id = parts[0].toIntOrNull() ?: continue
            val name = parts[1]
            val descId = parts[2]
            val image = parts[3]
            val useParts = parts[4].split(',')
            val primaryUse = ItemPrimaryUse.fromString(useParts.firstOrNull()?.trim() ?: "none")
            val secondaryUses = useParts.drop(1).map { it.trim() }.filter { it.isNotEmpty() }.toSet()
            val accessStr = parts[5]
            val access = accessStr.split(',').mapNotNull { it.trim().firstOrNull() }.toSet()
            val autosell = parts[6].toIntOrNull() ?: 0
            val plural = parts.getOrNull(7)?.takeIf { it.isNotBlank() }

            val item = ItemData(id, name, descId, image, primaryUse, secondaryUses, access, autosell, plural)
            byId[id] = item
            byName[name.lowercase()] = item
            byDescId[descId] = item
            plural?.let { byPlural[it.lowercase()] = item }
        }
    }

    private val VIRTUAL_ITEM_IDS = setOf(
        3649, // madness reef map
        3683, // marinara trench map
        3701, // anemone mine map
        3774, // dive bar map
        4222, // skate park map
        7589, // glass of milk
        7590, // cup of tea
        7591, // thermos of whiskey
        7592, // lucky lindy
        7593, // bee's knees
        7594, // sockdolloger
        7595, // ish kabibble
        7596, // hot socks
        7597, // phonus balonus
        7598, // flivver
        7599, // sloppy jalopy
    )
}
