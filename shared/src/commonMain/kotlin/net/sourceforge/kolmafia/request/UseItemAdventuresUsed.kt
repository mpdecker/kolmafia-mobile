package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop `UseItemRequest.getAdventuresUsed`: items that can open a fight or a choice count as
 * turns. The deck uses the drawn card. The drum machine is free with worm-riding hooks. The gong
 * costs three turns on the roachform choice.
 */
object UseItemAdventuresUsed {
    const val CHATEAU_WATERCOLOR = 8033
    const val GOD_LOBSTER = 9661
    const val WITCHESS_SET = 8989
    const val REPLICA_WITCHESS_SET = 11233
    const val DRUM_MACHINE = 2328
    const val GONG = 3353
    const val D4 = 5285
    const val D10 = 5288
    const val WORM_RIDING_HOOKS = 2302

    private val FIGHT_OR_CHOICE = setOf(
        6782, 9558, 8599, 2338, 2950, 8033, 9908, 6677, 3034, 8382,
        3997, 11972, 6388, 2951, 9529, 9561, 8392, 9661, 7080, 7204,
        9184, 11644, 4873, 9537, 5564, 11230, 11234, 9023, 4170, 7176,
        6412, 3667, 11352, 11354, 11353, 9104, 5704, 7555, 8989, 7739, 7742,
        4509, 5171, 5172, 5307,
    )

    private val WHICH_ITEM = Regex("""whichitem=(\d+)""")
    private val QUANTITY = Regex("""quantity=(\d+)""")

    fun getAdventuresUsed(
        url: String,
        preferences: Preferences? = null,
        ownsItem: (Int) -> Boolean = { false },
        equippedWeaponId: Int = -1,
    ): Int {
        val path = requestPath(url)
        val resolved = when {
            path.contains("action=chateau_painting") -> CHATEAU_WATERCOLOR to 1
            path.contains("fightgodlobster=1") -> GOD_LOBSTER to 1
            path.contains("action=witchess") -> WITCHESS_SET to 1
            else -> extractItem(path) ?: return 0
        }
        val (itemId, count) = resolved
        if (itemId == DeckOfEveryCardRequest.DECK_ID || itemId == DeckOfEveryCardRequest.REPLICA_DECK_ID) {
            return DeckOfEveryCardRequest.getAdventuresUsed(path)
        }
        return byItem(itemId, count, preferences, ownsItem, equippedWeaponId)
    }

    /**
     * Desktop `UseItemRequest.getAdventuresUsed()` on the request itself. Chateau painting,
     * God Lobster, and Witchess are proxies here and cost no turns until their action URL.
     */
    fun forItem(
        itemId: Int,
        count: Int,
        preferences: Preferences? = null,
        ownsItem: (Int) -> Boolean = { false },
        equippedWeaponId: Int = -1,
    ): Int {
        if (itemId == CHATEAU_WATERCOLOR ||
            itemId == GOD_LOBSTER ||
            itemId == WITCHESS_SET ||
            itemId == REPLICA_WITCHESS_SET
        ) {
            return 0
        }
        return byItem(itemId, count, preferences, ownsItem, equippedWeaponId)
    }

    private fun byItem(
        itemId: Int,
        count: Int,
        preferences: Preferences?,
        ownsItem: (Int) -> Boolean,
        equippedWeaponId: Int,
    ): Int {
        val turns = when (itemId) {
            DRUM_MACHINE -> {
                if (ownsItem(WORM_RIDING_HOOKS) || equippedWeaponId == WORM_RIDING_HOOKS) {
                    return 0
                }
                1
            }
            GONG -> if ((preferences?.getInt("choiceAdventure276", 0) ?: 0) == 1) 3 else 0
            D4 -> if (count == 100) 1 else 0
            D10 -> if (count == 1 || count == 2) 1 else 0
            in FIGHT_OR_CHOICE -> 1
            else -> 0
        }
        return turns * count
    }

    private fun extractItem(path: String): Pair<Int, Int>? {
        val inventory = path.startsWith("inventory.php")
        val allowed = path.startsWith("inv_use.php") ||
            path.startsWith("inv_eat.php") ||
            path.startsWith("inv_booze.php") ||
            path.startsWith("inv_spleen.php") ||
            path.startsWith("multiuse.php") ||
            path.startsWith("inv_familiar.php") ||
            (inventory &&
                !path.contains("action=ghost") &&
                !path.contains("action=hobo") &&
                !path.contains("action=slime") &&
                !path.contains("action=breakbricko") &&
                !path.contains("action=candy"))
        if (!allowed) return null
        val itemId = WHICH_ITEM.find(path)?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val count = if (
            path.startsWith("multiuse.php") ||
            path.startsWith("inv_eat.php") ||
            path.startsWith("inv_booze.php") ||
            path.startsWith("inv_spleen.php") ||
            path.startsWith("inv_use.php")
        ) {
            QUANTITY.find(path)?.groupValues?.get(1)?.toIntOrNull() ?: 1
        } else {
            1
        }
        return itemId to count
    }

    private fun requestPath(url: String): String {
        val afterScheme = url.substringAfter("://", url)
        val path = if (afterScheme == url) url else afterScheme.substringAfter('/')
        return path.removePrefix("/")
    }
}
