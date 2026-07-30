package net.sourceforge.kolmafia.data

/** Desktop FamiliarDatabase / FamiliarRaceData helpers for `$familiar[field]` proxy reads. */
object FamiliarDefinitionProxy {

    fun getByIdOrName(familiarRef: String): FamiliarDefinition? {
        familiarRef.toIntOrNull()?.let { FamiliarDefinitionDatabase.getById(it) }?.let { return it }
        return FamiliarDefinitionDatabase.getByName(familiarRef)
    }

    fun resolveFamiliarId(familiarRef: String): Int =
        familiarRef.toIntOrNull()
            ?: FamiliarDefinitionDatabase.getByName(familiarRef)?.id
            ?: 0

    fun getImage(familiarId: Int): String =
        FamiliarDefinitionDatabase.getById(familiarId)?.image ?: ""

    fun getAttributesString(familiarId: Int): String =
        FamiliarDefinitionDatabase.getById(familiarId)?.combinedAttributes() ?: ""

    fun getLarvaItemName(familiarId: Int): String =
        FamiliarDefinitionDatabase.getById(familiarId)?.larvaItem ?: ""

    fun getLarvaItemId(familiarId: Int): Int =
        ItemDatabase.getByName(getLarvaItemName(familiarId))?.id ?: -1

    fun getDropName(familiarId: Int): String =
        FamiliarDailyStats.getDropInfo(familiarId)?.dropName ?: ""

    fun getDropItemId(familiarId: Int): Int =
        FamiliarDailyStats.getDropInfo(familiarId)?.dropItemId ?: -1
}
