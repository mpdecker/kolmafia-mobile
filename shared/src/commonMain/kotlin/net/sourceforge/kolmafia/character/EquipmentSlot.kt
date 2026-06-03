package net.sourceforge.kolmafia.character

enum class EquipmentSlot(val apiKey: String, val displayName: String) {
    HAT("hat", "Hat"),
    WEAPON("weapon", "Weapon"),
    OFFHAND("offhand", "Off-hand"),
    SHIRT("shirt", "Shirt"),
    PANTS("pants", "Pants"),
    ACC1("acc1", "Accessory 1"),
    ACC2("acc2", "Accessory 2"),
    ACC3("acc3", "Accessory 3"),
    FAMILIAR("familiarequip", "Familiar Equipment"),
    CONTAINER("container", "Container");

    companion object {
        fun fromApiKey(key: String): EquipmentSlot? = entries.find { it.apiKey == key }
    }
}
