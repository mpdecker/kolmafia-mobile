package net.sourceforge.kolmafia.shop

/**
 * Shared coinmaster accessibility callbacks for ASH validate / concoction / manager paths
 * (HTTP Residual LIII Track A).
 */
data class CoinmasterAccessContext(
    val accessibleCount: (Int) -> Int = { 0 },
    val hasEffect: (Int) -> Boolean = { false },
    val ownsFamiliar: (Int) -> Boolean = { false },
    val adventureUnderwater: Boolean = false,
    val underwaterFamiliar: Boolean = false,
    val generatorQuestFinished: Boolean = false,
) {
    companion object {
        val EMPTY = CoinmasterAccessContext()
    }
}
