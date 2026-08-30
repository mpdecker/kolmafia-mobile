package net.sourceforge.kolmafia.familiar

import net.sourceforge.kolmafia.inventory.InventoryItem

data class FamiliarData(
    val id: Int, val name: String, val race: String,
    val weight: Int, val experience: Int, val kills: Int,
    val pokeLevel: Int = 0,
    val soupWeight: Int = 0,
    val soupAttributes: Set<String> = emptySet(),
    val equipment: InventoryItem? = null,
    val modifiers: Map<String, String> = emptyMap(),
    val feasted: Boolean = false,
    /** Desktop FamiliarDatabase favorite flag, populated by the familiars API when available. */
    val favorite: Boolean = false,
)
