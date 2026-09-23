package net.sourceforge.kolmafia.data

/**
 * Desktop [net.sourceforge.kolmafia.objectpool.Concoction.getYield] /
 * [net.sourceforge.kolmafia.persistence.ConcoctionDatabase.getYield].
 *
 * Base yield is the concoctions.txt result count (`resultQuantity` / `craftYield`).
 * Saucerors (`tripleReagent`) make 3× `SX3` reagent potions per craft.
 */
object ConcoctionYield {
    fun getYield(concoction: ConcoctionData, tripleReagent: Boolean = false): Int {
        val base = maxOf(concoction.resultQuantity, concoction.craftYield, 1)
        return if (tripleReagent && concoction.isTripleSauce) 3 * base else base
    }
}
