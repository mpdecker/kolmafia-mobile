package net.sourceforge.kolmafia.request

import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ConcoctionDatabase
import net.sourceforge.kolmafia.data.EquipmentDatabase
import net.sourceforge.kolmafia.familiar.FamiliarManager
import net.sourceforge.kolmafia.inventory.InventoryManager
import net.sourceforge.kolmafia.preferences.Preferences

/**
 * Desktop [UseItemRequest.parseBinge]. Refusal text leaves the item in inventory.
 * A successful feed consumes it, adds familiar experience when the familiar grows,
 * and estimates Slimeling stacks or fullness.
 *
 * Returns false only when the current familiar cannot binge.
 */
object UseItemBingeSync {
    private val WHICHITEM = Regex("""whichitem=(\d+)""", RegexOption.IGNORE_CASE)
    private val QTY = Regex("""qty=(\d+)""", RegexOption.IGNORE_CASE)
    private val BINGE_ACTION = Regex(
        """(?:^|[?&])action=(ghost|hobo|slime|candy)(?:&|$)""",
        RegexOption.IGNORE_CASE,
    )

    fun parse(
        url: String,
        responseText: String,
        character: KoLCharacter? = null,
        familiarManager: FamiliarManager? = null,
        inventory: InventoryManager? = null,
        preferences: Preferences? = null,
        familiarId: Int = familiarManager?.state?.value?.activeFamiliar?.id
            ?: character?.state?.value?.familiarId
            ?: 0,
        itemPower: (Int) -> Int = { id -> EquipmentDatabase.getPower(id) },
        usesMeatStack: (Int) -> Boolean = { id -> ConcoctionDatabase.usesMeatStackIngredient(id) },
    ): Boolean {
        val item = bingedItem(url) ?: return true
        if (responseText.contains("don't currently have") ||
            responseText.contains("not currently using")
        ) {
            return false
        }
        if (responseText.contains("don't have that many") ||
            responseText.contains("don't actually have any") ||
            responseText.contains("doesn't seem interested") ||
            responseText.contains("not something you can give")
        ) {
            return true
        }
        if (responseText.contains("He grows a bit")) {
            addFamiliarExperience(item.count, character, familiarManager)
        }
        if (familiarId == UseItemBingeLog.SLIMELING && preferences != null) {
            if (item.itemId == UseItemBingeLog.GNOLLISH_AUTOPLUNGER || usesMeatStack(item.itemId)) {
                preferences.increment(SlimeStackPref.STACKS_DUE, item.count)
            } else {
                val charges = item.count * itemPower(item.itemId) / 10.0f
                val key = SlimeStackPref.FULLNESS
                preferences.setFloat(key, preferences.getFloat(key, 0f) + charges)
            }
        }
        inventory?.consumeItemLocally(item.itemId, item.count)
        return true
    }

    fun bingedItem(url: String): BingedItem? {
        val path = pagePath(url)
        val bingePage = path.startsWith("familiarbinger.php") ||
            (path.startsWith("inventory.php") && BINGE_ACTION.containsMatchIn(path))
        if (!bingePage) return null
        val itemId = WHICHITEM.find(path)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: return null
        val count = QTY.find(path)?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 1
        if (count <= 0) return null
        return BingedItem(itemId, count)
    }

    private fun addFamiliarExperience(
        amount: Int,
        character: KoLCharacter?,
        familiarManager: FamiliarManager?,
    ) {
        if (amount == 0) return
        val active = familiarManager?.state?.value?.activeFamiliar
        if (active != null && familiarManager != null) {
            familiarManager.applyActiveFamiliarLocally(
                active.copy(experience = active.experience + amount),
            )
        }
        if (character != null) {
            val state = character.state.value
            character.updateFamiliar(
                state.familiarId,
                state.familiarName,
                state.familiarWeight,
                state.familiarExp + amount,
            )
        }
    }

    private fun pagePath(url: String): String {
        val noHash = url.substringBefore('#')
        return if (noHash.contains("://")) {
            noHash.substringAfter("://").substringAfter('/')
        } else {
            noHash.trimStart('/')
        }
    }

    data class BingedItem(val itemId: Int, val count: Int)

    private object SlimeStackPref {
        const val STACKS_DUE = "slimelingStacksDue"
        const val FULLNESS = "slimelingFullness"
    }
}
