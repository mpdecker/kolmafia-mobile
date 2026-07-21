package net.sourceforge.kolmafia.data

import net.sourceforge.kolmafia.shared.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi

/**
 * Minimal cargo-cult pocket data from [cultshorts.txt].
 * Mirrors desktop [PocketDatabase.scrapSyllables] ordering for Yeg demon name assembly.
 */
@OptIn(ExperimentalResourceApi::class)
object CultShortsDatabase {

    private var scrapPocketsInOrderList = listOf<Int>()
    private var loaded = false

    val scrapPocketsInOrder: List<Int> get() = scrapPocketsInOrderList
    val isLoaded: Boolean get() = loaded

    suspend fun load() {
        if (loaded) return
        val text = Res.readBytes("files/data/cultshorts.txt").decodeToString()
        applyParse(parse(text))
        loaded = true
    }

    internal fun parseForTest(text: String): List<Int> = parse(text)

    internal fun injectForTest(pockets: List<Int>) {
        scrapPocketsInOrderList = pockets
        loaded = true
    }

    internal fun resetForTest() {
        scrapPocketsInOrderList = emptyList()
        loaded = false
    }

    private fun applyParse(pockets: List<Int>) {
        scrapPocketsInOrderList = pockets
    }

    private fun parse(text: String): List<Int> {
        val scraps = mutableListOf<Pair<Int, Int>>()
        for (line in text.lines()) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
            val parts = trimmed.split('\t')
            if (parts.size < 3) continue
            if (!parts[1].equals("Scrap", ignoreCase = true)) continue
            val pocket = parts[0].toIntOrNull() ?: continue
            val index = parts[2].toIntOrNull() ?: continue
            if (pocket in 1..666) {
                scraps += pocket to index
            }
        }
        return scraps.sortedBy { it.second }.map { it.first }
    }
}
