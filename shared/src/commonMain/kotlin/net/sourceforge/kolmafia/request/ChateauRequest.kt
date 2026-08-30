package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.adventure.choice.ItemPool
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [net.sourceforge.kolmafia.request.ChateauRequest] — GIF furniture inventory
 * backing live `get_chateau`.
 */
object ChateauRequest {
    const val CHATEAU_MUSCLE = ItemPool.CHATEAU_MUSCLE
    const val CHATEAU_MYST = ItemPool.CHATEAU_MYST
    const val CHATEAU_MOXIE = ItemPool.CHATEAU_MOXIE
    const val CHATEAU_FAN = ItemPool.CHATEAU_FAN
    const val CHATEAU_CHANDELIER = ItemPool.CHATEAU_CHANDELIER
    const val CHATEAU_SKYLIGHT = ItemPool.CHATEAU_SKYLIGHT
    const val CHATEAU_BANK = ItemPool.CHATEAU_BANK
    const val CHATEAU_JUICE_BAR = ItemPool.CHATEAU_JUICE_BAR
    const val CHATEAU_PENS = ItemPool.CHATEAU_PENS
    const val CHATEAU_WATERCOLOR = ItemPool.CHATEAU_WATERCOLOR

    private val PAINTING = Regex("""Painting of a[n]? (.*?) \(1\)" title""")

    private val furniture = linkedSetOf<Int>()

    fun reset() {
        furniture.clear()
    }

    fun furnitureIds(): Set<Int> = furniture.toSet()

    fun parseFurniture(html: String, preferences: Preferences? = null) {
        reset()
        val painting = PAINTING.find(html)?.groupValues?.getOrNull(1)?.trim()
        if (painting != null) {
            preferences?.setString("chateauMonster", painting)
        } else if (html.contains("No Painting", ignoreCase = true)) {
            preferences?.setString("chateauMonster", "")
        }

        when {
            html.contains("nightstand_mus.gif") -> furniture.add(CHATEAU_MUSCLE)
            html.contains("nightstand_mag.gif") -> furniture.add(CHATEAU_MYST)
            html.contains("nightstand_moxie.gif") -> furniture.add(CHATEAU_MOXIE)
        }
        when {
            html.contains("ceilingfan.gif") -> furniture.add(CHATEAU_FAN)
            html.contains("chandelier.gif") -> furniture.add(CHATEAU_CHANDELIER)
            html.contains("skylight.gif") -> furniture.add(CHATEAU_SKYLIGHT)
        }
        when {
            html.contains("desk_bank.gif") -> furniture.add(CHATEAU_BANK)
            html.contains("desk_juice.gif") -> furniture.add(CHATEAU_JUICE_BAR)
            html.contains("desk_stat.gif") -> furniture.add(CHATEAU_PENS)
        }
    }
}
