package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.EquipmentDatabase

/**
 * Desktop [UseItemRequest.registerBingeRequest] session-log lines
 * (Phases 8171–8230).
 */
object UseItemBingeLog {
    const val HOBO = 52
    const val GHOST = 74
    const val SLIMELING = 112
    const val STOCKING_MIMIC = 120
    const val GNOLLISH_AUTOPLUNGER = 127

    private val BINGE_FAMILIARS = setOf(HOBO, GHOST, SLIMELING, STOCKING_MIMIC)
    private val WHICHITEM = Regex("""whichitem=(\d+)""", RegexOption.IGNORE_CASE)
    private val QTY = Regex("""qty=(\d+)""", RegexOption.IGNORE_CASE)

    fun line(
        url: String,
        familiarId: Int,
        familiarRace: String,
        itemName: (Int) -> String = { id -> net.sourceforge.kolmafia.data.ItemDatabase.getItemName(id) },
        itemPower: (Int) -> Int = { id -> EquipmentDatabase.getPower(id) },
        usesMeatStack: (Int) -> Boolean = { id -> ConcoctionDatabase.usesMeatStackIngredient(id) },
    ): String? {
        if (familiarId !in BINGE_FAMILIARS) return null
        if (!isBingeUrl(url)) return null
        val itemId = WHICHITEM.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
        val count = QTY.find(url)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
        val name = itemName(itemId).ifBlank { "item #$itemId" }
        val race = familiarRace.ifBlank { fallbackRace(familiarId) }
        var text = "feed $count $name to $race"
        if (familiarId == SLIMELING) {
            text += if (itemId == GNOLLISH_AUTOPLUNGER || usesMeatStack(itemId)) {
                " ($count more slime stack(s) due)"
            } else {
                val charges = count * itemPower(itemId) / 10.0f
                " (estimated $charges charges)"
            }
        }
        return text
    }

    private fun isBingeUrl(url: String): Boolean {
        if (url.startsWith("familiarbinger.php")) return true
        if (!url.startsWith("inventory.php")) return false
        return url.contains("action=ghost") ||
            url.contains("action=hobo") ||
            url.contains("action=slime") ||
            url.contains("action=candy")
    }

    private fun fallbackRace(familiarId: Int): String = when (familiarId) {
        HOBO -> "Spirit Hobo"
        GHOST -> "Gluttonous Green Ghost"
        SLIMELING -> "Slimeling"
        STOCKING_MIMIC -> "Stocking Mimic"
        else -> ""
    }
}
