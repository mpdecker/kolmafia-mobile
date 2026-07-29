package net.sourceforge.kolmafia.inventory

import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Persisted flagged-item lists (desktop [KoLConstants.junkList] / [FlaggedItems]).
 * User lists stored in preferences as pipe-delimited item names.
 */
@OptIn(ExperimentalResourceApi::class)
class JunkListManager(
    private val gameDatabase: GameDatabase,
) {
    private var itemIds: List<Int> = emptyList()
    private var idSet: Set<Int> = emptySet()
    private var singletonIds: List<Int> = emptyList()
    private var singletonIdSet: Set<Int> = emptySet()
    private var mementoIds: List<Int> = emptyList()
    private var mementoIdSet: Set<Int> = emptySet()
    private var profitableIds: List<Int> = emptyList()
    private var profitableIdSet: Set<Int> = emptySet()
    private var preferences: Preferences? = null

    fun itemIds(): List<Int> = itemIds

    fun singletonIds(): List<Int> = singletonIds

    fun mementoIds(): List<Int> = mementoIds

    fun profitableIds(): List<Int> = profitableIds

    fun contains(itemId: Int): Boolean = itemId in idSet

    fun isSingleton(itemId: Int): Boolean = itemId in singletonIdSet

    fun isMemento(itemId: Int): Boolean = itemId in mementoIdSet

    fun isProfitable(itemId: Int): Boolean = itemId in profitableIdSet

    suspend fun load(preferences: Preferences) {
        this.preferences = preferences

        if (preferences.getBoolean(ITEM_FLAGS_IMPORTED_KEY, false)) {
            loadFromImportedPrefs(preferences)
            return
        }

        val singletonNames = loadOrSeedPref(preferences, SINGLETON_PREF_KEY, loadBundledSingleton())
        val mementoNames = loadOrSeedPref(preferences, MEMENTO_PREF_KEY, loadBundledMemento())
        val junkNames = loadOrSeedPref(preferences, PREF_KEY, loadBundledJunk())
        val profitableNames = parsePrefNames(preferences, PROFITABLE_PREF_KEY)

        applyLists(junkNames, singletonNames, mementoNames, profitableNames)
    }

    fun importItemFlags(text: String) {
        val sections = ItemFlagsParser.parse(text) { name -> gameDatabase.item(name) != null }
        val prefs = preferences ?: return

        prefs.setString(PREF_KEY, sections.junk.joinToString("|"))
        prefs.setString(SINGLETON_PREF_KEY, sections.singleton.joinToString("|"))
        prefs.setString(MEMENTO_PREF_KEY, sections.memento.joinToString("|"))
        prefs.setString(PROFITABLE_PREF_KEY, sections.profitable.joinToString("|"))
        prefs.setBoolean(ITEM_FLAGS_IMPORTED_KEY, true)

        applyLists(sections.junk, sections.singleton, sections.memento, sections.profitable)
    }

    fun exportItemFlags(): String {
        val junkNames = idsToNames(itemIds)
        val singletonNames = idsToNames(singletonIds).toSet()
        val mementoNames = idsToNames(mementoIds)
        val profitableNames = idsToNames(profitableIds).map { name -> name to 1 }
        return ItemFlagsParser.export(junkNames, singletonNames, mementoNames, profitableNames)
    }

    fun addToJunkList(itemId: Int) {
        if (itemId in idSet) return
        val name = gameDatabase.item(itemId)?.name ?: return
        itemIds = itemIds + itemId
        idSet = itemIds.toSet()

        val prefs = preferences ?: return
        val current = prefs.getString(PREF_KEY, "").trim()
        val updated = if (current.isEmpty()) name else "$current|$name"
        prefs.setString(PREF_KEY, updated)
    }

    internal fun loadFromNamesForTest(names: List<String>) {
        applyJunkNames(names)
    }

    internal fun loadListsForTest(
        junkNames: List<String>,
        singletonNames: List<String> = emptyList(),
        mementoNames: List<String> = emptyList(),
        profitableNames: List<String> = emptyList(),
    ) {
        applyLists(junkNames, singletonNames, mementoNames, profitableNames)
    }

    internal fun resetForTest() {
        itemIds = emptyList()
        idSet = emptySet()
        singletonIds = emptyList()
        singletonIdSet = emptySet()
        mementoIds = emptyList()
        mementoIdSet = emptySet()
        profitableIds = emptyList()
        profitableIdSet = emptySet()
        preferences = null
    }

    private fun loadFromImportedPrefs(preferences: Preferences) {
        val junkNames = parsePrefNames(preferences, PREF_KEY)
        val singletonNames = parsePrefNames(preferences, SINGLETON_PREF_KEY)
        val mementoNames = parsePrefNames(preferences, MEMENTO_PREF_KEY)
        val profitableNames = parsePrefNames(preferences, PROFITABLE_PREF_KEY)
        applyLists(junkNames, singletonNames, mementoNames, profitableNames)
    }

    private fun applyLists(
        junkNames: List<String>,
        singletonNames: List<String>,
        mementoNames: List<String>,
        profitableNames: List<String>,
    ) {
        singletonIds = resolveIds(singletonNames)
        singletonIdSet = singletonIds.toSet()
        mementoIds = resolveIds(mementoNames)
        mementoIdSet = mementoIds.toSet()
        profitableIds = resolveIds(profitableNames)
        profitableIdSet = profitableIds.toSet()
        applyJunkNames(junkNames)
    }

    private fun loadOrSeedPref(
        preferences: Preferences,
        key: String,
        bundled: List<String>,
    ): List<String> {
        val pref = preferences.getString(key, "").trim()
        return if (pref.isEmpty()) {
            preferences.setString(key, bundled.joinToString("|"))
            bundled
        } else {
            parsePrefNames(preferences, key)
        }
    }

    private fun parsePrefNames(preferences: Preferences, key: String): List<String> =
        preferences.getString(key, "")
            .trim()
            .split('|')
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun applyJunkNames(names: List<String>) {
        val resolved = resolveIds(names).toMutableList()
        for (id in singletonIds) {
            if (id !in resolved) {
                resolved.add(id)
            }
        }
        itemIds = resolved
        idSet = resolved.toSet()
    }

    private fun resolveIds(names: List<String>): List<Int> =
        names.mapNotNull { name -> gameDatabase.item(name)?.id }

    private fun idsToNames(ids: List<Int>): List<String> =
        ids.mapNotNull { id -> gameDatabase.item(id)?.name }

    private suspend fun loadBundledJunk(): List<String> =
        loadBundledFile("files/data/common_junk.txt")

    private suspend fun loadBundledSingleton(): List<String> =
        loadBundledFile("files/data/common_singleton.txt")

    private suspend fun loadBundledMemento(): List<String> =
        loadBundledFile("files/data/common_memento.txt")

    private suspend fun loadBundledFile(path: String): List<String> {
        val text = Res.readBytes(path).decodeToString()
        return text.lines().map { it.trim() }.filter { it.isNotEmpty() }
    }

    companion object {
        const val PREF_KEY = "junkList"
        const val SINGLETON_PREF_KEY = "singletonList"
        const val MEMENTO_PREF_KEY = "mementoList"
        const val PROFITABLE_PREF_KEY = "profitableList"
        const val ITEM_FLAGS_IMPORTED_KEY = "itemFlagsImported"
    }
}
