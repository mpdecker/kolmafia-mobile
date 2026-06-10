package net.sourceforge.kolmafia.shop

object CoinmasterRegistry {

    val all: List<CoinmasterData>
        get() {
            val db = CoinmasterDatabase.all
            if (db.isEmpty()) return FALLBACK
            val dbNicks = db.flatMap { it.allNicknames }.map { it.lowercase() }.toSet()
            return db + FALLBACK.filter { fb ->
                fb.allNicknames.none { it.lowercase() in dbNicks }
            }
        }

    fun findByNickname(nickname: String): CoinmasterData? {
        val key = nickname.lowercase()
        val fromDb = CoinmasterDatabase.findByNickname(nickname)
        val fromFallback = FALLBACK.firstOrNull { it.allNicknames.any { n -> n.equals(key, ignoreCase = true) } }
        return when {
            fromDb == null -> fromFallback
            fromDb.buyItems.isEmpty() && fromFallback?.buyItems?.isNotEmpty() == true -> fromFallback
            else -> fromDb
        }
    }

    fun findByShopId(shopId: String): CoinmasterData? =
        CoinmasterDatabase.findByShopId(shopId)
            ?: all.firstOrNull { it.shopId.equals(shopId, ignoreCase = true) }

    fun findBuyRowForItem(itemId: Int): Pair<CoinmasterData, ShopRow>? =
        CoinmasterDatabase.findBuyRowForItem(itemId)
            ?: all.firstNotNullOfOrNull { master ->
                master.buyRowFor(itemId)?.let { master to it }
            }

    /** Minimal fallback when [CoinmasterDatabase] has not been loaded yet (e.g. unit tests). */
    private val FALLBACK: List<CoinmasterData> = listOf(
        CoinmasterData(
            masterName = "Dimemaster",
            nickname = "dmt",
            nicknames = listOf("dimemaster"),
            token = "dime",
            shopId = null,
            buyUrl = "bigisland.php",
            sellUrl = "bigisland.php",
            buyItems = listOf(
                ShopRow(
                    rowId = 1,
                    item = ItemStack(itemId = 8555, count = 1),
                    price = 5,
                )
            ),
            sellItems = emptyList(),
        ),
        CoinmasterData(
            masterName = "The Shore, Inc. Gift Shop",
            nickname = "shore",
            token = "Shore Inc. Ship Trip Scrip",
            shopId = "shore",
            buyItems = listOf(
                ShopRow(
                    rowId = 176,
                    item = ItemStack(itemId = 146, count = 1),
                    price = 3,
                )
            ),
            sellItems = emptyList(),
        ),
        CoinmasterData(
            masterName = "Bounty Hunter Hunter",
            nickname = "bhh",
            nicknames = listOf("hunter"),
            token = "lucre",
            shopId = null,
            buyItems = listOf(
                ShopRow(
                    rowId = 2417,
                    item = ItemStack(itemId = 2417, count = 1),
                    price = 15,
                )
            ),
            sellItems = emptyList(),
            useItemField = true,
            buyUrl = "bounty.php",
            sellUrl = "bounty.php",
        ),
    )
}
