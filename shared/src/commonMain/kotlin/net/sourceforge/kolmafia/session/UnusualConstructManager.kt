package net.sourceforge.kolmafia.session

import net.sourceforge.kolmafia.adventure.choice.ItemPool

/** Solves the language/color clue emitted by an Unusual Construct. */
object UnusualConstructManager {
    private var discId: Int = 0

    private val colors = mapOf(
        ItemPool.STRANGE_DISC_YELLOW to setOf("CHO", "FUNI", "TAZAK", "CANARY", "CITRINE", "GOLD"),
        ItemPool.STRANGE_DISC_RED to setOf("CHAKRO", "ZEVE", "ZEVESTANO", "CRIMSON", "RUBY", "VERMILLION"),
        ItemPool.STRANGE_DISC_BLACK to setOf("BUPABU", "PATA", "SOM", "OBSIDIAN", "EBONY", "JET"),
        ItemPool.STRANGE_DISC_GREEN to setOf("BE", "ZAKSOM", "ZEVEBENI", "JADE", "VERDIGRIS", "EMERALD"),
        ItemPool.STRANGE_DISC_BLUE to setOf("BELA", "BULAZAK", "BU", "FUFUGAKRO", "ULTRAMARINE", "SAPPHIRE", "COBALT"),
        ItemPool.STRANGE_DISC_WHITE to setOf("NIPA", "PACHA", "SOMPAPA", "IVORY", "ALABASTER", "PEARL"),
    )
    private val clue = Regex("""(?:LANO|ROUTING)\s+([A-Za-z]+)""")

    fun disc(): Int = discId

    fun solve(responseText: String?): Boolean {
        discId = 0
        val word = responseText?.let { clue.find(it)?.groupValues?.getOrNull(1) } ?: return false
        discId = colors.entries.firstOrNull { word.uppercase() in it.value }?.key ?: 0
        return discId != 0
    }
}
