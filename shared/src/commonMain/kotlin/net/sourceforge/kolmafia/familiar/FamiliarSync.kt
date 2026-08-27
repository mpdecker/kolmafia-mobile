package net.sourceforge.kolmafia.familiar

import net.sourceforge.kolmafia.character.EquipmentSlot
import net.sourceforge.kolmafia.character.KoLCharacter
import net.sourceforge.kolmafia.data.ItemDatabase
import net.sourceforge.kolmafia.inventory.InventoryItem
import net.sourceforge.kolmafia.inventory.ItemType
import net.sourceforge.kolmafia.preferences.Preferences
import net.sourceforge.kolmafia.session.EquipmentManager

/**
 * Desktop FamiliarRequest.parseResponse / handleFamiliarChange hub (Phases 2421–2450).
 */
object FamiliarSync {
    fun parseResponse(
        url: String,
        html: String,
        familiarManager: FamiliarManager?,
        preferences: Preferences? = null,
        character: KoLCharacter? = null,
        equipmentManager: EquipmentManager? = null,
    ): Boolean {
        if (!url.contains("familiar.php", ignoreCase = true)) return false
        val action = actionOf(url)
        return when (action) {
            null, "" -> {
                // Terrarium visit — lock state + optional weight scrape
                FamiliarEquipmentLockSyncLite.parseAndWrite(html, preferences)
                parseWeightXp(html, familiarManager, preferences)
                true
            }
            "message" -> true
            "newfam" -> parseNewFam(url, html, familiarManager, preferences, equipmentManager)
            "putback" -> parsePutback(html, familiarManager, preferences, equipmentManager)
            "equip" -> parseEquip(url, html, familiarManager, preferences, equipmentManager)
            "unequip" -> parseUnequip(url, html, familiarManager, preferences, equipmentManager)
            "lockequip" -> parseLockEquip(html, preferences)
            "hatseat" -> parseHatseat(url, html, familiarManager, preferences)
            "backpack" -> parseBackpack(url, html, familiarManager, preferences)
            "steal" -> {
                preferences?.setBoolean("_familiarStealUsed", true)
                true
            }
            else -> {
                FamiliarEquipmentLockSyncLite.parseAndWrite(html, preferences)
                parseWeightXp(html, familiarManager, preferences)
                true
            }
        }
    }

    fun handleFamiliarChange(
        changeTo: FamiliarData?,
        familiarManager: FamiliarManager?,
        preferences: Preferences? = null,
        equipmentManager: EquipmentManager? = null,
    ): Boolean {
        if (changeTo == null || familiarManager == null) return false
        familiarManager.applyActiveFamiliarLocally(changeTo)
        preferences?.setInt("activeFamiliarId", changeTo.id)
        preferences?.setString("activeFamiliarRace", changeTo.race)
        // Locked familiar item transfer is best-effort: clear lock if new fam can't hold it
        val locked = preferences?.getBoolean("familiarEquipmentLocked", false) == true
        if (locked && changeTo.equipment == null) {
            preferences?.setBoolean("familiarEquipmentLocked", false)
        }
        equipmentManager?.setEquipment(
            EquipmentSlot.FAMILIAR,
            changeTo.equipment?.itemId ?: -1,
            swapInventory = false,
        )
        return true
    }

    private fun parseNewFam(
        url: String,
        html: String,
        familiarManager: FamiliarManager?,
        preferences: Preferences?,
        equipmentManager: EquipmentManager?,
    ): Boolean {
        if (!html.contains("You take", ignoreCase = true)) return false
        val id = whichFam(url) ?: return false
        val owned = familiarManager?.state?.value?.ownedFamiliars?.firstOrNull { it.id == id }
            ?: FamiliarData(id, "Familiar #$id", "Familiar #$id", 1, 0, 0)
        return handleFamiliarChange(owned, familiarManager, preferences, equipmentManager)
    }

    private fun parsePutback(
        html: String,
        familiarManager: FamiliarManager?,
        preferences: Preferences?,
        equipmentManager: EquipmentManager?,
    ): Boolean {
        if (!html.contains("back in the Terrarium", ignoreCase = true)) return false
        familiarManager?.clearActiveFamiliarLocally()
        preferences?.setInt("activeFamiliarId", 0)
        preferences?.setString("activeFamiliarRace", "")
        preferences?.setBoolean("familiarEquipmentLocked", false)
        equipmentManager?.setEquipment(EquipmentSlot.FAMILIAR, -1, swapInventory = false)
        return true
    }

    private fun parseEquip(
        url: String,
        html: String,
        familiarManager: FamiliarManager?,
        preferences: Preferences?,
        equipmentManager: EquipmentManager?,
    ): Boolean {
        if (!html.contains("You equip", ignoreCase = true)) return false
        val itemId = whichItem(url) ?: return false
        val famId = whichFam(url)
            ?: familiarManager?.state?.value?.activeFamiliar?.id
            ?: return false
        val item = InventoryItem(
            itemId,
            ItemDatabase.getItemName(itemId).ifBlank { "Familiar item" },
            1,
            ItemType.FAMILIAR_ITEM,
        )
        familiarManager?.applyFamiliarEquipmentLocally(famId, item)
        if (familiarManager?.state?.value?.activeFamiliar?.id == famId) {
            equipmentManager?.setEquipment(EquipmentSlot.FAMILIAR, itemId, swapInventory = false)
        }
        preferences?.setInt("familiarItemId", itemId)
        return true
    }

    private fun parseUnequip(
        url: String,
        html: String,
        familiarManager: FamiliarManager?,
        preferences: Preferences?,
        equipmentManager: EquipmentManager?,
    ): Boolean {
        if (!html.contains("Item unequipped", ignoreCase = true)) return false
        val famId = famId(url)
            ?: whichFam(url)
            ?: familiarManager?.state?.value?.activeFamiliar?.id
            ?: return false
        familiarManager?.applyFamiliarEquipmentLocally(famId, null)
        if (familiarManager?.state?.value?.activeFamiliar?.id == famId) {
            equipmentManager?.setEquipment(EquipmentSlot.FAMILIAR, -1, swapInventory = false)
        }
        preferences?.setInt("familiarItemId", 0)
        return true
    }

    private fun parseLockEquip(html: String, preferences: Preferences?): Boolean {
        if (html.contains("You cannot", ignoreCase = true)) return false
        val prefs = preferences ?: return true
        val locked = prefs.getBoolean("familiarEquipmentLocked", false)
        prefs.setBoolean("familiarEquipmentLocked", !locked)
        return true
    }

    private fun parseHatseat(
        url: String,
        html: String,
        familiarManager: FamiliarManager?,
        preferences: Preferences?,
    ): Boolean {
        if (html.contains("You're not wearing a hat seat", ignoreCase = true)) return false
        val famId = famId(url) ?: return false
        if (famId == 0) {
            preferences?.setInt("enthronedFamiliarId", 0)
            preferences?.setString("enthronedFamiliar", "")
            return true
        }
        if (!html.contains("Crown of Thrones", ignoreCase = true) &&
            !html.contains("hat seat", ignoreCase = true)
        ) {
            // Still accept when AJAX success without prose
        }
        val race = familiarManager?.state?.value?.ownedFamiliars
            ?.firstOrNull { it.id == famId }?.race.orEmpty()
        preferences?.setInt("enthronedFamiliarId", famId)
        preferences?.setString("enthronedFamiliar", race)
        return true
    }

    private fun parseBackpack(
        url: String,
        html: String,
        familiarManager: FamiliarManager?,
        preferences: Preferences?,
    ): Boolean {
        if (html.contains("You're not wearing", ignoreCase = true) &&
            html.contains("Bjorn", ignoreCase = true)
        ) {
            return false
        }
        val famId = famId(url) ?: return false
        if (famId == 0) {
            preferences?.setInt("bjornedFamiliarId", 0)
            preferences?.setString("bjornedFamiliar", "")
            return true
        }
        val race = familiarManager?.state?.value?.ownedFamiliars
            ?.firstOrNull { it.id == famId }?.race.orEmpty()
        preferences?.setInt("bjornedFamiliarId", famId)
        preferences?.setString("bjornedFamiliar", race)
        return true
    }

    fun parseWeightXp(
        html: String,
        familiarManager: FamiliarManager?,
        preferences: Preferences?,
    ) {
        Regex(
            """(\d+)\s*(?:lb|pound)s?.*?(\d+)\s*(?:xp|exp)""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        ).find(html)?.let { m ->
            val weight = m.groupValues[1].toIntOrNull() ?: return@let
            val xp = m.groupValues[2].toIntOrNull() ?: return@let
            preferences?.setInt("familiarWeight", weight)
            preferences?.setInt("familiarExperience", xp)
            familiarManager?.applyActiveWeightXpLocally(weight, xp)
        }
        if (html.contains("feasted", ignoreCase = true) ||
            html.contains("well-fed", ignoreCase = true)
        ) {
            preferences?.setBoolean("_familiarFeasted", true)
        }
    }

    private fun actionOf(url: String): String? =
        Regex("""action=([^&]+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.lowercase()

    private fun whichFam(url: String): Int? =
        Regex("""whichfam=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull()

    private fun famId(url: String): Int? =
        Regex("""famid=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull()

    private fun whichItem(url: String): Int? =
        Regex("""whichitem=(\d+)""", RegexOption.IGNORE_CASE)
            .find(url)?.groupValues?.get(1)?.toIntOrNull()
}

/** Local copy of lock sync to avoid circular ash imports from familiar package. */
internal object FamiliarEquipmentLockSyncLite {
    private val LOCK_REGEX = Regex("""familiar\.php\?action=lockequip""")
    private val LOCKED_REGEX = Regex("""Locked""")

    fun parseAndWrite(html: String, prefs: Preferences?) {
        prefs ?: return
        if ("familiar.php" !in html && !html.contains("lockequip", ignoreCase = true)) {
            // Still allow Locked text alone
        }
        val hasLockLink = LOCK_REGEX.containsMatchIn(html)
        val isLocked = LOCKED_REGEX.containsMatchIn(html) && !hasLockLink
        if (LOCKED_REGEX.containsMatchIn(html) || hasLockLink) {
            prefs.setBoolean("familiarEquipmentLocked", isLocked)
        }
    }
}
