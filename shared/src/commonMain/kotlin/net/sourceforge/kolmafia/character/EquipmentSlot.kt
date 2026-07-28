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
    CONTAINER("container", "Container"),
    CODPIECE1("codpiece1", "Codpiece Gem 1"),
    CODPIECE2("codpiece2", "Codpiece Gem 2"),
    CODPIECE3("codpiece3", "Codpiece Gem 3"),
    CODPIECE4("codpiece4", "Codpiece Gem 4"),
    CODPIECE5("codpiece5", "Codpiece Gem 5");

    companion object {
        val CODPIECE_SLOTS = listOf(CODPIECE1, CODPIECE2, CODPIECE3, CODPIECE4, CODPIECE5)

        fun fromApiKey(key: String): EquipmentSlot? = entries.find { it.apiKey == key }
    }
}
