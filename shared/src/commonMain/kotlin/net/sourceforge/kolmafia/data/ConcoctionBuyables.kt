package net.sourceforge.kolmafia.data

/** Desktop calculateBasicItems / ItemPool buyable meat items. */
object ConcoctionBuyables {
    const val MEAT_PASTE = 25
    const val MEAT_STACK = 88
    const val DENSE_MEAT_STACK = 258
    const val FLAT_DOUGH = 301

    const val MEAT_PASTE_PRICE = 10
    const val MEAT_STACK_PRICE = 100
    const val DENSE_MEAT_STACK_PRICE = 1000

    fun buyablePrice(itemId: Int): Int? = when (itemId) {
        MEAT_PASTE -> MEAT_PASTE_PRICE
        MEAT_STACK -> MEAT_STACK_PRICE
        DENSE_MEAT_STACK -> DENSE_MEAT_STACK_PRICE
        else -> null
    }

    fun excludesCreatableMeatSubtract(itemId: Int): Boolean =
        itemId == MEAT_PASTE || itemId == MEAT_STACK || itemId == DENSE_MEAT_STACK
}
