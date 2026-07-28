package net.sourceforge.kolmafia.shop

/** Desktop EquipmentManager folder holder checks for folder shop items. */
object FolderHolderAccessibility {

    const val FOLDER_HOLDER = 6617
    const val REPLICA_FOLDER_HOLDER = 11220

    fun hasFolderHolder(accessibleCount: (Int) -> Int): Boolean =
        accessibleCount(FOLDER_HOLDER) > 0 || accessibleCount(REPLICA_FOLDER_HOLDER) > 0
}
