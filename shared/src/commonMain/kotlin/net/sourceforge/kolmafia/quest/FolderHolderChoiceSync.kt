package net.sourceforge.kolmafia.quest

/** Desktop EquipmentRequest folder-holder choice 774 synchronization. */
object FolderHolderChoiceSync {
    const val CHOICE_ID = 774
    const val FOLDER_01 = 6618
    private const val FOLDER_28 = FOLDER_01 + 27

    fun apply(
        choiceId: Int,
        decision: Int,
        choiceUrl: String,
        html: String,
        consumeItem: (Int, Int) -> Unit,
        setFolders: (List<Int>) -> Unit = {},
    ): Boolean {
        if (choiceId != CHOICE_ID) return false
        if (decision == 1 && html.contains("You carefully place your new folder in the holder")) {
            val index = Regex("""(?:[?&])folder=(\d+)""").find(choiceUrl)?.groupValues?.get(1)?.toIntOrNull()
            val itemId = index?.let { FOLDER_01 + it - 1 }
            if (itemId != null && itemId in FOLDER_01..FOLDER_28) consumeItem(itemId, 1)
        }
        val start = html.indexOf("Contents of your Folder Holder")
        val stop = listOf(html.indexOf("Folders in your Inventory"), html.indexOf("You don't have any folders to add."))
            .filter { it >= 0 }.minOrNull()
        if (start >= 0 && stop != null && stop > start) {
            val ids = Regex("""folders/folder(\d+)\.gif""").findAll(html.substring(start, stop))
                .mapNotNull { it.groupValues[1].toIntOrNull() }
                .map { FOLDER_01 + it - 1 }
                .filter { it in FOLDER_01..FOLDER_28 }
                .toList()
            setFolders(ids)
        }
        return true
    }
}
