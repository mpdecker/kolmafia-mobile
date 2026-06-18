package net.sourceforge.kolmafia.adventure

import net.sourceforge.kolmafia.character.CharacterState
import net.sourceforge.kolmafia.data.GameDatabase
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.modifiers.ModifierParser
import net.sourceforge.kolmafia.modifiers.StringModifier
import net.sourceforge.kolmafia.preferences.Preferences

object TowerDoorStatus {

    fun buildTable(
        character: CharacterState,
        preferences: Preferences,
        inventoryManager: InventoryManager?,
        gameDatabase: GameDatabase?,
        neededOnly: Boolean = false,
    ): String {
        val locks = TowerDoorConfig.locksFor(character)
        val sb = StringBuilder()
        sb.append("<table border=2 cols=4>")
        sb.append("<tr>")
        sb.append("<th>Lock</th>")
        sb.append("<th>Key</th>")
        sb.append("<th>Have/Used</th>")
        sb.append("<th>Location</th>")
        sb.append("<tr>")
        sb.append("<th colspan=4>Enchantments</th>")
        sb.append("</tr>")

        for (lock in locks) {
            if (lock.isDoorknob) continue
            val have = haveKey(lock, inventoryManager, character)
            val used = TowerDoorConfig.isKeyUsed(preferences, lock.keyName)
            if (neededOnly && (have || used)) continue

            sb.append("<tr>")
            sb.append("<td>").append(lock.name).append("</td>")
            sb.append("<td>").append(lock.keyName).append("</td>")
            sb.append("<td>")
            sb.append(if (have) "yes" else "no")
            sb.append("/")
            sb.append(if (used) "yes" else "no")
            sb.append("</td>")
            sb.append("<td>").append(lock.locationName.orEmpty()).append("</td>")
            sb.append("</tr>")
            sb.append("<tr>")
            sb.append("""<td colspan=4 style="word-wrap:break-word">""")
            sb.append(keyEnchantments(lock.keyItemId, gameDatabase))
            sb.append("</td>")
            sb.append("</tr>")
        }

        sb.append("</table>")
        return sb.toString()
    }

    private fun haveKey(
        lock: TowerDoorLock,
        inventoryManager: InventoryManager?,
        character: CharacterState,
    ): Boolean {
        val itemId = lock.keyItemId ?: return false
        val quantity = inventoryManager?.state?.value?.items?.get(itemId)?.quantity ?: 0
        if (quantity > 0) return true
        val keyName = lock.keyName
        return character.equipment.values.any { it.equals(keyName, ignoreCase = true) }
    }

    private fun keyEnchantments(itemId: Int?, gameDatabase: GameDatabase?): String {
        if (itemId == null) return ""
        val entry = gameDatabase?.itemModifier(itemId) ?: return ""
        val parsed = ModifierParser.parse(entry.modifiers)
        return parsed.get(StringModifier.MODIFIERS) ?: entry.modifiers
    }
}
