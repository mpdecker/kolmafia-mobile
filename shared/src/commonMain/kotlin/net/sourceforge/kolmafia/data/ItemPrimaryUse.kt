package net.sourceforge.kolmafia.data

enum class ItemPrimaryUse {
    NONE,
    FOOD,
    DRINK,
    SPLEEN,
    POTION,
    AVATAR,
    USABLE,
    MULTIPLE,
    REUSABLE,
    MESSAGE,
    GROW,
    POKEPILL,
    HAT,
    WEAPON,
    SIXGUN,
    OFFHAND,
    CONTAINER,
    SHIRT,
    PANTS,
    ACCESSORY,
    FAMILIAR,
    STICKER,
    CARD,
    FOLDER,
    BOOTSPUR,
    BOOTSKIN,
    FOOD_HELPER,
    DRINK_HELPER,
    ZAP,
    SPHERE,
    GUARDIAN,
    UNKNOWN;

    val isEquipment get() = this in EQUIPMENT_TYPES
    val isConsumable get() = this in CONSUMABLE_TYPES

    companion object {
        private val EQUIPMENT_TYPES = setOf(HAT, WEAPON, SIXGUN, OFFHAND, CONTAINER, SHIRT, PANTS, ACCESSORY, FAMILIAR)
        private val CONSUMABLE_TYPES = setOf(FOOD, DRINK, SPLEEN, FOOD_HELPER, DRINK_HELPER)

        fun fromString(s: String): ItemPrimaryUse = when (s.trim().lowercase()) {
            "none" -> NONE
            "food" -> FOOD
            "drink" -> DRINK
            "spleen" -> SPLEEN
            "potion" -> POTION
            "avatar" -> AVATAR
            "usable" -> USABLE
            "multiple" -> MULTIPLE
            "reusable" -> REUSABLE
            "message" -> MESSAGE
            "grow" -> GROW
            "pokepill" -> POKEPILL
            "hat" -> HAT
            "weapon" -> WEAPON
            "sixgun" -> SIXGUN
            "offhand" -> OFFHAND
            "container" -> CONTAINER
            "shirt" -> SHIRT
            "pants" -> PANTS
            "accessory" -> ACCESSORY
            "familiar" -> FAMILIAR
            "sticker" -> STICKER
            "card" -> CARD
            "folder" -> FOLDER
            "bootspur" -> BOOTSPUR
            "bootskin" -> BOOTSKIN
            "food helper" -> FOOD_HELPER
            "drink helper" -> DRINK_HELPER
            "zap" -> ZAP
            "sphere" -> SPHERE
            "guardian" -> GUARDIAN
            else -> UNKNOWN
        }
    }
}
