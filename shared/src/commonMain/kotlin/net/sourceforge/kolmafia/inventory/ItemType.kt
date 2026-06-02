package net.sourceforge.kolmafia.inventory

enum class ItemType {
    FOOD, DRINK, SPLEEN, WEAPON, OFFHAND, HAT, SHIRT, PANTS,
    ACCESSORY, FAMILIAR_ITEM, USABLE, MULTIUSABLE, REUSABLE, OTHER;

    companion object {
        fun fromApiString(s: String): ItemType = when (s.lowercase()) {
            "food" -> FOOD; "drink" -> DRINK; "spleen" -> SPLEEN
            "weapon" -> WEAPON; "offhand" -> OFFHAND; "hat" -> HAT
            "shirt" -> SHIRT; "pants" -> PANTS
            "acc1", "acc2", "acc3", "accessory" -> ACCESSORY
            "familiar" -> FAMILIAR_ITEM; "usable" -> USABLE
            "multiusable" -> MULTIUSABLE; "reusable" -> REUSABLE
            else -> OTHER
        }
    }
}
