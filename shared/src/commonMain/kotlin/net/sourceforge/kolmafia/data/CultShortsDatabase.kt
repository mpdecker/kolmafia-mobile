package net.sourceforge.kolmafia.data

/**
 * Minimal cargo-cult scrap pocket ordering facade over [PocketDatabase].
 * Mirrors desktop [PocketDatabase.scrapSyllables] ordering for Yeg demon name assembly.
 */
object CultShortsDatabase {

    private var scrapPocketsInOrderList = listOf<Int>()
    private var loaded = false

    val scrapPocketsInOrder: List<Int> get() = scrapPocketsInOrderList
    val isLoaded: Boolean get() = loaded

    suspend fun load() {
        if (loaded) return
        PocketDatabase.load()
        syncScrapOrder()
        loaded = true
    }

    internal fun parseForTest(text: String): List<Int> {
        val result = PocketDatabase.parseForTest(text)
        return result.pockets
            .filterIsInstance<PocketDatabase.ScrapPocket>()
            .sortedBy { it.scrap }
            .map { it.pocket }
    }

    internal fun injectForTest(pockets: List<Int>) {
        scrapPocketsInOrderList = pockets
        loaded = true
    }

    internal fun resetForTest() {
        scrapPocketsInOrderList = emptyList()
        loaded = false
    }

    private fun syncScrapOrder() {
        scrapPocketsInOrderList = PocketDatabase.scrapSyllables.map { it.pocket }
    }
}
