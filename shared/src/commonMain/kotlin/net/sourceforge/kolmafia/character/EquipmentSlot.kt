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
    CARDSLEEVE("cardsleeve", "Card Sleeve"),
    CONTAINER("container", "Container"),
    CODPIECE1("codpiece1", "Codpiece Gem 1"),
    CODPIECE2("codpiece2", "Codpiece Gem 2"),
    CODPIECE3("codpiece3", "Codpiece Gem 3"),
    CODPIECE4("codpiece4", "Codpiece Gem 4"),
    CODPIECE5("codpiece5", "Codpiece Gem 5"),
    STICKER1("sticker1", "Sticker 1"),
    STICKER2("sticker2", "Sticker 2"),
    STICKER3("sticker3", "Sticker 3"),
    FOLDER1("folder1", "Folder 1"),
    FOLDER2("folder2", "Folder 2"),
    FOLDER3("folder3", "Folder 3"),
    FOLDER4("folder4", "Folder 4"),
    FOLDER5("folder5", "Folder 5"),
    BOOTSKIN("bootskin", "Bootskin"),
    BOOTSPUR("bootspur", "Bootspur");

    companion object {
        val CODPIECE_SLOTS = listOf(CODPIECE1, CODPIECE2, CODPIECE3, CODPIECE4, CODPIECE5)
        val STICKER_SLOTS = listOf(STICKER1, STICKER2, STICKER3)
        val FOLDER_SLOTS = listOf(FOLDER1, FOLDER2, FOLDER3, FOLDER4, FOLDER5)
        val FOLDER_SLOTS_AFTERCORE = listOf(FOLDER1, FOLDER2, FOLDER3)
        val BOOT_SLOTS = listOf(BOOTSKIN, BOOTSPUR)
        val SUB_SLOTS = STICKER_SLOTS + FOLDER_SLOTS + BOOT_SLOTS

        val SEARCH_SLOTS = listOf(
            HAT,
            WEAPON,
            OFFHAND,
            SHIRT,
            PANTS,
            CONTAINER,
            ACC1,
            ACC2,
            ACC3,
            FAMILIAR,
        )

        /** Desktop SlotSet.ALL_SLOTS emit coverage (main + sub-slots + card sleeve). */
        val ALL_EMIT_SLOTS = SEARCH_SLOTS + SUB_SLOTS + listOf(CARDSLEEVE)

        fun folderSlotsFor(inKoLHS: Boolean): List<EquipmentSlot> =
            if (inKoLHS) FOLDER_SLOTS else FOLDER_SLOTS_AFTERCORE

        fun fromApiKey(key: String): EquipmentSlot? = entries.find { it.apiKey == key }
    }
}
